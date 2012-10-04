package eu.ttbox.gabuzomeu.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Editable.Factory;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import eu.ttbox.gabuzomeu.CalculatorEditText;
import eu.ttbox.gabuzomeu.R;
import eu.ttbox.gabuzomeu.service.GabuzomeuConverter;

public class CalculatorConverterDisplay extends LinearLayout {

	private static final String TAG = "CalculatorConverterDisplay";

	
	private GabuzomeuConverter converter;

	private CalculatorEditText calculatorEditText;
	private CalculatorEditText converterEditText;
	private CalculatorEditText converterSmallEditText;

	// public CalculatorConverterDisplay(Context context, AttributeSet attrs,
	// int defStyle) {
	// super(context, attrs, defStyle);
	// }
	//
	// public CalculatorConverterDisplay(Context context, AttributeSet attrs) {
	// super(context, attrs);
	// }

	public CalculatorConverterDisplay(Context context, AttributeSet attrs) {
		super(context, attrs);

		final LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.converter_display, this, true);

		converter = new GabuzomeuConverter(context);
		calculatorEditText = (CalculatorEditText) findViewById(R.id.display_calculator_EditText);
		converterEditText = (CalculatorEditText) findViewById(R.id.display_converter_EditText);
		converterSmallEditText = (CalculatorEditText) findViewById(R.id.display_converter_name_EditText);
	}

	public final void setText(CharSequence text) {
		calculatorEditText.setText(text);
		converterToShadok(text);
	}

	private void converterToShadok(CharSequence text) {
		CharSequence shadokDigit = text;
		CharSequence shadokDigitName = text;
		int textSize = text == null ? 0 : text.length();
		if (textSize > 0) {
			StringBuilder convertDigit = new StringBuilder(textSize * 2);
			StringBuilder convertDigitName = new StringBuilder(textSize * 8);
			converter.encodeEquationToShadokCode(text, convertDigit, convertDigitName);
			shadokDigit = convertDigit.toString();
			shadokDigitName = convertDigitName.toString();
		}
		converterEditText.setText(shadokDigit);
		converterSmallEditText.setText(shadokDigitName);
	}

	private void converterToBase10(CharSequence text) {
		Log.w(TAG, "converterToBase10  : " + text);
		CharSequence numberDigit = text;
		CharSequence shadokDigitName = text;
		int textSize = text == null ? 0 : text.length();
		if (textSize > 0) {
			StringBuilder convertDigit = new StringBuilder(textSize  );
//			StringBuilder convertDigitName = new StringBuilder(textSize *4);
			converter.decodeShadokDigitEquationToBase10Code(text,  convertDigit);
			numberDigit = convertDigit.toString();
//			shadokDigitName = convertDigitName.toString();
		}
		calculatorEditText.setText(numberDigit);
	}
	public void insert(String delta) {
		// editor
		int cursor = calculatorEditText.getSelectionStart();
		calculatorEditText.getText().insert(cursor, delta);
		// Converter
		converterToShadok(calculatorEditText.getText().toString());
	}

	public void insertGaBuZoMeu(String delta) {
		int cursor = converterEditText.getSelectionStart();
		converterEditText.getText().insert(cursor, delta);
		// TODO Recompute the tow other
		converterToBase10(converterEditText.getText().toString());
	}
	
	public void setSelection(int length) {
		calculatorEditText.setSelection(length);
	}

	public Editable getText() {
		return calculatorEditText.getText();
	}

	public int getSelectionStart() {
		return calculatorEditText.getSelectionStart();
	}

	public int length() {
		return calculatorEditText.length();
	}

	public void setEditableFactory(Factory factory) {
		calculatorEditText.setEditableFactory(factory);
	}

	public void setKeyListener(NumberKeyListener calculatorKeyListener) {
		calculatorEditText.setKeyListener(calculatorKeyListener);
		// converterEditText.setKeyListener(calculatorKeyListener);
	}

	public void setSingleLine() {
		calculatorEditText.setSingleLine();
		converterEditText.setSingleLine();
		converterSmallEditText.setSingleLine();
	}

	public void setBackgroundDrawable(Drawable d) {
		calculatorEditText.setBackgroundDrawable(d);
		converterEditText.setBackgroundDrawable(d);
		converterSmallEditText.setBackgroundDrawable(d);
		// Font 
		Typeface font = GabuzomeuConverter.getSymbolFont(getContext());
		converterEditText.setTypeface(font);
	}



}
