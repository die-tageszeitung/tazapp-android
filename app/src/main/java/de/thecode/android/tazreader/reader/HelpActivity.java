package de.thecode.android.tazreader.reader;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.utils.BaseActivity;

/**
 * Created by mate on 11.05.2015.
 */
public class HelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
