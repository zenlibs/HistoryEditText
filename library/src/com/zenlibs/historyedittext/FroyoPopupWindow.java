package com.zenlibs.historyedittext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.PopupWindow;

public class FroyoPopupWindow extends PopupWindow {

    
    
    private Method mGetMaxAvailableHeightMethod;

    public FroyoPopupWindow() {
        super();
        init();
    }

    public FroyoPopupWindow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public FroyoPopupWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FroyoPopupWindow(Context context) {
        super(context);
        init();
    }

    public FroyoPopupWindow(int width, int height) {
        super(width, height);
        init();
    }

    public FroyoPopupWindow(View contentView, int width, int height, boolean focusable) {
        super(contentView, width, height, focusable);
        init();
    }

    public FroyoPopupWindow(View contentView, int width, int height) {
        super(contentView, width, height);
        init();
    }

    public FroyoPopupWindow(View contentView) {
        super(contentView);
        init();
    }

    private void init() {
        try {
            mGetMaxAvailableHeightMethod = getClass().getMethod("getMaxAvailableHeight", View.class, int.class, boolean.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
        
    public int getMaxAvailableHeightCompat(View anchor, int yOffset, boolean ignoreBottomDecorations) {
        try {
            return (Integer) mGetMaxAvailableHeightMethod.invoke(this, anchor, yOffset, ignoreBottomDecorations);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
