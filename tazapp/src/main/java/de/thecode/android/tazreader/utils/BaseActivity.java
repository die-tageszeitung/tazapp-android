package de.thecode.android.tazreader.utils;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.dialog.PushNotificationDialog;
import de.thecode.android.tazreader.push.PushHelper;
import de.thecode.android.tazreader.push.PushNotification;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import timber.log.Timber;

/**
 * Created by mate on 12.05.2015.
 */
public class BaseActivity extends AppCompatActivity {

    private static final String DIALOG_PUSH = "DialogPush";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager.enableDebugLogging(BuildConfig.DEBUG);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault()
                .register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault()
                .unregister(this);
        super.onStop();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PushHelper.checkIntentForFCMPushNotificationExtras(getIntent());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPushNotification(PushNotification event) {
        Timber.d("received PushNotification Event");
        new PushNotificationDialog.Builder().setPositiveButton()
                                            .setPushNotification(event)
                                            .buildSupport()
                                            .show(getSupportFragmentManager(), DIALOG_PUSH);
    }
}
