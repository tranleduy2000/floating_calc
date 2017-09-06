package com.duy.calculator.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

/**
 * Created by Duy on 9/5/2017.
 */

public class CalculatorButton extends android.support.v7.widget.AppCompatButton {
    public CalculatorButton(Context context) {
        super(context);
        setup(context);
    }

    public CalculatorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);

    }

    public CalculatorButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup(context);
    }

    private void setup(Context context) {
        setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/RobotoMono-Light.ttf"));
    }
}
