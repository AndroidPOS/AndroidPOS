package com.ricoh.pos;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ricoh.pos.data.Order;
import com.ricoh.pos.data.Product;
import com.ricoh.pos.data.WomanShopFormatter;

import java.io.FileNotFoundException;
import java.text.NumberFormat;

public class CategoryDetailFragment extends GetProductFragment {

    @Override
    public void onResume() {
        super.onResume();
        setListAdapter(new ListAdapter(getActivity()));
    }

    public class ListAdapter extends BaseAdapter {

        private LayoutInflater inflater;

        public ListAdapter(Context context) {
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

            try {
                Bitmap image = product.decodeProductImage(IMAGE_VIEW_SIZE, IMAGE_VIEW_SIZE);
                imageView.setImageBitmap(image);
                imageView.setVisibility(View.VISIBLE);
            } catch (FileNotFoundException e) {
                imageView.setVisibility(View.INVISIBLE);
            }

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
                        Toast.makeText(getActivity(), R.string.error_image_file_not_found, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void setProductInformationView(View convertView, Product product) {
            TextView textView = (TextView) convertView.findViewById(R.id.filename);
            String productName = product.getName();
            if (productName == null || productName.isEmpty()) {
                throw new IllegalArgumentException("Argument is NOT correct");
            }
            textView.setText(productName);

            NumberFormat.getInstance().setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);

            TextView priceView = (TextView) convertView.findViewById(R.id.price);
            priceView.setText(NumberFormat.getInstance().format(
                    WomanShopFormatter.convertPaisaToRupee(product.getPrice())) + getString(R.string.currency_india));

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