package de.thecode.android.tazreader.update;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseArray;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.utils.UserDeviceInfo;

import java.util.List;

import timber.log.Timber;

public class UpdateHelper {

    private static volatile UpdateHelper mInstance;

    public static UpdateHelper getInstance(Context context) {
        if (mInstance == null) {
            synchronized (UpdateHelper.class) {
                if (mInstance == null) {
                    mInstance = new UpdateHelper(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }


    private boolean storeVersion = false;
    private TazSettings    settings;
    private boolean        updateMessageShown;
    private UserDeviceInfo userDeviceInfo;

    private UpdateHelper(Context context) {
        setStoreVersion(context);
        settings = TazSettings.getInstance(context);
        userDeviceInfo = UserDeviceInfo.getInstance(context);
    }

    public boolean isStoreVersion() {
        return storeVersion;
    }

    public boolean hasUpdate() {
        return settings.getLatestVersion() > BuildConfig.VERSION_CODE;
    }

    private void setStoreVersion(Context context) {
        try {
            String installer = context.getPackageManager()
                                      .getInstallerPackageName(userDeviceInfo.getPackageName());
            storeVersion = !TextUtils.isEmpty(installer);
        } catch (Throwable e) {
            Timber.w(e);
        }
    }

    public void setLatestVersion(String latestVersion) {
        try {
            settings.setLatestVersion(Integer.valueOf(latestVersion));
        } catch (NumberFormatException e) {
            Timber.e(e);
        }
    }

    public void setUpdateMessageShown(boolean updateMessageShown) {
        this.updateMessageShown = updateMessageShown;
    }

    public boolean isUpdateMessageShown() {
        return updateMessageShown;
    }

    public void update(Context context) {
        if (isStoreVersion()) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW,
                                                 Uri.parse("market://details?id=" + userDeviceInfo.getPackageName())));
            } catch (android.content.ActivityNotFoundException anfe) {
                context.startActivity(new Intent(Intent.ACTION_VIEW,
                                                 Uri.parse("https://play.google.com/store/apps/details?id=" + userDeviceInfo)));
            }
        } else {
            DownloadManager.getInstance(context).downloadUpdate();
        }
    }
}
