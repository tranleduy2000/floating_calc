/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.duy.calculator.view;

import android.content.Context;
import android.util.AttributeSet;

import com.duy.calculator.R;

public class BackspaceImageButton extends android.support.v7.widget.AppCompatImageButton {
    private static final int[] STATE_DELETE = {R.attr.state_delete};
    private static final int[] STATE_CLEAR = {R.attr.state_clear};
    private State mState = State.DELETE;

    public BackspaceImageButton(Context context) {
        super(context);
        setup();
    }

    public BackspaceImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public BackspaceImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        setState(State.DELETE);
    }

    public State getState() {
        return mState;
    }

    public void setState(State state) {
        mState = state;
        refreshDrawableState();
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        int[] state = super.onCreateDrawableState(extraSpace + 1);
        if (mState == null) mState = State.DELETE;

        switch (mState) {
            case DELETE:
                mergeDrawableStates(state, STATE_DELETE);
                break;
            case CLEAR:
                mergeDrawableStates(state, STATE_CLEAR);
                break;
        }
        return state;
    }

    public enum State {
        DELETE, CLEAR
    }
}
