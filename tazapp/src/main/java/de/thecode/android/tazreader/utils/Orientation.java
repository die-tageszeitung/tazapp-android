package de.thecode.android.tazreader.utils;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;

/**
 * Created by mate on 03.02.2016.
 */
public class Orientation {

    public static void setActivityOrientation(Activity activity, String orientationKey) {
        setActivityOrientation(activity, getOrientationFromConfiguration(orientationKey));
    }


    private static int getOrientationFromConfiguration(String orientationKey)
    {
        int preferenceOrientation = Configuration.ORIENTATION_UNDEFINED;
        if ("auto".equals(orientationKey))
            preferenceOrientation = Configuration.ORIENTATION_UNDEFINED;
        else if ("land".equals(orientationKey))
            preferenceOrientation = Configuration.ORIENTATION_LANDSCAPE;
        else if ("port".equals(orientationKey))
            preferenceOrientation = Configuration.ORIENTATION_PORTRAIT;
        return preferenceOrientation;
    }

    private static void setActivityOrientation(Activity activity, int orientationFromConfiguration) {
        if (orientationFromConfiguration == Configuration.ORIENTATION_LANDSCAPE) {
            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                // You need to check if your desired orientation isn't
                // already set because setting orientation restarts your
                // Activity which takes long
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        } else if (orientationFromConfiguration == Configuration.ORIENTATION_PORTRAIT) {
            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        } else {
            if (activity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_USER) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
        }
    }
}
