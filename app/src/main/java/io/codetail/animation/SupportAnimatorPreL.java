package io.codetail.animation;

import android.animation.TimeInterpolator;
import android.view.animation.Interpolator;

import com.nineoldandroids.animation.Animator;

import java.lang.ref.WeakReference;

final class SupportAnimatorPreL extends SupportAnimator {

    WeakReference<Animator> mSupportFramework;

    SupportAnimatorPreL(Animator animator) {
        mSupportFramework = new WeakReference<Animator>(animator);
    }

    @Override
    public boolean isNativeAnimator() {
        return false;
    }

    @Override
    public Object get() {
        return mSupportFramework.get();
    }

    @Override
    public void start() {
        Animator a = mSupportFramework.get();
        if (a != null) {
            a.start();
        }
    }

    @Override
    public long getDuration() {
        Animator a = mSupportFramework.get();
        if (a != null) {
            return a.getDuration();
        }
        return 0;
    }

    @Override
    public SupportAnimatorPreL setDuration(long duration) {
        Animator a = mSupportFramework.get();
        if (a != null) {
            a.setDuration(duration);
        }
        return this;
    }

    @Override
    public long getStartDelay() {
        Animator a = mSupportFramework.get();
        if (a != null) {
            return a.getStartDelay();
        }
        return 0;
    }

    @Override
    public void setStartDelay(long delay) {
        Animator a = mSupportFramework.get();
        if (a != null) {
            a.setStartDelay(delay);
        }
    }

    @Override
    public void setInterpolator(final TimeInterpolator value) {
        Animator a = mSupportFramework.get();
        if (a != null) {
            if (value == null) {
                a.setInterpolator(null);
            } else {
                a.setInterpolator(new Interpolator() {
                    @Override
                    public float getInterpolation(float input) {
                        return value.getInterpolation(input);
                    }
                });
            }
        }
    }

    @Override
    public void addListener(final AnimatorListener listener) {
        Animator a = mSupportFramework.get();
        if (a == null) {
            return;
        }
        a.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                listener.onAnimationStart(SupportAnimatorPreL.this);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                listener.onAnimationEnd(SupportAnimatorPreL.this);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                listener.onAnimationCancel(SupportAnimatorPreL.this);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                listener.onAnimationRepeat(SupportAnimatorPreL.this);
            }
        });
    }

    @Override
    public boolean isRunning() {
        Animator a = mSupportFramework.get();
        return a != null && a.isRunning();
    }
}
