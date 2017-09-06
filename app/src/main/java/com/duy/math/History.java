/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.duy.math;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class History {
    private static final int VERSION_1 = 1;
    private static final int VERSION_4 = 4;
    private static final int MAX_ENTRIES = 100;
    private List<HistoryEntry> mEntries = new LinkedList<HistoryEntry>();
    private int mPos;
    private int mGroupId;
    private Observer mObserver;

    History() {
        clear();
    }

    History(int version, DataInput in) throws IOException {
        if (version >= VERSION_1) {
            int size = in.readInt();
            for (int i = 0; i < size; ++i) {
                mEntries.add(new HistoryEntry(version, in));
            }
            mPos = in.readInt();
        }
        if (version >= VERSION_4) {
            mGroupId = in.readInt();
        }
    }

    public void clear() {
        mEntries.clear();
        mPos = -1;
        mGroupId = 0;
        notifyChanged();
    }

    private void notifyChanged() {
        if (mObserver != null) {
            mObserver.notifyDataSetChanged();
        }
    }

    public void setObserver(Observer observer) {
        mObserver = observer;
    }

    void write(DataOutput out) throws IOException {
        out.writeInt(mEntries.size());
        for (HistoryEntry entry : mEntries) {
            entry.write(out);
        }
        out.writeInt(mPos);
        out.writeInt(mGroupId);
    }

    public HistoryEntry current() {
        if (mPos >= mEntries.size()) {
            mPos = mEntries.size() - 1;
        }
        if (mPos == -1) {
            return null;
        }
        return mEntries.get(mPos);
    }

    public void enter(String formula, String result) {
        if (mEntries.size() >= MAX_ENTRIES) {
            mEntries.remove(0);
        }
        mEntries.add(new HistoryEntry(formula, result, mGroupId));
        mPos = mEntries.size() - 1;
        notifyChanged();
    }

    public void incrementGroupId() {
        ++mGroupId;
    }

    public String getText() {
        return current().getResult();
    }

    public String getBase() {
        return current().getFormula();
    }

    public void remove(HistoryEntry he) {
        mEntries.remove(he);
        mPos--;
    }

    public List<HistoryEntry> getEntries() {
        return mEntries;
    }

    public interface Observer {
        void notifyDataSetChanged();
    }
}
