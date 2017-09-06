package com.duy.calculator.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.duy.calculator.R;

/**
 * A ViewGroup that can be set to disallow touch events on its parents or children.
 */
public class SolidLayout extends FrameLayout {
    private boolean mPreventParentTouchEvents;
    private boolean mPreventChildTouchEvents;

    public SolidLayout(Context context) {
        this(context, null);
    }

    public SolidLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SolidLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SolidLayout, 0, 0);
            mPreventParentTouchEvents = a.getBoolean(R.styleable.SolidLayout_preventParentTouchEvents, mPreventParentTouchEvents);
            mPreventChildTouchEvents = a.getBoolean(R.styleable.SolidLayout_preventChildTouchEvents, mPreventChildTouchEvents);
            a.recycle();
        }
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
