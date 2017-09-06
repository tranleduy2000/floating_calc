package io.codetail.animation;

import android.animation.Animator;

public abstract class SupportAnimator extends Animator {

    /**
     * @return true if using native android animation framework, otherwise is
     * nineoldandroids
     */
    public abstract boolean isNativeAnimator();

    /**
     * @return depends from {@link android.os.Build.VERSION} if sdk version
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} and greater will return
     * {@link Animator} otherwise {@link com.nineoldandroids.animation.Animator}
     */
    public abstract Object get();

    /**
     * Returns whether this Animator is currently running (having been started and gone past any
     * initial startDelay period and not yet ended).
     *
     * @return Whether the Animator is running.
     */
    public abstract boolean isRunning();
}
