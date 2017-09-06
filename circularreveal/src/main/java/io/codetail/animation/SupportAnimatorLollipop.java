package io.codetail.animation;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.os.Build;

import java.lang.ref.WeakReference;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
final class SupportAnimatorLollipop extends SupportAnimator {

    WeakReference<Animator> mNativeAnimator;

    SupportAnimatorLollipop(Animator animator) {
        mNativeAnimator = new WeakReference<Animator>(animator);
    }

    @Override
    public boolean isNativeAnimator() {
        return true;
    }

    @Override
    public Object get() {
        return mNativeAnimator.get();
    }


    @Override
    public void start() {
        Animator a = mNativeAnimator.get();
        if (a != null) {
            a.start();
        }
    }

    @Override
    public long getDuration() {
        Animator a = mNativeAnimator.get();
        if (a != null) {
            return a.getDuration();
        }
        return 0;
    }

    @Override
    public SupportAnimatorLollipop setDuration(long duration) {
        Animator a = mNativeAnimator.get();
        if (a != null) {
            a.setDuration(duration);
        }
        return this;
    }

    @Override
    public long getStartDelay() {
        Animator a = mNativeAnimator.get();
        if (a != null) {
            return a.getStartDelay();
        }
        return 0;
    }

    @Override
    public void setStartDelay(long delay) {
        Animator a = mNativeAnimator.get();
        if (a != null) {
            a.setStartDelay(delay);
        }
    }

    @Override
    public void setInterpolator(TimeInterpolator value) {
        Animator a = mNativeAnimator.get();
        if (a != null) {
            a.setInterpolator(value);
        }
    }

    @Override
    public void addListener(final AnimatorListener listener) {
        Animator a = mNativeAnimator.get();
        if (a == null) {
            return;
        }
        a.addListener(listener);
    }

    @Override
    public boolean isRunning() {
        Animator a = mNativeAnimator.get();
        return a != null && a.isRunning();
    }
}
