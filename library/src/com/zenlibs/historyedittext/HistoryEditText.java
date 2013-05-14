/*
 * Copyright (C) 2013 Zenlibs
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

package com.zenlibs.historyedittext;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filter.FilterListener;
import android.widget.Filterable;
import android.widget.ListAdapter;
import com.commonsware.cwac.merge.MergeAdapter;

public class HistoryEditText extends AbsHistoryEditText {

    private boolean mFirstFiltering = true;
    private ListAdapter mHistoryAdapter;
    private Filter mHistoryFilter;
    private int mMaxHistoryValues;

    public HistoryEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttrs(context, attrs, defStyle);
    }

    private void parseAttrs(Context context, AttributeSet attrs, int defStyle) {

        // Getting max history values
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HistoryEditText, defStyle,
                R.style.Widget_HistoryEditText);

        mMaxHistoryValues = a.getInt(R.styleable.HistoryEditText_maxHistoryValues, 5);

        a.recycle();
    }

    public HistoryEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(context, attrs, android.R.attr.autoCompleteTextViewStyle);
    }

    public HistoryEditText(Context context) {
        super(context);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) {
            performFiltering(null, -1);
        }
    }

    @Override
    public void onEditorAction(int actionCode) {
        super.onEditorAction(actionCode);
        if (actionCode == getImeOptions()) {
            addCurrentTextToHistory();
            rebuildAdapter();
        }
    }

    private <T extends ListAdapter & Filterable> void setHistoryAdapter(T adapter) {
        mHistoryAdapter = adapter;
        if (adapter != null) {
            mHistoryFilter = adapter.getFilter();
        }
    }

    @Override
    protected void performFiltering(final CharSequence text, final int keyCode) {
        if (mFirstFiltering) {
            rebuildAdapter();
            mFirstFiltering = false;
        }
        if (text == null) {
            HistoryEditText.super.performFiltering(text, keyCode);
        } else if (mHistoryFilter != null) {
            mHistoryFilter.filter(text, new FilterListener() {
                @Override
                public void onFilterComplete(int count) {
                    HistoryEditText.super.performFiltering(text, keyCode);
                }
            });
        }
    }

    @Override
    protected ListAdapter getCombinedAdapter(ListAdapter userAdapter) {
        if (mHistoryAdapter == null) {
            return userAdapter;
        }
        MergeAdapter mergeAdapter = new MergeAdapter();
        mergeAdapter.addAdapter(mHistoryAdapter);
        mergeAdapter.addAdapter(userAdapter);
        return mergeAdapter;
    }

    private void rebuildAdapter() {
        SQLiteDatabase db = HistoryDb.getReadable(getContext());
        Cursor c = HistoryDb.queryByTag(db, (String) getTag());
        int count = Math.min(c.getCount(), mMaxHistoryValues);
        if (count == 0) {
            ArrayAdapter<String> adapter = null;
            setHistoryAdapter(adapter);
        } else {
            String[] items = new String[count];
            int i = 0;
            while (c.moveToNext() && i < count) {
                items[i++] = HistoryDb.getText(c);
            }
            c.close();
            db.close();
            int itemLayout = R.layout.het__dropdown_history_item;
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), itemLayout, items);
            setHistoryAdapter(adapter);
        }
    }

    private void addCurrentTextToHistory() {
        String text = getCurrentText();
        if (!TextUtils.isEmpty(text)) {
            addTextToHistory(text);
        }
    }

    private void addTextToHistory(String text) {
        SQLiteDatabase db = HistoryDb.getWritable(getContext());
        HistoryDb.insertEntry(db, (String) getTag(), text);
        db.close();
    }

    private String getCurrentText() {
        return getText().toString().trim();
    }
}