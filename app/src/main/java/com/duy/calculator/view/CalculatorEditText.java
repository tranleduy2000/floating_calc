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
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.CharacterStyle;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.duy.calculator.util.TextUtil;
import com.xlythe.math.BaseModule;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class CalculatorEditText extends FormattedNumberEditText {
    private static final String TAG = CalculatorEditText.class.getSimpleName();
    private static final boolean DEBUG = false;

    // Look for special text (like matrices) that we want to format differently
    private final Set<SpanComponent> mComponents = new HashSet<>();
    private final Set<CharacterStyle> mSpans = new HashSet<>();

    public CalculatorEditText(Context context) {
        super(context);
        setUp(context, null);
    }

    public CalculatorEditText(Context context, AttributeSet attr) {
        super(context, attr);
        setUp(context, attr);
    }

    private void setUp(Context context, AttributeSet attrs) {
        setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/RobotoMono-Light.ttf"));
    }

    @Override
    protected void onSelectionChanged(int handle, int selEnd) {
        super.onSelectionChanged(handle, selEnd);
        Editable text = getText();
        MathSpannable[] spans = text.getSpans(0, text.length(), MathSpannable.class);
        for (MathSpannable span : spans) {
            log("onSelectionChanged " + handle + " " + selEnd);
            int start = text.getSpanStart(span);
            int end = text.getSpanEnd(span);
            if (handle > start && handle < end) {
                log("notifying span(" + span.getEquation() + ") that its cursor is " + (handle - start));
                span.setCursor(handle - start);
            } else {
                log("removing span(" + span.getEquation() + ")'s cursor");
                span.setCursor(-1);
            }
        }
    }

    protected void onFormat(Editable s) {
        log("onFormat");

        // Grab the text, as well as the selection handle
        String editable = s.toString();
        MutableInteger selectionHandle = new MutableInteger(getSelectionStart());

        // Make adjustments (insert will append a SELECTION_HANDLE marker)
        int customHandle = editable.indexOf(BaseModule.SELECTION_HANDLE);
        if (customHandle >= 0) {
            selectionHandle.set(customHandle);
            editable = editable.replace(Character.toString(BaseModule.SELECTION_HANDLE), "");
        }

        // Update the text with the correct (no SELECTION_HANDLE) copy
        setText(editable);
        log("invalidateSpannables a");
        invalidateSpannables();
        setSelection(selectionHandle.intValue());
        s = getText();

        // We don't want to format anything that's controlled by MathSpannables (like matrices).
        // So grab all the spans in our EditText
        MathSpannable[] spans = s.getSpans(0, s.length(), MathSpannable.class);
        final Editable s2 = s;
        Arrays.sort(spans, new Comparator<MathSpannable>() {
            @Override
            public int compare(MathSpannable a, MathSpannable b) {
                return s2.getSpanStart(a) - s2.getSpanStart(b);
            }
        });

        // Ah, no spans. Nothing to think about, so easy.
        if (spans.length == 0) {
            super.onFormat(s);
            return;
        }

        // Start formatting, but skip the parts that involve spans
        StringBuilder builder = new StringBuilder();

        log("Parsing " + editable);
        for (int i = 0; i < spans.length + 1; i++) {
            int start = i == 0 ? 0 : s.getSpanEnd(spans[i - 1]);
            int end = i == spans.length ? s.length() : s.getSpanStart(spans[i]);
            log("I'm looking at the range " + start + " to " + end);

            String text = editable.substring(start, end);
            log("I grabbed " + text);
            log("My selection handle is " + selectionHandle);
            boolean chunkBeforeSelectionHandle = end <= selectionHandle.intValue();
            boolean selectionHandleInChunk = selectionHandle.intValue() >= start && selectionHandle.intValue() < end;
            if (chunkBeforeSelectionHandle || selectionHandleInChunk) {
                int length = Math.min(text.length(), selectionHandle.intValue() - start);
                // Special case -- keep track of the selection handle
                String cs = text.substring(0, length);
                selectionHandle.subtract(TextUtil.countOccurrences(cs, getSolver().getBaseModule().getSeparator()));
                text = formatText(removeFormatting(text), selectionHandle);
            } else {
                text = formatText(removeFormatting(text), selectionHandle);
            }
            log("I formatted it to look like " + text);
            builder.append(text);
            if (i < spans.length) {
                builder.append(spans[i].getEquation());
                log("Adding my span too: " + spans[i].getEquation());
            }
        }
        log("My end result is: " + builder.toString());

        // Update the text with formatted (comas, etc) text
        setText(Html.fromHtml(builder.toString()));
        log("invalidateSpannables b");
        invalidateSpannables();
        setSelection(selectionHandle.intValue());
    }

    @Override
    protected String removeFormatting(String input) {
        StringBuilder cleanText = new StringBuilder();
        StringBuilder cache = new StringBuilder();

        loop:
        for (int i = 0; i < input.length(); i++) {
            for (SpanComponent component : mComponents) {
                String equation = component.parse(input.substring(i));
                if (equation != null) {
                    // Apply super.removeFormatting on the cache (the part we didn't really care about)
                    cleanText.append(super.removeFormatting(cache.toString()));
                    cache = new StringBuilder();

                    // Leave the parsed equation as-is (TODO: clean this too? via component?)
                    cleanText.append(equation);
                    i += equation.length();

                    // Go to the next character
                    continue loop;
                }
            }
            cache.append(input.charAt(i));
        }
        cleanText.append(super.removeFormatting(cache.toString()));
        return cleanText.toString();
    }

    public void addSpanComponent(SpanComponent component) {
        mComponents.add(component);
        log("invalidateSpannables c");
        invalidateSpannables();
    }

    public void invalidateSpannables() {
        log("invalidating all spannables -- consider everything nullified");
        final Spannable spans = getText();
        final String text = spans.toString();

        // Remove existing spans
        for (CharacterStyle style : mSpans) {
            spans.removeSpan(style);
        }

        // Loop over the text, looking for new spans
        for (int i = 0; i < text.length(); i++) {
            for (SpanComponent component : mComponents) {
                String equation = component.parse(text.substring(i));
                if (equation != null) {
                    MathSpannable span = component.getSpan(equation);
                    spans.setSpan(span, i, i + equation.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    i += equation.length();
                    break;
                }
            }
        }

        setSelection(getSelectionStart());
    }

    @Override
    public void next() {
        log("Told to go next");
        final int selectionHandle = getSelectionStart();
        final Editable editable = getText();
        if (selectionHandle > 0) {
            log("Handle is " + selectionHandle);
            MathSpannable[] spans = editable.getSpans(selectionHandle, selectionHandle, MathSpannable.class);
            if (spans.length != 0) {
                // There's a spannable at our cursor position, but we don't know if it's before or after the position.
                if (selectionHandle != editable.getSpanEnd(spans[0])) {
                    int next = getSelectionStart() + spans[0].next();
                    log("setSelection " + next);
                    setSelection(next);
                    return;
                }
            }
        }

        super.next();
    }

    @Override
    public void backspace() {
        final int selectionHandle = getSelectionStart();
        final Editable editable = getText();
        if (selectionHandle > 0) {
            MathSpannable[] spans = editable.getSpans(selectionHandle, selectionHandle, MathSpannable.class);
            if (spans.length != 0) {
                String text = editable.toString();
                String textBeforeInsertionHandle = text.substring(0, selectionHandle);
                String textAfterInsertionHandle = text.substring(selectionHandle, text.length());

                int deletionLength = -1;
                if (selectionHandle == editable.getSpanEnd(spans[0]) && spans[0].removeOnBackspace()) {
                    deletionLength = spans[0].getEquation().length();
                } else if (selectionHandle != editable.getSpanStart(spans[0])) {
                    deletionLength = spans[0].backspace();
                }

                if (deletionLength != -1) {
                    String newText = textBeforeInsertionHandle.substring(0, textBeforeInsertionHandle.length() - deletionLength) + textAfterInsertionHandle;
                    setText(newText);
                    setSelection(selectionHandle - deletionLength);

                    return;
                }
            }
        }

        super.backspace();
    }

    private void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }

    public static abstract class SpanComponent {
        public abstract String parse(String formula);

        public abstract MathSpannable getSpan(String equation);
    }

    /**
     * A span that represents a mathematical expression (eg. a matrix) that can't be easily
     * expressed as just text
     */
    public static abstract class MathSpannable extends ReplacementSpan {
        private String mEquation;
        private int mCursor = -1;

        public MathSpannable(String equation) {
            mEquation = equation;
        }

        public String getEquation() {
            return mEquation;
        }

        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }

        public boolean removeOnBackspace() {
            return false;
        }

        public int backspace() {
            return 1;
        }

        public int getCursor() {
            return mCursor;
        }

        public void setCursor(int cursor) {
            mCursor = cursor;
        }

        public int next() {
            return 1;
        }
    }

    /**
     * Looks for MathSpannables and passes onTouch events to them
     */
    public static class MathMovementMethod extends LinkMovementMethod {
        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            MathSpannable[] spans = buffer.getSpans(off, off, MathSpannable.class);

            if (spans.length != 0) {
                return spans[0].onTouchEvent(event);
            }

            return false;
        }
    }
}
