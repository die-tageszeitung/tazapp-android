package de.thecode.android.tazreader.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.secure.Installation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by mate on 05.02.2018.
 */

public class UserDeviceInfo {

    private static Map<String, Integer> archMap = new HashMap<>();

    static {
        //Arch, Weight
        archMap.put("armeabi", 1);
        archMap.put("armeabi-v7a", 2);
        archMap.put("arm64-v8a", 3);
        archMap.put("mips", 4);
        archMap.put("mips64", 5);
        archMap.put("x86", 6);
        archMap.put("x86_64", 7);
    }

    public static int getWeightForArch(String arch) {
        Integer result = archMap.get(arch);
        if (result == null) return 0;
        return result;
    }

    public static String getArchForWeight(int weight) {
        for (Map.Entry<String, Integer> archEntry : archMap.entrySet()) {
            if (archEntry.getValue() == weight) return archEntry.getKey();
        }
        return null;
    }

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
    private List<String> supportedArchList;
    private String       installationId;
    private String       packageName;

    private UserDeviceInfo(Context context) {
        installationId = Installation.id(context);
        versionName = BuildConfig.VERSION_NAME;
        packageName = context.getPackageName();
        try {
            PackageInfo packageInfo = context.getPackageManager()
                                             .getPackageInfo(packageName, 0);
            versionName = packageInfo.versionName;
        } catch (Exception ignored) {
        }
        versionName += " (" + BuildConfig.VERSION_CODE + ")";

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

    public String getPackageName() {
        return packageName;
    }
}
