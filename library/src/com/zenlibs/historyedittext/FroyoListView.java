package com.zenlibs.historyedittext;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FroyoListView extends ListView {
    /**
     * Used to indicate a no preference for a position type.
     */
    public static final int NO_POSITION = -1;
    private boolean mAreAllItemsSelectable;
    private Method mHideSelectorMethod;
    private Object mRecycler;
    private Method mGetScrapViewMethod;
    private Method mAddScrapViewMethod;
    private Method mDispatchFinishTemporaryDispatchMethod;
    private Method mMeasureHeightOfChildren;

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
            // This is dangerous, whenever a new version of Android is released we should
            // check that this method still exists and that it hasn't
            mHideSelectorMethod = getClass().getMethod("hideSelector");
            mDispatchFinishTemporaryDispatchMethod = View.class.getMethod("dispatchFinishTemporaryDetach");
            mMeasureHeightOfChildren = getClass().getMethod("measureHeightOfChildren", int.class, int.class, int.class, int.class, int.class);

            Field recyclerField = getClass().getField("mRecycler");
            mRecycler = recyclerField.get(this);
            Class<?>[] classes = getClass().getClasses();
            for (Class<?> class_ : classes) {
                if (class_.getSimpleName().equals("RecycleBin")) {
                    Class<?> recycleBinClass = class_;
                    mGetScrapViewMethod = recycleBinClass.getMethod("getScrapView", int.class);
                    mAddScrapViewMethod = recycleBinClass.getMethod("addScrapView", View.class);
                    break;
                }
            }
        } catch (Exception e) {
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

    int lookForSelectablePositionCompat(int position, boolean lookDown) {
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
    
    /**
     * Get a view and have it show the data associated with the specified
     * position. This is called when we have already discovered that the view is
     * not available for reuse in the recycle bin. The only choices left are
     * converting an old view or making a new one.
     *
     * @param position The position to display
     * @param isScrap Array of at least 1 boolean, the first entry will become true if
     *                the returned view was taken from the scrap heap, false if otherwise.
     * 
     * @return A view displaying the data associated with the specified position
     */
    View obtainView(int position, boolean[] isScrap) {
        try {  
            isScrap[0] = false;
            View scrapView;
            scrapView = (View) mGetScrapViewMethod.invoke(mRecycler, position);
    
            View child;
            ListAdapter adapter = getAdapter();
            int cacheColorHint = getCacheColorHint();
            if (scrapView != null) {
                if (ViewDebug.TRACE_RECYCLER) {
                    ViewDebug.trace(scrapView, ViewDebug.RecyclerTraceType.RECYCLE_FROM_SCRAP_HEAP,
                            position, -1);
                }
    
                child = adapter.getView(position, scrapView, this);
    
                if (ViewDebug.TRACE_RECYCLER) {
                    ViewDebug.trace(child, ViewDebug.RecyclerTraceType.BIND_VIEW,
                            position, getChildCount());
                }
                if (child != scrapView) {
                    mAddScrapViewMethod.invoke(mRecycler, scrapView);
                    if (cacheColorHint != 0) {
                        child.setDrawingCacheBackgroundColor(cacheColorHint);
                    }
                    if (ViewDebug.TRACE_RECYCLER) {
                        ViewDebug.trace(scrapView, ViewDebug.RecyclerTraceType.MOVE_TO_SCRAP_HEAP,
                                position, -1);
                    }
                } else {
                    isScrap[0] = true;
                    mDispatchFinishTemporaryDispatchMethod.invoke(child);
                }
            } else {
                child = adapter.getView(position, null, this);
                if (cacheColorHint != 0) {
                    child.setDrawingCacheBackgroundColor(cacheColorHint);
                }
                if (ViewDebug.TRACE_RECYCLER) {
                    ViewDebug.trace(child, ViewDebug.RecyclerTraceType.NEW_VIEW,
                            position, getChildCount());
                }
            }
            return child;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public final int measureHeightOfChildrenCompat(int widthMeasureSpec, int startPosition, int endPosition,
                                                   final int maxHeight, int disallowPartialChildPosition) {
        try {
            return (Integer) mMeasureHeightOfChildren.invoke(this, widthMeasureSpec, startPosition, endPosition,
                maxHeight, disallowPartialChildPosition);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
