package com.ricoh.pos;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ricoh.pos.data.WomanShopFormatter;
import com.ricoh.pos.model.RegisterManager;

import java.math.BigDecimal;

public class PriceDownDialog {
	static TextView prompt;
	static TextView promptValue;

	static Button btn1;
	static Button btn2;
	static Button btn3;
	static Button btn4;
	static Button btn5;
	static Button btn6;
	static Button btn7;
	static Button btn8;
	static Button btn9;
	static Button btn0;
	static Button btnC;
	static Button btnDot;

	private String value = "";
	private final String title = "Price Down";
	private final String inputMessage = "Input down value";

	public String getValue() {
		return value;
	}

	public void show(final Activity activity) {
		Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(title);
		LayoutInflater inflater = activity.getLayoutInflater();
		View view = inflater.inflate(R.layout.numb_pad, null, false);

		// create code to handle the change tender
		prompt = (TextView) view.findViewById(R.id.promptText);
		prompt.setText(inputMessage);
		if (inputMessage.equals("")) {
			prompt.setVisibility(View.GONE);
		}
		promptValue = (TextView) view.findViewById(R.id.promptValue);

		// Defaults
		value = "";
		promptValue.setText("");

		btn1 = (Button) view.findViewById(R.id.button1);
		btn2 = (Button) view.findViewById(R.id.button2);
		btn3 = (Button) view.findViewById(R.id.button3);
		btn4 = (Button) view.findViewById(R.id.button4);
		btn5 = (Button) view.findViewById(R.id.button5);
		btn6 = (Button) view.findViewById(R.id.button6);
		btn7 = (Button) view.findViewById(R.id.button7);
		btn8 = (Button) view.findViewById(R.id.button8);
		btn9 = (Button) view.findViewById(R.id.button9);
		btn0 = (Button) view.findViewById(R.id.button0);
		btnC = (Button) view.findViewById(R.id.buttonC);
		btnDot = (Button) view.findViewById(R.id.buttonDot);

		btnC.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				value = "";
				promptValue.setText("");
			}
		});
		btn1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("1");
			}
		});
		btn2.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("2");
			}
		});
		btn3.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("3");
			}
		});
		btn4.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("4");
			}
		});
		btn5.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("5");
			}
		});
		btn6.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("6");
			}
		});
		btn7.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("7");
			}
		});
		btn8.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("8");
			}
		});
		btn9.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("9");
			}
		});
		btn0.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber("0");
			}
		});
		btnDot.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				appendNumber(".");
			}
		});

		builder.setView(view);
		builder.setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int sumthin) {
				dlg.dismiss();
				try {
					RegisterManager.getInstance().updateDiscountValue(WomanShopFormatter.convertRupeeToPaisa(value));
				} catch (IllegalArgumentException e) {
					Toast.makeText(activity.getBaseContext(), R.string.discount_error, Toast.LENGTH_LONG).show();
				}
			}
		});
		builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dlg, int sumthin) {
				dlg.dismiss();
			}
		});
		builder.show();
	}

	void appendNumber(String inNumb) {
		value = value + inNumb;
		promptValue.setText(promptValue.getText() + inNumb);
	}
}