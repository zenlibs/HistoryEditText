package com.zenlibs.historyedittext.demo.simplehistorysample;

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

    private static final String[] COUNTRIES = new String[]{
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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.het__simple_dropdown_item_1line, COUNTRIES);
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
