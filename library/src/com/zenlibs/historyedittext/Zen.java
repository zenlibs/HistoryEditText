package com.zenlibs.historyedittext;

import android.content.Context;
import android.database.Cursor;
import android.widget.Toast;

public class Zen {
    public static void longToast(Context ctx, String fmt, Object ... args) {
        Toast.makeText(ctx, String.format(fmt, args), Toast.LENGTH_LONG).show();
    }

    public static void longToast(Context ctx, int resId, Object ... args) {
        longToast(ctx, ctx.getString(resId), args);
    }

    public static void shortToast(Context ctx, String fmt, Object ... args) {
        Toast.makeText(ctx, String.format(fmt, args), Toast.LENGTH_SHORT).show();
    }

    public static void shortToast(Context ctx, int resId, Object ... args) {
        shortToast(ctx, ctx.getString(resId), args);
    }

    public static String getTextColumn(Cursor c, String columnName) {
        int index = c.getColumnIndex(columnName);
        return c.getString(index);
    }
}
