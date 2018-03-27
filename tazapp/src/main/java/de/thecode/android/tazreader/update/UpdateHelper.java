package de.thecode.android.tazreader.update;

import android.content.Context;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Publication;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.utils.UserDeviceInfo;

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
    private TazSettings settings;


    private UpdateHelper(Context context) {
        setStoreVersion(context);
        settings = TazSettings.getInstance(context);
        UserDeviceInfo userDeviceInfo = UserDeviceInfo.getInstance(context);
    }

    public boolean isStoreVersion() {
        return storeVersion;
    }

    public boolean hasUpdate() {
        return true;//settings.getLatestVersion() > BuildConfig.VERSION_CODE;
    }

    private void setStoreVersion(Context context) {
        try {
            String installer = context.getPackageManager()
                                      .getInstallerPackageName(context.getPackageName());
            storeVersion = !TextUtils.isEmpty(installer);
        } catch (Throwable e) {
            Timber.w(e);
        }
    }

}
