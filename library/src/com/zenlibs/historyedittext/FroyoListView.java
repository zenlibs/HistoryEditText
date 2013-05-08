package com.zenlibs.historyedittext;

import java.lang.reflect.Method;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListAdapter;
import android.widget.ListView;

public class FroyoListView extends ListView {

    private boolean mAreAllItemsSelectable;
    private Method mHideSelectorMethod;

    public FroyoListView(Context context) {
        super(context);
        init();
    }

    public FroyoListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FroyoListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        try {
            mHideSelectorMethod = getClass().getMethod("hideSelector");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        if (adapter != null) {
            mAreAllItemsSelectable = adapter.areAllItemsEnabled();
        } else {
            mAreAllItemsSelectable = true;
        }
    }

    int lookForSelectablePosition(int position, boolean lookDown) {
        final ListAdapter adapter = getAdapter();
        if (adapter == null || isInTouchMode()) {
            return INVALID_POSITION;
        }

        final int count = adapter.getCount();
        if (!mAreAllItemsSelectable) {
            if (lookDown) {
                position = Math.max(0, position);
                while (position < count && !adapter.isEnabled(position)) {
                    position++;
                }
            } else {
                position = Math.min(position, count - 1);
                while (position >= 0 && !adapter.isEnabled(position)) {
                    position--;
                }
            }

            if (position < 0 || position >= count) {
                return INVALID_POSITION;
            }
            return position;
        } else {
            if (position < 0 || position >= count) {
                return INVALID_POSITION;
            }
            return position;
        }
    }

    void hideSelectorCompat() {
        try {
            mHideSelectorMethod.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
