
package de.thecode.android.tazreader.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import de.thecode.android.tazreader.data.TazSettings;

public class Utils {


    // Connection ++++++++++++++++++

    public static final int CONNECTION_NOT_AVAILABLE = -1;
    public static final int CONNECTION_MOBILE_ROAMING = 0;
    public static final int CONNECTION_MOBILE = 1;
    public static final int CONNECTION_FAST = 2;

    public static int getConnectionType(Context context) {
        int result = CONNECTION_NOT_AVAILABLE;
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null) {
            switch (info.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_BLUETOOTH:
                case ConnectivityManager.TYPE_ETHERNET:
                    result = CONNECTION_FAST;
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    TelephonyManager tmanager = (TelephonyManager) context
                            .getSystemService(Context.TELEPHONY_SERVICE);
                    if (tmanager.isNetworkRoaming())
                        result = CONNECTION_MOBILE_ROAMING;
                    else
                        result = CONNECTION_MOBILE;
                    break;
            }
        }
        Log.t( "Connection Type:",result);
        return result;
    }



//    public static boolean deleteDir(File dir) {
//        deleteDirContent(dir);
//        return dir.delete();
//    }
//
//    public static boolean deleteDirContent(File dir) {
//        if (dir.isDirectory()) {
//            String[] children = dir.list();
//            for (int i = 0; i < children.length; i++) {
//                boolean success = deleteDir(new File(dir, children[i]));
//                if (!success) {
//                    return false;
//                }
//            }
//        }
//        return true;
//    }

    // Orientation
    public static void setActivityOrientationFromPrefs(Activity activity)
    {
        if (!orientationLocked)
        {
            setActivityOrientation(activity, getOrientationFromConfigurationFromPrefs(activity));
        }
    }

    public static int getOrientationFromConfigurationFromPrefs(Context context)
    {
        String orientationKey = TazSettings.getPrefString(context, TazSettings.PREFKEY.ORIENTATION, "0");
        int preferenceOrientation = Configuration.ORIENTATION_UNDEFINED;
        if (orientationKey.equals("auto"))
            preferenceOrientation = Configuration.ORIENTATION_UNDEFINED;
        else if (orientationKey.equals("land"))
            preferenceOrientation = Configuration.ORIENTATION_LANDSCAPE;
        else if (orientationKey.equals("port"))
            preferenceOrientation = Configuration.ORIENTATION_PORTRAIT;
        return preferenceOrientation;
    }

    public static void setActivityOrientation(Activity activity, int orientationFromConfiguration) {
        if (orientationFromConfiguration == Configuration.ORIENTATION_LANDSCAPE) {
            if (Build.VERSION.SDK_INT < 9) {
                if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    // You need to check if your desired orientation isn't
                    // already set because setting orientation restarts your
                    // Activity which takes long
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            } else {
                if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                    // You need to check if your desired orientation isn't
                    // already set because setting orientation restarts your
                    // Activity which takes long
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
            }
        } else if (orientationFromConfiguration == Configuration.ORIENTATION_PORTRAIT) {
            if (Build.VERSION.SDK_INT < 9) {
                if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            } else {
                if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
                    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                }
            }
        } else {
            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_USER) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
        }
    }

    public static boolean orientationLocked = false;



}
