package com.ricoh.pos;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ricoh.pos.data.Order;
import com.ricoh.pos.data.Product;
import com.ricoh.pos.data.SingleSalesRecord;
import com.ricoh.pos.data.WomanShopFormatter;
import com.ricoh.pos.model.ProductsManager;
import com.ricoh.pos.model.RegisterManager;
import com.ricoh.pos.model.SalesRecordManager;
import com.ricoh.pos.model.WomanShopIOManager;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;

public class RegisterConfirmActivity extends FragmentActivity
		implements RegisterConfirmFragment.OnButtonClickListener, OrderListFragment.OnOrderClickListener {

	private SalesDatabaseHelper salesDatabaseHelper;
	private static SQLiteDatabase salesDatabase;
	private RegisterManager registerManager;

	public RegisterConfirmActivity() {
		this.registerManager = RegisterManager.getInstance();
	}

	private static final int IMAGE_VIEW_SIZE = 240;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_register_confirm);
		if (findViewById(R.id.order_list_container) != null) {
			// add OrderListFragment
			OrderListFragment fragment = new OrderListFragment();
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.order_list_container, fragment).commit();

			// add RegisterConfirmFragment
			RegisterConfirmFragment registerConfirmFragment = new RegisterConfirmFragment();
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.register_confirm_container, registerConfirmFragment).commit();
		}

		salesDatabaseHelper = new SalesDatabaseHelper(this);
		salesDatabase = salesDatabaseHelper.getWritableDatabase();
	}

	@Override
	public void onOkClicked() {
		EditText discount = (EditText) RegisterConfirmActivity.this.findViewById(R.id.discountValue);
		try {
			String text = discount.getText().toString();
			RegisterManager.getInstance().updateDiscountValue(WomanShopFormatter.convertRupeeToPaisa(text));
		} catch (IllegalArgumentException e) {
			Log.e("RegisterConfirmActivity","failed in discount value check.", e);
			Toast.makeText(RegisterConfirmActivity.this.getBaseContext(), R.string.discount_error, Toast.LENGTH_LONG).show();
			return;
		}
		if (RegisterManager.getInstance().getOriginalTotalAmount() != 0) {
			// Save this sales record
			SingleSalesRecord record = RegisterManager.getInstance().getSingleSalesRecord();
			record.calcDiscountAllocation(); // 値引き割り当ての実施
			SalesRecordManager.getInstance().storeSingleSalesRecord(salesDatabase, record);
			WomanShopIOManager womanShopIOManager = new WomanShopIOManager();
			DatabaseHelper databaseHelper = new DatabaseHelper(this);
			womanShopIOManager.setDatabase(databaseHelper.getWritableDatabase());
			for (Order order : record.getAllOrders()) {
				womanShopIOManager.updateStock(order.getProduct(), order.getNumberOfOrder());
			}
			List<Product> productList = womanShopIOManager.searchAll();
			ProductsManager.getInstance().updateProducts(productList);
		}

		// Clear this record
		RegisterManager.getInstance().clearAllOrders();

		// Go to the CategoryListActivity
		Intent intent = new Intent(this, CategoryListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	@Override
	public void onCancelClicked() {
		finish();
	}

	@Override
	public void onPriceDownClicked() {
		showPriceDownDialog();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		salesDatabase.close();
		salesDatabaseHelper.close();
		RegisterManager.getInstance().clearDiscountValue();
		Log.d("debug", "Exit RegisterConfirmActivity onDestroy");
	}

	private void showPriceDownDialog() {
		PriceDownDialog dialog = new PriceDownDialog();
		dialog.show(this);
	}

	@Override
	public void onOrderClicked(Product product) {
		Order order = registerManager.findOrderOfTheProduct(product);
		EditNumberOfOrderDialog dialog = EditNumberOfOrderDialog.newInstance(product, order);
		dialog.show(getFragmentManager(), null);
	}

	public static class EditNumberOfOrderDialog extends DialogFragment {
		private Product product;
		private Order order;
		private int orderNum = 0;

		public void setProduct(Product product) {
			this.product = product;
		}

		public void setOrder(Order order) {
			this.order = order;
		}

		public static EditNumberOfOrderDialog newInstance(Product product, Order order) {
			EditNumberOfOrderDialog frag = new EditNumberOfOrderDialog();
			frag.setProduct(product);
			frag.setOrder(order);
			return frag;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			LayoutInflater inflater = getActivity().getLayoutInflater();
			final View view = inflater.inflate(R.layout.change_order_num_dialog, null);
			builder.setView(view);

			setImageView(view);
			setProductInformationView(view);
			setNumberOfOrderView(view);

			builder.setTitle(R.string.dialog_title_edit_number_of_order);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (orderNum > 0) {
						new RegisterConfirmActivity().addProduct(orderNum, product);
					} else if (orderNum < 0) {
						new RegisterConfirmActivity().removeProduct(orderNum, product);
					}
				}
			});
			AlertDialog dialog = builder.create();
			return dialog;
		}

		private void setImageView(View convertView) {
			ImageView imageView = (ImageView) convertView.findViewById(R.id.photo);
			imageView.setLayoutParams(new LinearLayout.LayoutParams(IMAGE_VIEW_SIZE, IMAGE_VIEW_SIZE));
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			setImageView(product, imageView);
			imageView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					try {
						ProductDetailDialogFragment dialogFragment = new ProductDetailDialogFragment();
						Bundle arguments = new Bundle();
						DisplayMetrics metrics = getResources().getDisplayMetrics();
						arguments.putParcelable(ProductDetailDialogFragment.ARG_KEY_IMAGE_BITMAP, product.decodeProductImage(metrics.widthPixels, metrics.heightPixels));
						dialogFragment.setArguments(arguments);
						dialogFragment.show(getActivity().getFragmentManager(), ProductDetailDialogFragment.DIALOG_TAG);
					} catch (FileNotFoundException e) {
						Log.e("RegisterConfirmActivity", "Product image file is not found.", e);
						Toast.makeText(getActivity(), R.string.error_image_file_not_found, Toast.LENGTH_SHORT).show();
					}
				}
			});
		}

		private void setImageView(Product product, ImageView imageView) {
			try {
				Bitmap image = product.decodeProductImage(IMAGE_VIEW_SIZE, IMAGE_VIEW_SIZE);
				imageView.setImageBitmap(image);
				imageView.setVisibility(View.VISIBLE);
			} catch (FileNotFoundException e) {
				// プロダクトIDに対応する写真がない。ないことは許容されるので問題はない
				// ただしViewが再利用されるため、関係ない写真がViewに表示される可能性がある。
				// そのため写真がない場合はinvisibleにしている。
				// TODO: 裏ではメモリを余計に消費している可能性があるのでそれをクリアした方が本当は良いはず）
				imageView.setVisibility(View.INVISIBLE);
				//imageView.setImageBitmap(null);
			}
		}

		private void setProductInformationView(View contentView) {
			TextView textView = (TextView) contentView.findViewById(R.id.filename);
			textView.setPadding(10, 0, 0, 0);
			String productName = product.getName();
			if (productName == null || productName.length() == 0) {
				throw new NullPointerException("Product name is not valid");
			}
			textView.setText(productName);

			TextView priceView = (TextView) contentView.findViewById(R.id.price);
			priceView.setPadding(10, 0, 0, 0);
			NumberFormat.getInstance().setMaximumFractionDigits(2);
			priceView.setText(NumberFormat.getInstance().format(
					WomanShopFormatter.convertPaisaToRupee(product.getPrice())));
		}

		private void setNumberOfOrderView(View contenView) {
			final TextView numberOfSalesText = (TextView) contenView
					.findViewById(R.id.numberOfSales);

			ProductButton plusBtn = (ProductButton) contenView.findViewById(R.id.plusButton);
			plusBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					numberOfSalesText.setText(String.valueOf(Integer.parseInt(numberOfSalesText.getText().toString()) + 1));
					orderNum++;
				}
			});

			ProductButton minusBtn = (ProductButton) contenView.findViewById(R.id.minusButton);
			minusBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int i = Integer.parseInt(numberOfSalesText.getText().toString());
					if (i != 0) {
						numberOfSalesText.setText(String.valueOf(i - 1));
						orderNum--;
					}
				}
			});

			if (order == null) {
				numberOfSalesText.setText("0");
			} else {
				int numberOfSales = order.getNumberOfOrder();
				numberOfSalesText.setText(NumberFormat.getInstance().format(numberOfSales));
			}
		}
	}

	private void addProduct(int num, Product product) {
		for (int i = 0; i < num; i++) {
			registerManager.plusNumberOfOrder(product);
		}
		registerManager.notifyUpdateOrderList();

	}

	private void removeProduct(int num, Product product) {
		for (int i = 0; i > num; i--) {
			registerManager.minusNumberOfOrder(product);
		}
		registerManager.notifyUpdateOrderList();
	}

}
