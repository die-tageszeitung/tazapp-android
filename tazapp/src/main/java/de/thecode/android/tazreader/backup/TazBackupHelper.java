package de.thecode.android.tazreader.backup;

import android.annotation.SuppressLint;
import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.os.Build;
import android.preference.PreferenceManager;

import timber.log.Timber;

/**
 * Created by mate on 01.03.2017.
 */

public class TazBackupHelper extends BackupAgentHelper {

    // A key to uniquely identify the set of backup data
    static final String PREFS_BACKUP_KEY = "prefs";

    @SuppressLint("NewApi")
    @Override
    public void onCreate() {
        super.onCreate();
        String prefsName = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? PreferenceManager.getDefaultSharedPreferencesName(
                this) : getDefaultSharedPreferencesName(this);
        SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, prefsName);
        addHelper(PREFS_BACKUP_KEY, helper);
    }

    @Override
    public void onQuotaExceeded(long backupDataBytes, long quotaBytes) {
        super.onQuotaExceeded(backupDataBytes, quotaBytes);
        Timber.e("Backup quota exceeded!");
    }

    @Override
    public void onRestoreFinished() {
        super.onRestoreFinished();
        Timber.i("Backup restore finished");
    }

    private static String getDefaultSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }


}
