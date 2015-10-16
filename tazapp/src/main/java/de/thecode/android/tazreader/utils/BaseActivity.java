package de.thecode.android.tazreader.utils;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import de.thecode.android.tazreader.BuildConfig;

/**
 * Created by mate on 12.05.2015.
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager.enableDebugLogging(BuildConfig.DEBUG);
    }
}
