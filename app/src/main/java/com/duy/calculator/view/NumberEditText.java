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
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

/**
 * NumberEditText disables the keyboard and most EditText touch events.
 * It also restricts the characters allowed from physical keyboards to only numbers
 * and a few operations.
 */
public class NumberEditText extends ResizingEditText {
    // Restrict keys from hardware keyboards
    private static final char[] ACCEPTED_CHARS = "0123456789.+-*/\u2212\u00d7\u00f7()!%^".toCharArray();

    private final Editable.Factory mFactory = new CalculatorEditable.Factory();

    public NumberEditText(Context context) {
        super(context);
        setUp();
    }

    public NumberEditText(Context context, AttributeSet attr) {
        super(context, attr);
        setUp();
    }

    private void setUp() {
        setEditableFactory(mFactory);
        setKeyListener(new KeyListener());
    }

    public Editable.Factory getEditableFactory() {
        return mFactory;
    }

    public void backspace() {
        String text = getText().toString();
        int selectionHandle = getSelectionStart();
        String textBeforeInsertionHandle = text.substring(0, selectionHandle);
        String textAfterInsertionHandle = text.substring(selectionHandle, text.length());

        if (selectionHandle != 0) {
            setText(textBeforeInsertionHandle.substring(0, textBeforeInsertionHandle.length() - 1)
                    + textAfterInsertionHandle);
            setSelection(selectionHandle - 1);
        }
    }

    private class KeyListener extends NumberKeyListener {
        @Override
        public int getInputType() {
            return EditorInfo.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        }

        @Override
        protected char[] getAcceptedChars() {
            return ACCEPTED_CHARS;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                /*
                 * the EditText should still accept letters (eg. 'sin') coming from the on-screen touch buttons, so don't filter anything.
                 */
            return null;
        }

        @Override
        public boolean onKeyDown(View view, Editable content, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                NumberEditText.this.backspace();
            }
            return super.onKeyDown(view, content, keyCode, event);
        }
    }
}
