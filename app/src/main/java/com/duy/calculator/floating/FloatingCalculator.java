package com.duy.calculator.floating;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewSwitcher;

import com.duy.calculator.CalculatorExpressionEvaluator;
import com.duy.calculator.CalculatorExpressionTokenizer;
import com.duy.calculator.Clipboard;
import com.duy.calculator.R;
import com.duy.calculator.view.BackspaceImageButton;
import com.duy.calculator.view.CalculatorEditText;
import com.duy.calculator.view.floating.FloatingView;
import com.duy.math.Constants;
import com.duy.math.EquationFormatter;
import com.duy.math.History;
import com.duy.math.HistoryEntry;
import com.duy.math.Persist;
import com.duy.math.Solver;

import static com.duy.calculator.CalculatorExpressionEvaluator.INVALID_RES_ID;

public class FloatingCalculator extends FloatingView implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "FloatingCalculator";
    // Calc logic
    private ViewSwitcher mDisplay;
    private BackspaceImageButton mDelete;
    private ViewPager mPager;
    private Persist mPersist;
    private History mHistory;
    private CalculatorExpressionTokenizer mTokenizer;
    private CalculatorExpressionEvaluator mEvaluator;
    private State mState;
    private FloatingPageAdapter mFloatingPageAdapter;

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @NonNull
    @Override
    protected Notification createNotification() {
        Intent intent = new Intent(this, FloatingCalculator.class).setAction(ACTION_OPEN);
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.floating_notification_description))
                .setContentIntent(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    @NonNull
    @Override
    public View inflateButton(@NonNull ViewGroup parent) {
        Log.d(TAG, "inflateButton() called with: parent = [" + parent + "]");
        return LayoutInflater.from(getContext()).inflate(R.layout.floating_calculator_icon, parent, false);
    }

    protected void setAccentColor() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int defaultColor = ContextCompat.getColor(getContext(), R.color.calculator_accent_color);
        int color = pref.getInt(ColorAdapter.PREF_KEY_COLOR_ACCENT, defaultColor);

        if (mInactiveButton != null) {
            FloatingActionButton fab = mInactiveButton.findViewById(R.id.fab);
            if (fab != null) fab.setBackgroundTintList(ColorStateList.valueOf(color));
        }
        if (mDraggableIcon != null) {
            FloatingActionButton fab = mDraggableIcon.findViewById(R.id.fab);
            if (fab != null) fab.setBackgroundTintList(ColorStateList.valueOf(color));
        }
        if (mFloatingPageAdapter != null) {
            View padAdvance = mFloatingPageAdapter.getViewAt(3);
            padAdvance.setBackgroundColor(color);
        }
    }

    @NonNull
    @Override
    public View inflateView(@NonNull ViewGroup parent) {
        // Rebuild constants. If the user changed their locale, it won't kill the app
        // but it might change a decimal point from . to ,
        Constants.rebuildConstants();

        final View child = LayoutInflater.from(getContext()).inflate(R.layout.floating_calculator, parent, false);

        mTokenizer = new CalculatorExpressionTokenizer(this);
        mEvaluator = new CalculatorExpressionEvaluator(mTokenizer);

        mPager = child.findViewById(R.id.panelswitch);

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.getHistory();

        mDisplay = child.findViewById(R.id.display);
        for (int i = 0; i < mDisplay.getChildCount(); i++) {
            final CalculatorEditText displayChild = (CalculatorEditText) mDisplay.getChildAt(i);
            displayChild.setSolver(mEvaluator.getSolver());
            displayChild.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    copyContent(displayChild.getCleanText());
                    return true;
                }
            });
            displayChild.setRawInputType(InputType.TYPE_CLASS_TEXT);
            displayChild.setTextIsSelectable(true);

        }

        mDelete = child.findViewById(R.id.delete);
        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.sw_radian:
                        break;
                    case R.id.delete:
                        if (mDelete.getState() == BackspaceImageButton.State.CLEAR) {
                            mDisplay.showNext();
                            onClear();
                        } else {
                            onDelete();
                        }
                        break;
                    case R.id.eq:
                        mEvaluator.evaluate(getActiveEditText().getCleanText(), new CalculatorExpressionEvaluator.EvaluateCallback() {
                            @Override
                            public void onEvaluate(String expr, String result, int errorResourceId) {
                                mDisplay.showNext();
                                if (errorResourceId != INVALID_RES_ID) {
                                    onError(errorResourceId);
                                } else {
                                    setText(result);
                                }
                                if (saveHistory(expr, result)) {
                                    RecyclerView history = child.findViewById(R.id.history);
                                    history.getLayoutManager().scrollToPosition(history.getAdapter().getItemCount() - 1);
                                }
                            }
                        });
                        break;
                    case R.id.parentheses:
                        setText("(" + getActiveEditText().getText() + ")");
                        break;
                    default:
                        if (((Button) v).getText().toString().length() >= 2) {
                            onInsert(((Button) v).getText().toString() + "(");
                        } else {
                            onInsert(((Button) v).getText().toString());
                        }
                        break;
                }
            }
        };
        mDelete.setOnClickListener(onClickListener);
        mDelete.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mDelete.getState() == BackspaceImageButton.State.DELETE) {
                    mDisplay.showNext();
                    onClear();
                    return true;
                }

                return false;
            }
        });
        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                switch (v.getId()) {
                    case R.id.btn_sin:
                    case R.id.btn_cos:
                    case R.id.btn_tan:
                        if (v.getTag().toString().length() >= 2) {
                            onInsert(v.getTag().toString() + "(");
                        } else {
                            onInsert(v.getTag().toString());
                        }
                        return true;
                }
                return false;
            }
        };
        FloatingHistoryAdapter.HistoryItemCallback historyItemCallback = new FloatingHistoryAdapter.HistoryItemCallback() {
            @Override
            public void onHistoryItemSelected(HistoryEntry entry) {
                setState(State.DELETE);
                getActiveEditText().insert(entry.getResult());
            }
        };
        mFloatingPageAdapter = new FloatingPageAdapter(
                getContext(), onClickListener, onLongClickListener,
                historyItemCallback, mEvaluator.getSolver(), mHistory);
        mPager.setAdapter(mFloatingPageAdapter);
        mPager.setCurrentItem(2);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int mActivePage = -1;

            @Override
            public void onPageScrolled(int i, float v, int i1) {
                // We're scrolling, so enable everything
                if (mActivePage != -1) {
                    mActivePage = -1;
                    setActivePage(mActivePage);
                }
            }

            @Override
            public void onPageSelected(int i) {
                // We've landed on a page, so disable all pages but this one
                mActivePage = i;
                setActivePage(mActivePage);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                // We've landed on a page (possibly the current page) so disable all pages but this one
                if (mActivePage == -1) {
                    mActivePage = mPager.getCurrentItem();
                    setActivePage(mActivePage);
                }
            }

            private void setActivePage(int page) {
                for (int i = 0; i < mFloatingPageAdapter.getCount(); i++) {
                    mFloatingPageAdapter.setEnabled(mFloatingPageAdapter.getViewAt(i), page == -1 || i == page);
                }
            }
        });

        child.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        setState(State.DELETE);

        return child;
    }

    @Override
    public void onShow() {
        if (mPager != null) {
            mPager.setCurrentItem(2);
        }

        if (mDisplay != null) {
            for (int i = 0; i < mDisplay.getChildCount(); i++) {
                final CalculatorEditText displayChild = (CalculatorEditText) mDisplay.getChildAt(i);
                displayChild.setSelection(displayChild.length());
            }
        }
        setAccentColor();
    }

    @Override
    public void onHide() {
        if (mPersist != null) {
            mPersist.save();
        }
    }

    private void onDelete() {
        setState(State.DELETE);
        getActiveEditText().backspace();
    }

    private void onClear() {
        setState(State.DELETE);
        getActiveEditText().clear();
    }

    private void setText(String text) {
        setState(State.CLEAR);
        getActiveEditText().setText(text);
    }

    private void onInsert(String text) {
        if (mState == State.ERROR || (mState == State.CLEAR && !Solver.isOperator(text))) {
            setText(text);
        } else {
            getActiveEditText().insert(text);
        }

        setState(State.DELETE);
    }

    private void onError(int resId) {
        setState(State.ERROR);
        getActiveEditText().setText(resId);
    }

    private void setState(State state) {
        mDelete.setState(state == State.DELETE ? BackspaceImageButton.State.DELETE : BackspaceImageButton.State.CLEAR);
        if (mState != state) {
            switch (state) {
                case CLEAR:
                    getActiveEditText().setTextColor(ContextCompat.getColor(this, R.color.display_formula_text_color));
                    break;
                case DELETE:
                    getActiveEditText().setTextColor(ContextCompat.getColor(this, R.color.display_formula_text_color));
                    break;
                case ERROR:
                    getActiveEditText().setTextColor(ContextCompat.getColor(this, R.color.calculator_error_color));
                    break;
            }
            mState = state;
        }
    }

    private void copyContent(String text) {
        Clipboard.copy(getContext(), text);
    }

    private CalculatorEditText getActiveEditText() {
        return (CalculatorEditText) mDisplay.getCurrentView();
    }

    protected boolean saveHistory(String expr, String result) {
        if (mHistory == null) {
            return false;
        }

        if (!TextUtils.isEmpty(expr)
                && !TextUtils.isEmpty(result)
                && !Solver.equal(expr, result)
                && (mHistory.current() == null || !mHistory.current().getFormula().equals(expr))) {
            expr = EquationFormatter.appendParenthesis(expr);
            expr = Solver.clean(expr);
            expr = mTokenizer.getLocalizedExpression(expr);
            mHistory.enter(expr, result);
            return true;
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(ColorAdapter.PREF_KEY_COLOR_ACCENT)) {
            setAccentColor();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    private enum State {
        DELETE, CLEAR, ERROR
    }
}
