package de.thecode.android.tazreader.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.secure.Installation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by mate on 05.02.2018.
 */

public class UserDeviceInfo {
    private static volatile UserDeviceInfo mInstance;

    public static UserDeviceInfo getInstance(Context context) {
        if (mInstance == null) {
            synchronized (UserDeviceInfo.class) {
                if (mInstance == null) {
                    mInstance = new UserDeviceInfo(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private String       versionName;
    private     List<String> supportedArchList;
    private String installationId;

    private UserDeviceInfo(Context context) {
        installationId = Installation.id(context);
        versionName = BuildConfig.VERSION_NAME;
        try {
            PackageInfo packageInfo = context.getPackageManager()
                                                  .getPackageInfo(context.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (Exception ignored) {
        }
        versionName += " ("+BuildConfig.VERSION_CODE+")";

        String[] supportedArch;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            supportedArch = Build.SUPPORTED_ABIS;
        } else {
            supportedArch = new String[]{Build.CPU_ABI, Build.CPU_ABI2};
        }
        supportedArchList = new ArrayList<>(Arrays.asList(supportedArch));
        supportedArchList.removeAll(Arrays.asList("", null)); //remove empty
        supportedArchList = new ArrayList<>(new LinkedHashSet<>(supportedArchList)); //remove duplicate

    }

    public String getVersionName() {
        return versionName;
    }

    public List<String> getSupportedArchList() {
        return supportedArchList;
    }

    public String getInstallationId() {
        return installationId;
    }
}
