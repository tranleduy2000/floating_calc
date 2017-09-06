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

public class HistoryEntry {
    private static final int VERSION_1 = 1;
    private static final int VERSION_4 = 4;
    private String mFormula;
    private String mResult;
    private int mGroupId;

    public HistoryEntry(String formula, String result, int groupId) {
        mFormula = formula;
        mResult = result;
        mGroupId = groupId;
    }

    HistoryEntry(int version, DataInput in) throws IOException {
        if (version >= VERSION_1) {
            mFormula = in.readUTF();
            mResult = in.readUTF();
        }
        if (version >= VERSION_4) {
            mGroupId = in.readInt();
        }
    }

    void write(DataOutput out) throws IOException {
        out.writeUTF(mFormula);
        out.writeUTF(mResult);
        out.writeInt(mGroupId);
    }

    @Override
    public String toString() {
        return mFormula;
    }

    public String getResult() {
        return mResult;
    }

    void setResult(String result) {
        mResult = result;
    }

    public String getFormula() {
        return mFormula;
    }

    public int getGroupId() {
        return mGroupId;
    }

    void setGroupId(int groupId) {
        mGroupId = groupId;
    }
}
