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
import android.view.View;
import android.widget.LinearLayout;
import eu.ttbox.gabuzomeu.Calculator;
import eu.ttbox.gabuzomeu.CalculatorEditText;
import eu.ttbox.gabuzomeu.R;
import eu.ttbox.gabuzomeu.service.GabuzomeuConverter;

public class CalculatorConverterDisplay extends LinearLayout {

    private static final String TAG = "CalculatorConverterDisplay";

    public static final int FIELD_FOCUS_NUMBER = Calculator.BASIC_PANEL;
    public static final int FIELD_FOCUS_SHADOK = Calculator.SHADOK_PANEL;

    private GabuzomeuConverter converter;

    private CalculatorEditText calculatorEditText;
    private CalculatorEditText converterEditText;
    private CalculatorEditText converterSmallEditText;

    private OnFocusPanelListener onFocusPanelListener;

    public interface OnFocusPanelListener {
        void onFocusChangeTo(int panelfocus);

    }

    public CalculatorConverterDisplay(final Context context, AttributeSet attrs) {
        super(context, attrs);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.converter_display, this, true);

        converter = new GabuzomeuConverter(context);
        calculatorEditText = (CalculatorEditText) findViewById(R.id.display_calculator_EditText);
        converterEditText = (CalculatorEditText) findViewById(R.id.display_converter_EditText);
        converterSmallEditText = (CalculatorEditText) findViewById(R.id.display_converter_name_EditText);

        // Font
        Typeface font = GabuzomeuConverter.getSymbolFont(getContext());
        converterEditText.setTypeface(font);

        // Listener
        calculatorEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && onFocusPanelListener != null) {
                    onFocusPanelListener.onFocusChangeTo(FIELD_FOCUS_NUMBER);
                }
            }
        });
        converterEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && onFocusPanelListener != null) {
                    onFocusPanelListener.onFocusChangeTo(FIELD_FOCUS_SHADOK);
                }
            }
        });
        converterSmallEditText.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && onFocusPanelListener != null) {
                    onFocusPanelListener.onFocusChangeTo(FIELD_FOCUS_SHADOK);
                }
            }
        });
    }

    public void setOnFocusPanelListener(OnFocusPanelListener onFocusPanelListener) {
        this.onFocusPanelListener = onFocusPanelListener;
    }

    public int getFocusFieldCode() {
        return converterEditText.hasFocus() ? FIELD_FOCUS_NUMBER : FIELD_FOCUS_SHADOK;
    }

//    private CalculatorEditText getFocusField() {
//        return converterEditText.hasFocus() ? converterEditText : calculatorEditText;
//    }

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
        converterEditText.setSelection(shadokDigit.length());
        converterSmallEditText.setText(shadokDigitName);
    }

    private void converterToBase10(CharSequence text) {
        Log.w(TAG, "converterToBase10  : " + text);
        CharSequence numberDigit = text;
        CharSequence shadokDigitName = text;
        int textSize = text == null ? 0 : text.length();
        if (textSize > 0) {
            StringBuilder convertDigit = new StringBuilder(textSize);
            StringBuilder convertDigitName = new StringBuilder(textSize * 4);
            converter.decodeShadokDigitEquationToBase10Code(text, convertDigit, convertDigitName);
            numberDigit = convertDigit.toString();
            shadokDigitName = convertDigitName.toString();
        }
        calculatorEditText.setText(numberDigit);
        calculatorEditText.setSelection(numberDigit.length());
        converterSmallEditText.setText(shadokDigitName);
    }

    public void insert(String delta) {
        // editor
        int cursor = calculatorEditText.getSelectionStart();
        boolean hasFocus = calculatorEditText.hasFocus();
        if (!hasFocus) {
            cursor = calculatorEditText.getText().length();
        }
        Log.w(TAG, String.format("Insert delta : %s at position %s with focus %s of size %s", delta, cursor, hasFocus, calculatorEditText.getText().length()));
        calculatorEditText.getText().insert(cursor, delta);
        // Converter
        converterToShadok(calculatorEditText.getText().toString());
    }

    public void insertGaBuZoMeu(String delta) {
        int cursor = converterEditText.getSelectionStart();
        boolean hasFocus = converterEditText.hasFocus();
        if (!hasFocus) {
            cursor = converterEditText.getText().length();
        }
        Log.w(TAG, String.format("insertGaBuZoMeu delta : %s at position %s with focus %s of size %s", delta, cursor, hasFocus, converterEditText.getText().length()));
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
    }

}
