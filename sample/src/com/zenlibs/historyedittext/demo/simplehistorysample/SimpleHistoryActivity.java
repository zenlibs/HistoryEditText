package com.zenlibs.historyedittext.demo.simplehistorysample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.zenlibs.historyedittext.HistoryEditText;
import com.zenlibs.historyedittext.demo.R;

public class SimpleHistoryActivity extends SherlockActivity {

    private List<String> mCountries = new ArrayList<String>();
    private HistoryEditText mHistoryEditText;
    private HistoryEditText mHistoryEditText2;
    private AutoCompleteTextView autoCompleteTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            InputStream inputStream = getResources().openRawResource(R.raw.countries);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                mCountries.add(line);
            }
            reader.close();
        } catch (IOException e) {
        }

        setContentView(R.layout.activity_simple);

        mHistoryEditText = (HistoryEditText) findViewById(R.id.historyEditText);
        mHistoryEditText.setAdapter(createAdapter());
        mHistoryEditText2 = (HistoryEditText) findViewById(R.id.historyEditText2);
        mHistoryEditText2.setAdapter(createAdapter());
        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        autoCompleteTextView.setAdapter(createAdapter());
        autoCompleteTextView.setThreshold(1);
        autoCompleteTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    autoCompleteTextView.showDropDown();
                } else {
                    autoCompleteTextView.dismissDropDown();
                }
            }
        });
    }

    private ArrayAdapter<String> createAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.het__simple_dropdown_item_1line,
                mCountries);
        return adapter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(this).inflate(R.menu.activity_simple, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_history) {
            clearHistory();
            return true;
        }
        return false;
    }

    private void clearHistory() {
        mHistoryEditText.clearHistory();
    }
}
