/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ttbox.gabuzomeu;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.animation.TranslateAnimation;
import android.widget.ViewSwitcher;
import eu.ttbox.gabuzomeu.ui.CalculatorConverterDisplay;
import eu.ttbox.gabuzomeu.ui.CalculatorConverterDisplay.OnFocusPanelListener;

/**
 * Provides vertical scrolling for the input/result EditText.
 */
public class CalculatorDisplay extends ViewSwitcher {

    private static final String ATTR_MAX_DIGITS = "maxDigits";
    private static final int DEFAULT_MAX_DIGITS = 10;

    // only these chars are accepted from keyboard
    private static final char[] ACCEPTED_CHARS =
        "0123456789.+-*/\u2212\u00d7\u00f7()!%^".toCharArray();

    private static final int ANIM_DURATION = 500;

    enum Scroll { UP, DOWN, NONE }

    TranslateAnimation inAnimUp;
    TranslateAnimation outAnimUp;
    TranslateAnimation inAnimDown;
    TranslateAnimation outAnimDown;

    private int mMaxDigits = DEFAULT_MAX_DIGITS;

    public CalculatorDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMaxDigits = attrs.getAttributeIntValue(null, ATTR_MAX_DIGITS, DEFAULT_MAX_DIGITS);
    }

    public int getMaxDigits() {
        return mMaxDigits;
    }

    protected void setLogic(Logic logic, OnFocusPanelListener l) {
 

        Editable.Factory factory = new CalculatorEditable.Factory(logic);
        for (int i = 0; i < 2; ++i) {
            CalculatorConverterDisplay text = (CalculatorConverterDisplay) getChildAt(i);
            text.setBackgroundDrawable(null);
            text.setEditableFactory(factory); 
            text.setSingleLine();
            text.setOnFocusPanelListener(l);
        }
    }

    @Override
    public void setOnKeyListener(OnKeyListener l) {
        getChildAt(0).setOnKeyListener(l);
        getChildAt(1).setOnKeyListener(l);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        inAnimUp = new TranslateAnimation(0, 0, h, 0);
        inAnimUp.setDuration(ANIM_DURATION);
        outAnimUp = new TranslateAnimation(0, 0, 0, -h);
        outAnimUp.setDuration(ANIM_DURATION);

        inAnimDown = new TranslateAnimation(0, 0, -h, 0);
        inAnimDown.setDuration(ANIM_DURATION);
        outAnimDown = new TranslateAnimation(0, 0, 0, h);
        outAnimDown.setDuration(ANIM_DURATION);
    }

    void insert(String delta) {
        CalculatorConverterDisplay editor = (CalculatorConverterDisplay) getCurrentView();
        editor.insert(  delta); 
    }
    
	public void insertGaBuZoMeu(String delta) {
		   CalculatorConverterDisplay editor = (CalculatorConverterDisplay) getCurrentView();
	        editor.insertGaBuZoMeu(  delta); 
	}
	

    CalculatorConverterDisplay getEditText() {
        return (CalculatorConverterDisplay) getCurrentView();
    }

    Editable getText() {
        CalculatorConverterDisplay text = (CalculatorConverterDisplay) getCurrentView();
        return text.getText();
    }

    void setText(CharSequence text, Scroll dir) {
        if (getText().length() == 0) {
            dir = Scroll.NONE;
        }

        if (dir == Scroll.UP) {
            setInAnimation(inAnimUp);
            setOutAnimation(outAnimUp);
        } else if (dir == Scroll.DOWN) {
            setInAnimation(inAnimDown);
            setOutAnimation(outAnimDown);
        } else { // Scroll.NONE
            setInAnimation(null);
            setOutAnimation(null);
        }

        CalculatorConverterDisplay editText = (CalculatorConverterDisplay) getNextView();
        editText.setText(text);
        //Calculator.log("selection to " + text.length() + "; " + text);
        editText.setSelection(text.length());
        showNext();
    }

    int getSelectionStart() {
        CalculatorConverterDisplay text = (CalculatorConverterDisplay) getCurrentView();
        return text.getSelectionStart();
    }

    @Override
    protected void onFocusChanged(boolean gain, int direction, Rect prev) {
        //Calculator.log("focus " + gain + "; " + direction + "; " + prev);
        if (!gain) {
            requestFocus();
        }
    }


}
