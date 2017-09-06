package com.duy.calculator.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.duy.calculator.R;

/**
 * A ViewGroup that can be set to disallow touch events on its parents or children.
 */
public class SolidPadLayout extends CalculatorPadLayout {
    private static final String STATE_SUPER = "super";
    private static final String STATE_PREVENT_PARENT_TOUCH_EVENTS = "prevent_parent_touch_events";
    private static final String STATE_PREVENT_CHILD_TOUCH_EVENTS = "prevent_child_touch_events";

    private boolean mPreventParentTouchEvents;
    private boolean mPreventChildTouchEvents;

    public SolidPadLayout(Context context) {
        this(context, null);
    }

    public SolidPadLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SolidPadLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SolidLayout, 0, 0);
            mPreventParentTouchEvents = a.getBoolean(R.styleable.SolidLayout_preventParentTouchEvents, mPreventParentTouchEvents);
            mPreventChildTouchEvents = a.getBoolean(R.styleable.SolidLayout_preventChildTouchEvents, mPreventChildTouchEvents);
            a.recycle();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());
        bundle.putBoolean(STATE_PREVENT_PARENT_TOUCH_EVENTS, mPreventParentTouchEvents);
        bundle.putBoolean(STATE_PREVENT_CHILD_TOUCH_EVENTS, mPreventChildTouchEvents);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        mPreventParentTouchEvents = bundle.getBoolean(STATE_PREVENT_PARENT_TOUCH_EVENTS);
        mPreventChildTouchEvents = bundle.getBoolean(STATE_PREVENT_CHILD_TOUCH_EVENTS);
        super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mPreventChildTouchEvents) {
            return true;
        }
        if (mPreventParentTouchEvents) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void setPreventParentTouchEvents(boolean prevent) {
        mPreventParentTouchEvents = prevent;
    }

    public void setPreventChildTouchEvents(boolean prevent) {
        mPreventChildTouchEvents = prevent;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }
}
