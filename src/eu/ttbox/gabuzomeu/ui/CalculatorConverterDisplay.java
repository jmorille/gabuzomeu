package eu.ttbox.gabuzomeu.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Editable.Factory;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import eu.ttbox.gabuzomeu.CalculatorEditText;
import eu.ttbox.gabuzomeu.R;
import eu.ttbox.gabuzomeu.service.GabuzomeuConverter;

public class CalculatorConverterDisplay extends LinearLayout {

    private CalculatorEditText calculatorEditText;
    private CalculatorEditText converterEditText;

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

        calculatorEditText = (CalculatorEditText) findViewById(R.id.display_calculator_EditText);
        converterEditText = (CalculatorEditText) findViewById(R.id.display_converter_EditText);
    }

    public final void setText(CharSequence text) {
        calculatorEditText.setText(text);
        converterToShadok(text);
    }

    private void converterToShadok(CharSequence text) {
        CharSequence shadok = text;
        if (text.length() > 0) {
            shadok = GabuzomeuConverter.encodeTobase4(Integer.valueOf(text.toString()));
        }
        converterEditText.setText(shadok);
    }
    
    public void insert(String delta) {
        // editor
        int cursor = calculatorEditText.getSelectionStart();
        calculatorEditText.getText().insert(cursor, delta);
        // Converter
        converterToShadok(calculatorEditText.getText().toString());
//        int cursorConv = converterEditText.getSelectionStart();
//        converterEditText.getText().insert(cursorConv, delta);
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
    }

    public void setBackgroundDrawable(Drawable d) {
        calculatorEditText.setBackgroundDrawable(d);
        converterEditText.setBackgroundDrawable(d);
    }

}
