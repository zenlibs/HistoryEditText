package com.zenlibs.historyedittext.demo.simplehistorysample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.zenlibs.historyedittext.HistoryEditText;
import com.zenlibs.historyedittext.demo.R;

public class SimpleHistoryActivity extends Activity {

    private static final String[] COUNTRIES = new String[] {
            "Belgium", "France", "Italy", "Germany", "Spain"
    };

    private HistoryEditText mHistoryEditText;
    private AutoCompleteTextView autoCompleteTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_simple);

        mHistoryEditText = (HistoryEditText) findViewById(R.id.historyEditText);
        mHistoryEditText.setAdapter(createAdapter());
        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        autoCompleteTextView.setAdapter(createAdapter());
    }

    private ArrayAdapter<String> createAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, COUNTRIES);
        return adapter;
    }
}
