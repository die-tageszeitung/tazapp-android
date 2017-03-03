package de.thecode.android.tazreader.acra;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.acra.ACRA;
import org.acra.dialog.BaseCrashReportDialog;
import org.acra.prefs.PrefUtils;
import org.acra.prefs.SharedPreferencesFactory;

/**
 * Created by mate on 03.03.2017.
 */

public class NewTazCrashDialog extends BaseCrashReportDialog implements CrashDialogFragment.CrashActivtyCallback {

    private static final String DIALOG_TAG = "CRASHDIALOG";
    private SharedPreferencesFactory sharedPreferencesFactory;

    @Override
    protected void init(@Nullable Bundle savedInstanceState) {
        sharedPreferencesFactory = new SharedPreferencesFactory(getApplicationContext(), getConfig());
        if (getSupportFragmentManager().findFragmentByTag(DIALOG_TAG) == null) {
            CrashDialogFragment.newInstance(getConfig())
                               .show(getSupportFragmentManager(), DIALOG_TAG);
        }
    }

    @Override
    public void onSend(String comment, String userEmail, boolean always) {
        final SharedPreferences prefs = sharedPreferencesFactory.create();
        final SharedPreferences.Editor prefEditor = prefs.edit();
        if (always) prefEditor.putBoolean(ACRA.PREF_ALWAYS_ACCEPT, true);
        if (TextUtils.isEmpty(comment)) {
            comment = "";
        }
        if (!TextUtils.isEmpty(userEmail)) {
            prefEditor.putString(ACRA.PREF_USER_EMAIL_ADDRESS, userEmail);
        } else {
            userEmail = "";
        }
        PrefUtils.save(prefEditor);
        sendCrash(comment, userEmail);
        finish();
    }

    @Override
    public void onCancel() {
        cancelReports();
        finish();
    }


}
