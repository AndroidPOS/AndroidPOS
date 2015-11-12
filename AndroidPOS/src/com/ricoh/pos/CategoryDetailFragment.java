package com.ricoh.pos;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ricoh.pos.data.Order;
import com.ricoh.pos.data.Product;
import com.ricoh.pos.model.ProductsManager;
import com.ricoh.pos.model.RegisterManager;

import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * A fragment representing a single Category detail screen. This fragment is
 * either contained in a {@link CategoryListActivity} in two-pane mode (on
 * tablets) or a {@link CategoryDetailActivity} on handsets.
 */
public class CategoryDetailFragment extends ListFragment {

	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";
	public static final String ARG_SEARCH_WORD = "serch_word";
	// This is the maximum fraction digits for total payment to display.
	private static final int MAXIMUM_FRACTION_DIGITS = 2;

	private final int IMAGE_VIEW_SIZE = 120;

	private RegisterManager registerManager;

	private String category;
	private ArrayList<Product> productList;
	private ArrayList<Product> searchProductList;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public CategoryDetailFragment() {
		this.registerManager = RegisterManager.getInstance();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = getArguments();
		this.category = bundle.getString(CategoryDetailFragment.ARG_ITEM_ID);

		if (category.equals(getString(R.string.category_title_default))) {
			productList = ProductsManager.getInstance().getAllProducts();
		} else {
			productList = ProductsManager.getInstance().getProductsInCategory(category);
		}
		String searchWord = bundle.getString(CategoryDetailFragment.ARG_SEARCH_WORD);
		if (null != searchWord && !searchWord.isEmpty()) {
			searchProductList = new ArrayList<Product>();
			for (Product product : productList) {
				if (product.getName().toUpperCase().indexOf(searchWord.toUpperCase()) != -1) {
					searchProductList.add(product);
				}
			}
		} else {
			searchProductList = productList;
		}

		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	}

	@Override
	public void onResume() {
		super.onResume();
		setListAdapter(new ListAdapter(getActivity()));
	}

	public class ListAdapter extends BaseAdapter {
		// private Context contextInAdapter;
		private LayoutInflater inflater;

		public ListAdapter(Context context) {
			// contextInAdapter = context;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return searchProductList.size();
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
				convertView = inflater.inflate(R.layout.row, null);
			}

			Product product = searchProductList.get(position);

			setImageView(convertView, product);
			setProductInformationView(convertView, product);
			setNumberOfOrderView(convertView, product);

			return convertView;
		}

		private void setImageView(View convertView, final Product product) {
			ImageView imageView = (ImageView) convertView.findViewById(R.id.photo);
			imageView.setLayoutParams(new LinearLayout.LayoutParams(IMAGE_VIEW_SIZE, IMAGE_VIEW_SIZE));
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			setImageView(product, imageView);
			imageView.setOnClickListener(new OnClickListener() {
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
						Log.e("CategoryDetailFragment", "Product image file is not found.", e);
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

		private void setProductInformationView(View convertView, Product product) {
			TextView textView = (TextView) convertView.findViewById(R.id.filename);
			String productName = product.getName();
			if (productName == null || productName.length() == 0) {
				throw new NullPointerException("Product name is not valid");
			}
			textView.setText(productName);

			NumberFormat.getInstance().setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);

			TextView priceView = (TextView) convertView.findViewById(R.id.price);
			priceView.setText(NumberFormat.getInstance().format(product.getPrice()) + getString(R.string.currency_india));

			TextView stockView = (TextView) convertView.findViewById(R.id.stock);
			int stock = product.getStock();
			stockView.setText(NumberFormat.getInstance().format(stock));
			if (stock <= 0) {
				stockView.setTextColor(getResources().getColor(R.color.warn));
			} else {
				stockView.setTextColor(getResources().getColor(android.R.color.black));
			}
		}

		private void setNumberOfOrderView(View convertView, Product product) {

			ProductEditText numberOfSalesText = (ProductEditText) convertView
					.findViewById(R.id.numberOfSales);
			numberOfSalesText.setProduct(product);
			numberOfSalesText.setInputType(InputType.TYPE_CLASS_NUMBER);
			numberOfSalesText.setImeOptions(EditorInfo.IME_ACTION_DONE);
			numberOfSalesText.addTextChangedListener(new NumberOfSalesWatcher(numberOfSalesText));

			ProductButton plusBtn = (ProductButton) convertView.findViewById(R.id.plusButton);
			plusBtn.setProduct(product);
			plusBtn.setProductEditText(numberOfSalesText);
			plusBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ProductButton button = (ProductButton) v;
					Product product = button.getProduct();
					registerManager.plusNumberOfOrder(product);
					int numberOfOrder = registerManager.getNumberOfOrder(product);
					ProductEditText editText = button.getProductEditText();
					NumberFormat.getInstance().setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);
					editText.setText(NumberFormat.getInstance().format(numberOfOrder));
				}
			});

			ProductButton minusBtn = (ProductButton) convertView.findViewById(R.id.minusButton);
			minusBtn.setProduct(product);
			minusBtn.setProductEditText(numberOfSalesText);
			minusBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ProductButton button = (ProductButton) v;
					Product product = button.getProduct();
					registerManager.minusNumberOfOrder(product);
					int numberOfOrder = registerManager.getNumberOfOrder(product);
					ProductEditText editText = button.getProductEditText();
					if (numberOfOrder == 0) {
						editText.getEditableText().clear();
					} else {
						NumberFormat.getInstance().setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);
						editText.setText(NumberFormat.getInstance().format(numberOfOrder));
					}
				}
			});

			Order order = registerManager.findOrderOfTheProduct(product);
			if (order == null || order.getNumberOfOrder() == 0) {
				numberOfSalesText.getEditableText().clear();
			} else {
				int numberOfSales = order.getNumberOfOrder();
				NumberFormat.getInstance().setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);
				numberOfSalesText.setText(NumberFormat.getInstance().format(numberOfSales));
			}
		}
	}

	public class NumberOfSalesWatcher implements TextWatcher {

		private ProductEditText productEditView;

		public NumberOfSalesWatcher(ProductEditText view) {
			this.productEditView = view;
		}

		@Override
		public void afterTextChanged(Editable s) {

		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {

			Product product = productEditView.getProduct();
			if (s.length() == 0) {
				registerManager.updateOrder(product, 0);
			} else {
				registerManager.updateOrder(product, Integer.parseInt(s.toString()));
			}
		}

	}

}