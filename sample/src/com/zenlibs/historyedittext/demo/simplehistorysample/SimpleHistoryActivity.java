package com.zenlibs.historyedittext.demo.simplehistorysample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import com.zenlibs.historyedittext.HistoryEditText;
import com.zenlibs.historyedittext.demo.R;

public class SimpleHistoryActivity extends Activity {

    private static final String[] COUNTRIES = new String[] {
            "Belgium", "France", "Italy", "Germany", "Spain"
    };

    private HistoryEditText mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_simple);

        mText = (HistoryEditText) findViewById(R.id.editText);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, COUNTRIES);

        mText.setAdapter(adapter);
    }
}
