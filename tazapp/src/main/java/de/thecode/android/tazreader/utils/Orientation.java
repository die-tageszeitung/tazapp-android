package de.thecode.android.tazreader.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;

import de.thecode.android.tazreader.data.TazSettings;

/**
 * Created by mate on 03.02.2016.
 */
public class Orientation {

    public static boolean orientationLocked = false;

    public static void setActivityOrientationFromPrefs(Activity activity)
    {
        if (!orientationLocked)
        {
            setActivityOrientation(activity, getOrientationFromConfigurationFromPrefs(activity));
        }
    }

    public static int getOrientationFromConfigurationFromPrefs(Context context)
    {
        String orientationKey = TazSettings.getInstance(context).getPrefString(TazSettings.PREFKEY.ORIENTATION, "0");
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
}
