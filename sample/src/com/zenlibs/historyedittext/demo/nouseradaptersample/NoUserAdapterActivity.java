package com.zenlibs.historyedittext.demo.nouseradaptersample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.zenlibs.historyedittext.HistoryEditText;
import com.zenlibs.historyedittext.demo.R;

public class NoUserAdapterActivity extends SherlockActivity {

    private List<String> mCountries = new ArrayList<String>();
    private HistoryEditText mHistoryEditText;
    private HistoryEditText mHistoryEditText2;

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

        setContentView(R.layout.activity_no_user_adapter);
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
        mHistoryEditText2.clearHistory();
    }
}
