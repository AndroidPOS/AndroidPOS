package com.ricoh.pos;

import java.text.NumberFormat;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ricoh.pos.model.RegisterManager;
import com.ricoh.pos.model.UpdateOrderListener;

public class TotalPaymentFragment extends Fragment implements UpdateOrderListener{

	// This is the maximum fraction digits for total payment to display.
	private static final int MAXIMUM_FRACTION_DIGITS = 2;
	private OnOkButtonClickListener okButtonClickListener; 

	@Override  
	public void onAttach(Activity activity) {  
		super.onAttach(activity); 
		if (activity instanceof OnOkButtonClickListener == false) {  
			throw new ClassCastException("okButtonClickListener isn't implemented");  
		}  
		okButtonClickListener = (OnOkButtonClickListener) activity;  
	}

	@Override  
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		RegisterManager.getInstance().setUpdateOrderListener(this);
	}

	@Override  
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_total_payment, container, false);

		Button ok_button = (Button) v.findViewById(R.id.total_payment_ok_button);
		ok_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (okButtonClickListener != null) {  
					okButtonClickListener.onOkClicked();  
				}
			}
		});
		return v;
	}

	@Override
	public void notifyUpdateOrder(double totalPayment) {
		TextView totalPaymentView = (TextView) getView().findViewById(R.id.totalPaymentView);

		NumberFormat format = NumberFormat.getInstance();
		format.setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);
		totalPaymentView.setText(format.format(totalPayment) + " Rp");
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		RegisterManager.getInstance().clearUpdateOrderListener();
	}

	public interface OnOkButtonClickListener {  
		public void onOkClicked();  
	}  
}
