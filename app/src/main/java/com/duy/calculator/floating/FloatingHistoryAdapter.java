/*
* Copyright (C) 2008 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.duy.calculator.floating;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.duy.calculator.R;
import com.duy.math.Constants;
import com.duy.math.EquationFormatter;
import com.duy.math.History;
import com.duy.math.HistoryEntry;
import com.duy.math.Solver;

import java.util.List;

class FloatingHistoryAdapter extends RecyclerView.Adapter<FloatingHistoryAdapter.ViewHolder> {
    private final Context mContext;
    private final Solver mSolver;
    private final List<HistoryEntry> mEntries;
    private final EquationFormatter mEquationFormatter;
    protected HistoryItemCallback mCallback;

    public FloatingHistoryAdapter(Context context, Solver solver, History history, HistoryItemCallback callback) {
        mContext = context;
        mSolver = solver;
        mEntries = history.getEntries();
        mEquationFormatter = new EquationFormatter();
        mCallback = callback;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(getContext()).inflate(getLayoutResourceId(), parent, false);
        return new ViewHolder(view);
    }

    protected int getLayoutResourceId() {
        return R.layout.list_item_history;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final HistoryEntry entry = getEntry(position);
        holder.historyExpr.setText(formatText(entry.getFormula()));
        holder.historyResult.setText(formatText(entry.getResult()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onHistoryItemSelected(entry);
            }
        });
    }

    private HistoryEntry getEntry(int position) {
        if (position < 0 || position >= mEntries.size()) {
            return null;
        }

        return mEntries.get(position);
    }

    @Override
    public int getItemCount() {
        return mEntries.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    protected Spanned formatText(String text) {
        if (text == null) {
            return null;
        }

        if (text.matches(".*\\de[-" + Constants.MINUS + "]?\\d.*")) {
            text = text.replace("e", Constants.MUL + "10^");
        }
        return Html.fromHtml(
                mEquationFormatter.insertSupScripts(
                        mEquationFormatter.addComas(mSolver, text)));
    }

    public Context getContext() {
        return mContext;
    }

    public interface HistoryItemCallback {
        void onHistoryItemSelected(HistoryEntry entry);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView historyExpr;
        public TextView historyResult;

        public ViewHolder(View v) {
            super(v);
            historyExpr = (TextView) v.findViewById(R.id.historyExpr);
            historyResult = (TextView) v.findViewById(R.id.historyResult);
        }
    }
}
