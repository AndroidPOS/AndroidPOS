package com.ricoh.pos;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ricoh.pos.data.Order;
import com.ricoh.pos.data.Product;
import com.ricoh.pos.data.WomanShopFormatter;
import com.ricoh.pos.model.ProductsManager;
import com.ricoh.pos.model.RegisterManager;
import com.ricoh.pos.model.UpdateOrderListListener;

import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * A fragment representing a single Category detail screen. This fragment is
 * either contained in a {@link CategoryListActivity} in two-pane mode (on
 * tablets) or a {@link CategoryDetailActivity} on handsets.
 */
public class OrderListFragment extends ListFragment implements UpdateOrderListListener {

	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";
	// This is the maximum fraction digits for total payment to display.
	private static final int MAXIMUM_FRACTION_DIGITS = 2;

	private final int IMAGE_VIEW_SIZE = 120;

	private RegisterManager registerManager;
	private ArrayList<Product> orderProductList;

	private OnOrderClickListener onOrderClickListener;

	public OrderListFragment() {
		this.registerManager = RegisterManager.getInstance();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof OnOrderClickListener)) {
			throw new ClassCastException("OnOrderClickListener isn't implemented");
		}
		onOrderClickListener = (OnOrderClickListener) activity;
	}

	@Override
	public void notifyUpdateOrderList() {
		setOrderProductList();
		setListAdapter(new ListAdapter(getActivity()));
	}

	public interface OnOrderClickListener {
		public void onOrderClicked(Product product);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerManager.setUpdateOrderListListener(this);
		setOrderProductList();
		setListAdapter(new ListAdapter(getActivity()));
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	}

	private void setOrderProductList() {
		ArrayList<Product> allProductList = ProductsManager.getInstance().getAllProducts();
		if (allProductList == null) {
			throw new IllegalStateException("No product data");
		}

		orderProductList = new ArrayList<Product>();
		for (Product product : allProductList) {
			Order order = registerManager.findOrderOfTheProduct(product);
			if (order != null && order.getNumberOfOrder() > 0) {
				orderProductList.add(product);
			}
		}
	}

	public class ListAdapter extends BaseAdapter {
		private LayoutInflater inflater;

		public ListAdapter(Context context) {
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return orderProductList.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.order_row, null);
			}

			final Product product = orderProductList.get(position);

			ImageView imageView = (ImageView) convertView.findViewById(R.id.photo);
			imageView.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
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
						Log.e("OrderListFragment", "Product image file is not found.", e);
						Toast.makeText(getActivity(), R.string.error_image_file_not_found, Toast.LENGTH_SHORT).show();
					}
				}
			});

			TextView textView = (TextView) convertView.findViewById(R.id.filename);
			textView.setPadding(10, 0, 0, 0);
			String productName = product.getName();
			if (productName == null || productName.length() == 0) {
				throw new NullPointerException("Product name is not valid");
			}
			textView.setText(productName);

			TextView priceView = (TextView) convertView.findViewById(R.id.price);
			priceView.setPadding(10, 0, 0, 0);
			NumberFormat.getInstance().setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);
			priceView.setText(NumberFormat.getInstance().format(
					WomanShopFormatter.convertPaisaToRupee(product.getPrice())));

			final TextView numberOfSalseView = (TextView) convertView.findViewById(R.id.numberOfSales);
			numberOfSalseView.setPadding(10, 0, 0, 0);

			Order order = registerManager.findOrderOfTheProduct(product);
			if (order != null) {
				int numberOfSales = order.getNumberOfOrder();
				numberOfSalseView.setText(NumberFormat.getInstance().format(numberOfSales));
			} else {
				numberOfSalseView.setText(NumberFormat.getInstance().format(0));
			}

			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					onOrderClickListener.onOrderClicked(product);
				}
			});

			return convertView;
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
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		registerManager.removeUpdateOrderListListener(this);
	}

}
