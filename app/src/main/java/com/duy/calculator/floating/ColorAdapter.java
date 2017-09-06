package com.duy.calculator.floating;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.duy.calculator.R;

import static com.duy.calculator.CalculatorSettings.PREF_KEY_COLOR_ACCENT;

/**
 * Created by Duy on 9/5/2017.
 */

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> {
    private int[] mColors;
    private LayoutInflater mInflater;
    private SharedPreferences mSharedPreferences;

    public ColorAdapter(Context context) {
        this.mColors = context.getResources().getIntArray(R.array.calculator_accent_color);
        this.mInflater = LayoutInflater.from(context);
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public ColorAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item_color, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ColorAdapter.ViewHolder holder, int position) {
        final int color = mColors[position];
        holder.mCardView.setCardBackgroundColor(color);
        holder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSharedPreferences.edit().putInt(PREF_KEY_COLOR_ACCENT, color).apply();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mColors.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private CardView mCardView;

        public ViewHolder(View itemView) {
            super(itemView);
            mCardView = itemView.findViewById(R.id.card_view);
        }
    }
}
