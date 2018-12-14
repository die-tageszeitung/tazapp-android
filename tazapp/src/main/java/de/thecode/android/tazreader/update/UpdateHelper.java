package de.thecode.android.tazreader.update;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.OldDownloadManager;
import de.thecode.android.tazreader.utils.UserDeviceInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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


    private boolean googlePlayVersion = false;
    private TazSettings    settings;
    private boolean        updateMessageShown;
    private UserDeviceInfo userDeviceInfo;

    private UpdateHelper(Context context) {
        settings = TazSettings.getInstance(context);
        userDeviceInfo = UserDeviceInfo.getInstance(context);
        setGooglePlayVersion(context);
    }

    public boolean isGooglePlayVersion() {
        return googlePlayVersion;
    }

    public boolean hasUpdate() {
        return settings.getLatestVersion() > BuildConfig.VERSION_CODE;
    }

    private void setGooglePlayVersion(Context context) {
        try {
            String installer = context.getPackageManager()
                                      .getInstallerPackageName(userDeviceInfo.getPackageName());
            googlePlayVersion = InstallerID.GOOGLE_PLAY.containsId(installer);
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
        if (isGooglePlayVersion()) {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW,
                                                 Uri.parse("market://details?id=" + userDeviceInfo.getPackageName())));
            } catch (android.content.ActivityNotFoundException anfe) {
                OldDownloadManager.getInstance(context)
                                  .downloadUpdate();
            }
        } else {
            OldDownloadManager.getInstance(context)
                              .downloadUpdate();
        }
    }

    public enum InstallerID {
        GOOGLE_PLAY("com.android.vending", "com.google.android.feedback"), AMAZON_APP_STORE("com.amazon.venezia"), GALAXY_APPS(
                "com.sec.android.app.samsungapps");

        @NonNull
        private final List<String> ids;

        InstallerID(@NonNull String... id) {
            if (id.length == 1) ids = Collections.singletonList(id[0]);
            else ids = new ArrayList<>(Arrays.asList(id));
        }

        public @NonNull
        List<String> getIds() {
            return ids;
        }

        public boolean containsId(String id) {
            return ids.contains(id);
        }
    }
}
