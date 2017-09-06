package io.codetail.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import io.codetail.animation.RevealAnimator;

public class RevealView extends View implements RevealAnimator {

    final Paint mPaint = new Paint();
    Path mRevealPath;
    float mCenterX;
    float mCenterY;
    float mRadius;
    View mTarget;

    public RevealView(Context context) {
        this(context, null);
    }

    public RevealView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RevealView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mRevealPath = new Path();
    }

    @Override
    public void setClipOutlines(boolean clip) {

    }

    /**
     * Animation target
     *
     * @hide
     */
    @Override
    public void setTarget(View view) {
        mTarget = view;
    }

    /**
     * Epicenter of animation circle reveal
     *
     * @hide
     */
    @Override
    public void setCenter(float centerX, float centerY) {
        mCenterX = centerX;
        mCenterY = centerY;
    }

    /**
     * Circle radius size
     *
     * @hide
     */
    @Override
    public float getRevealRadius() {
        return mRadius;
    }

    /**
     * Circle radius size
     *
     * @hide
     */
    @Override
    public void setRevealRadius(float radius) {
        mRadius = radius;
        invalidate();
    }

    public void setRevealColor(int color) {
        mPaint.setColor(color);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mTarget == null) {
            canvas.drawColor(mPaint.getColor());
        } else {
            final int state = canvas.save();

            mRevealPath.reset();
            mRevealPath.addCircle(mCenterX, mCenterY, mRadius, Path.Direction.CW);

            canvas.drawPath(mRevealPath, mPaint);

            canvas.restoreToCount(state);
        }
    }
}
