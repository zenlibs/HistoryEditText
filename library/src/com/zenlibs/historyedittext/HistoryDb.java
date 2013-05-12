package com.zenlibs.historyedittext;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class HistoryDb {
    private static final String HISTORY_TABLE = "history";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TAG = "tag";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_TIME = "time";
    private static final String[] QUERY_BY_TAG_COLUMNS = new String[] { COLUMN_TEXT };
    private static final String QUERY_BY_TAG_SELECTION = COLUMN_TAG + "=?";
    private static final String QUERY_BY_TAG_ORDER = COLUMN_TIME + " DESC";
    private static HistoryDbSQLiteHelper mHelper;

    public static synchronized void insertEntry(SQLiteDatabase db, String tag, String text) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TAG, tag);
        values.put(COLUMN_TEXT, text);
        values.put(COLUMN_TIME, System.currentTimeMillis());
        db.insert(HISTORY_TABLE, null, values);
    }
    
    public static Cursor queryByTag(SQLiteDatabase db, String tag) {
        return db.query(HISTORY_TABLE, QUERY_BY_TAG_COLUMNS, QUERY_BY_TAG_SELECTION, new String[] { tag }, null, null,
                QUERY_BY_TAG_ORDER);
    }

    static SQLiteDatabase getReadable(Context context) {
        HistoryDbSQLiteHelper helper = getHelper(context);
        return helper.getReadableDatabase();
    }

    static SQLiteDatabase getWritable(Context context) {
        HistoryDbSQLiteHelper helper = getHelper(context);
        return helper.getWritableDatabase();
    }

    private static HistoryDbSQLiteHelper getHelper(Context context) {
        if (mHelper == null) {
            mHelper = new HistoryDbSQLiteHelper(context);
        }
        return mHelper;
    }

    static class HistoryDbSQLiteHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "historyedittext.db";
        private static final int DATABASE_VERSION = 1;

        public HistoryDbSQLiteHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + HISTORY_TABLE + "(" + COLUMN_ID + " integer primary key autoincrement, "
                    + COLUMN_TAG + " text not null, " + COLUMN_TEXT + " text not null, " + COLUMN_TIME + " long);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("drop table if exists " + HISTORY_TABLE);
            onCreate(db);
        }
    }

    public static String getText(Cursor c) {
        return Zen.getTextColumn(c, COLUMN_TEXT);
    }
}
