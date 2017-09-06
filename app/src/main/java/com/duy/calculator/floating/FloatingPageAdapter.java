package com.duy.calculator.floating;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;

import com.duy.calculator.CalculatorSettings;
import com.duy.calculator.R;
import com.duy.calculator.view.SolidLayout;
import com.duy.calculator.view.SolidPadLayout;
import com.duy.math.Constants;
import com.duy.math.History;
import com.duy.math.Solver;

public class FloatingPageAdapter extends PagerAdapter {
    private static final int COUNT = 4;
    private final Context mContext;
    private final View.OnClickListener mOnClickListener;
    private final FloatingHistoryAdapter.HistoryItemCallback mHistoryCallback;
    private final Solver mSolver;
    private final History mHistory;
    private final View[] mViews = new View[4];
    private View.OnLongClickListener mOnLongClickListener;

    public FloatingPageAdapter(
            Context context,
            View.OnClickListener listener, View.OnLongClickListener onLongClickListener,
            FloatingHistoryAdapter.HistoryItemCallback historyCallback,
            Solver solver,
            History history) {
        mContext = new ContextThemeWrapper(context, R.style.Theme);
        mOnClickListener = listener;
        mOnLongClickListener = onLongClickListener;
        mHistoryCallback = historyCallback;
        mSolver = solver;
        mHistory = history;
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return COUNT;
    }

    @Override
    public void startUpdate(ViewGroup container) {
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View v = getViewAt(position);
        container.addView(v);

        return v;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (mViews[position] != null) mViews[position] = null;
        container.removeView((View) object);
    }

    @Override
    public void finishUpdate(ViewGroup container) {
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    }

    public View getViewAt(int position) {
        if (mViews[position] != null) return mViews[position];
        switch (position) {
            case 0:
                mViews[position] = View.inflate(mContext, R.layout.floating_pref, null);
                setUpPref(mViews[position]);
                break;
            case 1:
                mViews[position] = View.inflate(mContext, R.layout.floating_calculator_history, null);
                RecyclerView historyView = mViews[position].findViewById(R.id.history);
                setUpHistory(historyView);

                // This is the first time loading the history panel -- disable it until the user moves to it
                setEnabled(mViews[position], false);
                break;
            case 2:
                mViews[position] = View.inflate(mContext, R.layout.floating_calculator_basic, null);

                Button dot = mViews[position].findViewById(R.id.dec_point);
                dot.setText(String.valueOf(Constants.DECIMAL_POINT));

                break;
            case 3:
                mViews[position] = View.inflate(mContext, R.layout.floating_calculator_advanced, null);

                // This is the first time loading the advanced panel -- disable it until the user moves to it
                setEnabled(mViews[position], false);
                break;

        }
        applyListener(mViews[position]);
        return mViews[position];
    }

    private void setUpPref(View v) {
        RecyclerView recyclerView = v.findViewById(R.id.recycle_view_color);
        recyclerView.setLayoutManager(new GridLayoutManager(mContext, 3));
        recyclerView.setAdapter(new ColorAdapter(mContext));
        Switch mRadian = v.findViewById(R.id.sw_radian);
        mRadian.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                CalculatorSettings.setRadiansEnabled(mContext, b);
            }
        });
        mRadian.setChecked(CalculatorSettings.useRadians(mContext));
    }

    private void applyListener(View view) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                applyListener(((ViewGroup) view).getChildAt(i));
            }
        } else if (view instanceof Button) {
            view.setOnClickListener(mOnClickListener);
            view.setOnLongClickListener(mOnLongClickListener);
        } else if (view instanceof ImageButton) {
            view.setOnClickListener(mOnClickListener);
            view.setOnLongClickListener(mOnLongClickListener);
        }
    }

    protected void setEnabled(View view, boolean enabled) {
        if (view instanceof SolidLayout) {
            ((SolidLayout) view).setPreventChildTouchEvents(!enabled);
        } else if (view instanceof SolidPadLayout) {
            ((SolidPadLayout) view).setPreventChildTouchEvents(!enabled);
        } else if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setEnabled(((ViewGroup) view).getChildAt(i), enabled);
            }
        }
    }

    private void setUpHistory(RecyclerView historyView) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        historyView.setLayoutManager(layoutManager);
        historyView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));

        final FloatingHistoryAdapter historyAdapter = new FloatingHistoryAdapter(mContext, mSolver, mHistory, mHistoryCallback);
        mHistory.setObserver(new History.Observer() {
            @Override
            public void notifyDataSetChanged() {
                historyAdapter.notifyDataSetChanged();
            }
        });
        historyView.setAdapter(historyAdapter);

        layoutManager.scrollToPosition(historyAdapter.getItemCount() - 1);
    }
}
