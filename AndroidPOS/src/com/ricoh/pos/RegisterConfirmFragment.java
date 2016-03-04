package com.ricoh.pos;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.ricoh.pos.data.OrderUpdateInfo;
import com.ricoh.pos.data.WomanShopFormatter;
import com.ricoh.pos.model.RegisterManager;
import com.ricoh.pos.model.UpdateOrderListener;

import java.math.BigDecimal;
import java.text.NumberFormat;

public class RegisterConfirmFragment extends Fragment implements UpdateOrderListener{
	// This is the maximum fraction digits for total payment to display.
	private static final int MAXIMUM_FRACTION_DIGITS = 2;
	private OnButtonClickListener buttonClickListener;
	private String[] userAttributes;
	private RegisterManager registerManager = RegisterManager.getInstance();

	@Override  
	public void onAttach(Activity activity) {  
		super.onAttach(activity); 
		if (activity instanceof OnButtonClickListener == false) {  
			throw new ClassCastException("okButtonClickListener isn't implemented");  
		}  
		buttonClickListener = (OnButtonClickListener) activity;  
	}
	
	@Override  
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		registerManager.setUpdateOrderListener(this);
		userAttributes = getResources().getStringArray(R.array.user_attributes);
	}

	@Override  
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_register_confirm_table, container, false);
		
		EditText discountView = (EditText) v.findViewById(R.id.discountValue);
		discountView.addTextChangedListener(new DiscountWatcher());

		Button ok_button = (Button) v.findViewById(R.id.ok_button);
		ok_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (buttonClickListener != null) {  
					buttonClickListener.onOkClicked();  
				}
			}
		});
		
		Button cancel_button = (Button) v.findViewById(R.id.cancel_button);
		cancel_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (buttonClickListener != null) {  
					buttonClickListener.onCancelClicked();  
				}
			}
		});
		
		long totalAmount = registerManager.getOriginalTotalAmount();
		updateTotalAmount(v, totalAmount, totalAmount);
		
		// Add Spinner for User Attributes 
	    Spinner userAttributesSpinner = (Spinner)v.findViewById(R.id.spinner1);
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),R.layout.spinner_item,userAttributes);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    userAttributesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                String selectedAttribute = userAttributes[spinner.getSelectedItemPosition()];
                registerManager.setUserAttribute(getName(selectedAttribute));
;            }

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				throw new IllegalStateException("User attributes have to be selected");
			}
        });
	    userAttributesSpinner.setAdapter(adapter);
		return v;
	}
	
	private String getName(String selectedAttribute){

		String[] userAttributeKeys = getResources().getStringArray(R.array.user_attribute_keys);
		for(int i = 0; i<userAttributes.length;i++){
			if(userAttributes[i].equals(selectedAttribute)){
				return userAttributeKeys[i];
			}
		}
		
		return null;
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		registerManager.removeUpdateOrderListener(this);
	}

	@Override
	public void notifyUpdateOrder(OrderUpdateInfo orderInfo) {
		updateTotalAmount(getView(),
				orderInfo.getTotalAmountBeforeDiscount(),
				orderInfo.getTotalAmountAfterDiscount());
	}
	
	private void updateTotalAmount(View view, long totalPayment, long totalPaymentAfterDiscount)
	{
		double totalPaymentRupee = WomanShopFormatter.convertPaisaToRupee(totalPayment);
		double totalPaymentRupeeAfterDiscount = WomanShopFormatter.convertPaisaToRupee(totalPaymentAfterDiscount);

		TextView totalPaymentView = (TextView) view.findViewById(R.id.beforwTotalAmountView);
		NumberFormat format = NumberFormat.getInstance();
		format.setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);
		totalPaymentView.setText(format.format(totalPaymentRupee) + getString(R.string.currency_india));
		
		TextView totalPaymentViewAfterDiscount = (TextView) view.findViewById(R.id.totalPaymentView);
		totalPaymentViewAfterDiscount.setText(format.format(totalPaymentRupeeAfterDiscount) + getString(R.string.currency_india));
	}

	public interface OnButtonClickListener { 
		public void onPriceDownClicked();
		public void onOkClicked(); 
		public void onCancelClicked(); 
	}
	
	public class DiscountWatcher implements TextWatcher {
		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			try {
				registerManager.updateDiscountValue(WomanShopFormatter.convertRupeeToPaisa(s.toString()));
			} catch (IllegalArgumentException e) {

			}
		}
	}
}
