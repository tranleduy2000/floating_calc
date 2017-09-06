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
import android.text.Html;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.duy.calculator.R;
import com.duy.calculator.util.TextUtil;
import com.duy.math.BaseModule;
import com.duy.math.Constants;
import com.duy.math.EquationFormatter;
import com.duy.math.Solver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FormattedNumberEditText adds more advanced functionality to NumberEditText.
 * <p>
 * Commas will appear as numbers are typed, exponents will be raised, and backspacing
 * on sin( and log( will remove the whole word. Because of the formatting, getText() will
 * no longer return the correct value. getCleanText() has been added instead.
 */
public class FormattedNumberEditText extends NumberEditText {
    private final Set<TextWatcher> mTextWatchers = new HashSet<>();
    private boolean mDebug = false;
    private boolean mTextWatchersEnabled = true;
    private EquationFormatter mEquationFormatter;
    private Solver mSolver;
    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!mTextWatchersEnabled || mSolver == null || getSelectionStart() == -1) return;
            mTextWatchersEnabled = false;
            onFormat(s);
            mTextWatchersEnabled = true;
        }
    };
    private List<String> mKeywords;
    private boolean mIsInserting;

    public FormattedNumberEditText(Context context) {
        super(context);
        setUp(context, null);
    }

    public FormattedNumberEditText(Context context, AttributeSet attr) {
        super(context, attr);
        setUp(context, attr);
    }

    private void setUp(Context context, AttributeSet attrs) {
        // Display ^ , and other visual cues
        mEquationFormatter = new EquationFormatter();
        addTextChangedListener(mTextWatcher);
        invalidateKeywords(context);
    }

    public void invalidateKeywords(Context context) {
        mKeywords = Arrays.asList(
                context.getString(R.string.fun_arcsin) + "(",
                context.getString(R.string.fun_arccos) + "(",
                context.getString(R.string.fun_arctan) + "(",
                context.getString(R.string.fun_sin) + "(",
                context.getString(R.string.fun_cos) + "(",
                context.getString(R.string.fun_tan) + "(",
                context.getString(R.string.fun_arccsc) + "(",
                context.getString(R.string.fun_arcsec) + "(",
                context.getString(R.string.fun_arccot) + "(",
                context.getString(R.string.fun_csc) + "(",
                context.getString(R.string.fun_sec) + "(",
                context.getString(R.string.fun_cot) + "(",
                context.getString(R.string.fun_log) + "(",
                context.getString(R.string.mod) + "(",
                context.getString(R.string.fun_ln) + "(",
                context.getString(R.string.fun_det) + "(",
                context.getString(R.string.fun_transpose) + "(",
                context.getString(R.string.fun_inverse) + "(",
                context.getString(R.string.fun_trace) + "(",
                context.getString(R.string.fun_norm) + "(",
                context.getString(R.string.fun_polar) + "(",
                context.getString(R.string.dx),
                context.getString(R.string.dy),
                context.getString(R.string.op_cbrt) + "(");
    }

    protected void onFormat(Editable s) {
        String text = removeFormatting(s.toString());

        // Get the selection handle, since we're setting text and that'll overwrite it
        MutableInteger selectionHandle = new MutableInteger(getSelectionStart());

        // Adjust the handle by removing any comas or spacing to the left
        String cs = s.subSequence(0, selectionHandle.intValue()).toString();
        selectionHandle.subtract(TextUtil.countOccurrences(cs, mSolver.getBaseModule().getSeparator()));

        // Update the text with formatted (comas, etc) text
        setText(Html.fromHtml(formatText(text, selectionHandle)));
        setSelection(selectionHandle.intValue());
    }

    @Override
    public void addTextChangedListener(TextWatcher watcher) {
        // Some flavors of Android call addTextChangedListener in the constructor, so add a
        // null check to mTextWatchers
        if (watcher.equals(mTextWatcher) || mTextWatchers == null) {
            super.addTextChangedListener(watcher);
        } else {
            mTextWatchers.add(watcher);
        }
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (mTextWatchersEnabled) {
            for (TextWatcher textWatcher : mTextWatchers) {
                textWatcher.beforeTextChanged(getCleanText(), 0, 0, 0);
            }
        }
        super.setText(text, type);
        if (text != null && !mIsInserting) {
            setSelection(getText().length());
        }
        invalidateTextSize();
        if (mTextWatchersEnabled) {
            for (TextWatcher textWatcher : mTextWatchers) {
                textWatcher.afterTextChanged(getEditableFactory().newEditable(getCleanText()));
                textWatcher.onTextChanged(getCleanText(), 0, 0, 0);
            }
        }
    }

    public String getCleanText() {
        return TextUtil.getCleanText(this, getSolver());
    }

    public void insert(String delta) {
        String currentText = getText().toString();
        int selectionHandle = getSelectionStart();
        String textBeforeInsertionHandle = currentText.substring(0, selectionHandle);
        String textAfterInsertionHandle = currentText.substring(selectionHandle, currentText.length());

        // Add extra rules for decimal points and operators
        if (delta.length() == 1) {
            char text = delta.charAt(0);

            // don't allow two dots in the same number
            if (text == Constants.DECIMAL_POINT) {
                int p = selectionHandle - 1;
                while (p >= 0 && Solver.isDigit(getText().charAt(p))) {
                    if (getText().charAt(p) == Constants.DECIMAL_POINT) {
                        return;
                    }
                    --p;
                }
                p = selectionHandle;
                while (p < getText().length() && Solver.isDigit(getText().charAt(p))) {
                    if (getText().charAt(p) == Constants.DECIMAL_POINT) {
                        return;
                    }
                    ++p;
                }
            }

            char prevChar = selectionHandle > 0 ? getText().charAt(selectionHandle - 1) : '\0';

            // don't allow 2 successive minuses
            if (text == Constants.MINUS && prevChar == Constants.MINUS) {
                return;
            }

            // don't allow the first character to be an operator
            if (selectionHandle == 0 && Solver.isOperator(text) && text != Constants.MINUS) {
                return;
            }

            // don't allow multiple successive operators
            if (Solver.isOperator(text) && text != Constants.MINUS) {
                while (Solver.isOperator(prevChar)) {
                    if (selectionHandle == 1) {
                        return;
                    }

                    --selectionHandle;
                    prevChar = selectionHandle > 0 ? getText().charAt(selectionHandle - 1) : '\0';
                    textBeforeInsertionHandle = textBeforeInsertionHandle.substring(0, selectionHandle);
                }
            }
        }

        mIsInserting = true;
        setText(textBeforeInsertionHandle + delta + BaseModule.SELECTION_HANDLE + textAfterInsertionHandle);
        mIsInserting = false;
    }

    public void clear() {
        setText(null);
    }

    public boolean isCursorModified() {
        return getSelectionStart() != getText().length();
    }

    public void next() {
        if (getSelectionStart() == getText().length()) {
            setSelection(0);
        } else {
            setSelection(getSelectionStart() + 1);
        }
    }

    @Override
    public void backspace() {
        // Check and remove keywords
        String text = getText().toString();
        int selectionHandle = getSelectionStart();
        String textBeforeInsertionHandle = text.substring(0, selectionHandle);
        String textAfterInsertionHandle = text.substring(selectionHandle, text.length());

        for (String s : mKeywords) {
            if (textBeforeInsertionHandle.endsWith(s)) {
                int deletionLength = s.length();
                String newText = textBeforeInsertionHandle.substring(0, textBeforeInsertionHandle.length() - deletionLength) + textAfterInsertionHandle;
                setText(newText);
                setSelection(selectionHandle - deletionLength);
                return;
            }
        }

        // Override NumberEditText's method -- because commas might disappear, it complicates things
        if (selectionHandle != 0) {
            setText(textBeforeInsertionHandle.substring(0, textBeforeInsertionHandle.length() - 1)
                    + textAfterInsertionHandle);

            if (getText().length() == text.length() - 2) {
                // 2 characters were deleted (likely a comma and a number)
                selectionHandle -= 2;
            } else {
                --selectionHandle;
            }

            setSelection(selectionHandle);
        }
    }

    @Override
    public void setSelection(int index) {
        super.setSelection(Math.max(0, Math.min(getText().length(), index)));
    }

    @Override
    public int getSelectionStart() {
        // When setting a movement method, selectionStart() suddenly defaults to -1 instead of 0.
        return Math.max(0, super.getSelectionStart());
    }

    protected String removeFormatting(String input) {
        input = input.replace(Constants.POWER_PLACEHOLDER, Constants.POWER);
        if (mSolver != null) {
            input = input.replace(String.valueOf(mSolver.getBaseModule().getSeparator()), "");
        }
        return input;
    }

    protected String formatText(String input, MutableInteger selectionHandle) {
        int customHandle = input.indexOf(BaseModule.SELECTION_HANDLE);
        if (customHandle >= 0) {
            selectionHandle.set(customHandle);
            input = input.replace(Character.toString(BaseModule.SELECTION_HANDLE), "");
        }

        if (mSolver != null) {
            // Add grouping, and then split on the selection handle
            // which is saved as a unique char
            String grouped = mEquationFormatter.addComas(mSolver, input, selectionHandle.intValue());
            if (grouped.contains(String.valueOf(BaseModule.SELECTION_HANDLE))) {
                String[] temp = grouped.split(String.valueOf(BaseModule.SELECTION_HANDLE));
                selectionHandle.set(temp[0].length());
                input = "";
                for (String s : temp) {
                    input += s;
                }
            } else {
                input = grouped;
            }
        }

        return mEquationFormatter.insertSupScripts(input);
    }

    public Solver getSolver() {
        return mSolver;
    }

    public void setSolver(Solver solver) {
        mSolver = solver;
    }

    public EquationFormatter getEquationFormatter() {
        return mEquationFormatter;
    }

    public void setDebugEnabled(boolean enabled) {
        mDebug = enabled;
    }

    public boolean isDebuggingEnabled() {
        return mDebug;
    }

    public class MutableInteger {

        private int value;

        public MutableInteger(int value) {
            this.value = value;
        }

        public MutableInteger(MutableInteger value) {
            this.value = value.intValue();
        }

        public void set(int value) {
            this.value = value;
        }

        public void add(int value) {
            this.value += value;
        }

        public void subtract(int value) {
            this.value -= value;
        }

        public int intValue() {
            return value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }
}
