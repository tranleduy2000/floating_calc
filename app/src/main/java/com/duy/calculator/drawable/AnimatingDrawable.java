package com.duy.calculator.drawable;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

/**
 * Created by Will on 7/2/2015.
 */
public class AnimatingDrawable extends Drawable implements Animatable {
    private final Drawable[] mFrames;
    private final ValueAnimator mAnimator;
    private Drawable mCurrentFrame;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;

    private AnimatingDrawable(Drawable[] frames, long duration) {
        mFrames = frames;
        mAnimator = ValueAnimator.ofInt(0, mFrames.length - 1);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Normalize the position in case the interporator isn't linear
                int pos = Math.max(Math.min((int) animation.getAnimatedValue(), mFrames.length - 1), 0);
                setFrame(mFrames[pos]);
            }
        });
        mAnimator.setDuration(duration);
        mAnimator.setInterpolator(new LinearInterpolator());

        // Calculate the largest drawable, and use that as our intrinsic width/height
        for (Drawable drawable : mFrames) {
            mIntrinsicWidth = Math.max(mIntrinsicWidth, drawable.getIntrinsicWidth());
            mIntrinsicHeight = Math.max(mIntrinsicHeight, drawable.getIntrinsicWidth());
        }

        setFrame(mFrames[0]);
    }

    public Animator getAnimator() {
        return mAnimator;
    }

    private void setFrame(Drawable drawable) {
        if (mCurrentFrame != drawable) {
            mCurrentFrame = drawable;

            int l = (mIntrinsicWidth - mCurrentFrame.getIntrinsicWidth()) / 2;
            int t = (mIntrinsicHeight - mCurrentFrame.getIntrinsicHeight()) / 2;
            int r = l + mCurrentFrame.getIntrinsicWidth();
            int b = t + mCurrentFrame.getIntrinsicHeight();
            mCurrentFrame.setBounds(l, t, r, b);
            invalidateSelf();
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }

    @Override
    public boolean isRunning() {
        return mAnimator.isRunning();
    }

    @Override
    public void start() {
        if (!isRunning()) {
            mAnimator.start();
        }
    }

    @Override
    public void stop() {
        if (isRunning()) {
            mAnimator.cancel();
        }
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(ColorFilter cf) {

    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public void draw(Canvas canvas) {
        mCurrentFrame.draw(canvas);
    }

    public static class Builder {
        private final Context mContext;
        private Drawable[] mFrames;
        private long mDuration;

        public Builder(Context context) {
            mContext = context;
            mDuration = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        }

        public Builder frames(int... resIds) {
            mFrames = new Drawable[resIds.length];
            for (int i = 0; i < resIds.length; i++) {
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    mFrames[i] = mContext.getResources().getDrawable(resIds[i], null);
                } else {
                    mFrames[i] = mContext.getResources().getDrawable(resIds[i]);
                }
            }
            return this;
        }

        public Builder frames(Drawable... drawables) {
            mFrames = drawables;
            return this;
        }

        public Builder duration(long duration) {
            mDuration = duration;
            return this;
        }

        public AnimatingDrawable build() {
            return new AnimatingDrawable(mFrames, mDuration);
        }
    }
}
