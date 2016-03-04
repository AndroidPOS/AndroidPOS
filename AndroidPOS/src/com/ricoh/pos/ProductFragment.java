package com.ricoh.pos;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ricoh.pos.data.Product;
import com.ricoh.pos.data.WomanShopFormatter;
import com.ricoh.pos.model.ProductsManager;
import com.ricoh.pos.model.WomanShopIOManager;

import java.io.FileNotFoundException;
import java.text.NumberFormat;




public class ProductFragment extends GetProductFragment {

    private final static String productCodeKeyName="productCode";
    private final static String productCategoryKeyName="productCategory";

    public void refresh() {
        setListAdapter(new ListAdapter(getActivity()));
    }

    public void showDialogFragment(Product product) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ChangeStockDialogFragment fragment = new ChangeStockDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putString(productCodeKeyName, product.getCode());
        bundle.putString(productCategoryKeyName, product.getCategory());

        fragment.setArguments(bundle);
        fragment.show(getActivity().getFragmentManager(),null);
    }

    public static class ChangeStockDialogFragment extends DialogFragment {

        private final int IMAGE_VIEW_SIZE = 120;
        private Product product;

        public void getProductData() {
            String productCode = getArguments().getString(productCodeKeyName);
            String productCategory = getArguments().getString(productCategoryKeyName);
            this.product = ProductsManager.getInstance().getProductFromCode(productCategory, productCode);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            getProductData();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View content = inflater.inflate(R.layout.change_stock_num_dialog, null);
            builder.setView(content);

            setImageViewForDialog(content, product);
            setProductInformationView(content, product);

            final EditText changingEditText = (EditText) content.findViewById(R.id.changing_stock_num);
            changingEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            changingEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            changingEditText.setText(String.valueOf(product.getStock()));
            changingEditText.setSelection(String.valueOf(product.getStock()).length());

            ProductButton plusBtn = (ProductButton) content.findViewById(R.id.plusButton);
            plusBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String changingEditTextString = changingEditText.getText().toString();
                    int changingEditTextInteger = 0;
                    try {
                        changingEditTextInteger = Integer.parseInt(changingEditTextString);
                        changingEditTextInteger++;
                    } catch (NumberFormatException e) {
                        Toast.makeText(ChangeStockDialogFragment.this.getActivity(), getString(R.string.edit_stock_in_dialog_warning), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (changingEditTextInteger < 0) {
                            changingEditTextInteger = 0;
                        }
                        changingEditTextString = String.valueOf(changingEditTextInteger);
                        changingEditText.setText(changingEditTextString);
                    }
                }
            });

            ProductButton minusBtn = (ProductButton) content.findViewById(R.id.minusButton);
            minusBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String changingEditTextString = changingEditText.getText().toString();
                    int num = 0;
                    try {
                        num = Integer.parseInt(changingEditTextString);
                        num = num - 1;

                    } catch (NumberFormatException e) {
                        Toast.makeText(ChangeStockDialogFragment.this.getActivity(), getString(R.string.edit_stock_in_dialog_warning), Toast.LENGTH_SHORT).show();
                    } finally {
                        if (num < 0) {
                            num = 0;
                        }
                        changingEditTextString = String.valueOf(num);
                        changingEditText.setText(changingEditTextString);
                    }
                }
            });

            builder.setPositiveButton(getString(R.string.ok_edit), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        if (Integer.parseInt(changingEditText.getText().toString()) < 0) {
                            changingEditText.setText("0");
                        }
                        product.setStock(Integer.parseInt(changingEditText.getText().toString()));
                        confirmEdit(product.getCode(), changingEditText.getText().toString());

                        FragmentActivity activity = (FragmentActivity) getActivity();
                        ProductFragment fragment = (ProductFragment) activity.getSupportFragmentManager().findFragmentById(R.id.product_list);
                        fragment.refresh();
                    } catch (NumberFormatException e) {
                        Toast.makeText(ChangeStockDialogFragment.this.getActivity(), getString(R.string.edit_stock_in_dialog_warning), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton(getString(R.string.cancel_edit), null);
            AlertDialog dialog = builder.create();

            return dialog;
        }

        private void confirmEdit(String productCode, String productStock) {

            if (Integer.parseInt(productStock) < 0 || Integer.parseInt(productStock) > Integer.MAX_VALUE) {
                productStock = "0";
            }

            WomanShopIOManager womanShopIOManager = new WomanShopIOManager();
            DatabaseHelper databaseHelper = new DatabaseHelper(this.getActivity());
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            womanShopIOManager.setDatabase(db);
            womanShopIOManager.updateStock(productCode, productStock);
            womanShopIOManager.closeDatabase();
        }

        private void setImageViewForDialog(View convertView, final Product product) {
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
                        Log.e("ProductFragment", "Product image file is not found.", e);
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

            TextView stockView = (TextView) convertView.findViewById(R.id.stock);
            int stock = product.getStock();
            stockView.setText(NumberFormat.getInstance().format(stock));

            if (stock <= 0) {
                stockView.setTextColor(getResources().getColor(R.color.warn));
            } else {
                stockView.setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }

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
                convertView = inflater.inflate(R.layout.row_product_for_edit, null);
            }

            final Product product = searchProductList.get(position);
            setImageViewForProductFragment(convertView, product);
            setProductInformationView(convertView, product);
            convertView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    showDialogFragment(product);
                }
            });
            return convertView;
        }

        private void setImageViewForProductFragment(View convertView, final Product product) {
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
                        Log.e("ProductFragment", "Product image file is not found.", e);
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
            priceView.setText(NumberFormat.getInstance().format(WomanShopFormatter.convertPaisaToRupee(product.getPrice())));

            TextView costView = (TextView) convertView.findViewById(R.id.cost);
            costView.setText(NumberFormat.getInstance().format(WomanShopFormatter.convertPaisaToRupee(product.getOriginalCost())));

            TextView stockView = (TextView) convertView.findViewById(R.id.stock);
            int stock = product.getStock();
            stockView.setText(NumberFormat.getInstance().format(stock));

            if (stock <= 0) {
                stockView.setTextColor(getResources().getColor(R.color.warn));
            } else {
                stockView.setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }
}