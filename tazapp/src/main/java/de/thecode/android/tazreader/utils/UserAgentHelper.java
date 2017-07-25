package de.thecode.android.tazreader.utils;

import android.content.Context;
import android.os.Build;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;

/**
 * Created by mate on 25.07.2017.
 */

public class UserAgentHelper {

    public static final String USER_AGENT_HEADER_NAME = "User-Agent";

    private static volatile UserAgentHelper mInstance;
    private final String userAgentHeaderValue;

    public static UserAgentHelper getInstance(Context context) {
        if (mInstance == null) {
            synchronized (UserAgentHelper.class) {
                if (mInstance == null) {
                    mInstance = new UserAgentHelper(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private UserAgentHelper(Context context) {
        this.userAgentHeaderValue = "Android" + " " + Build.VERSION.RELEASE + " " + (context.getResources()
                                                                                            .getBoolean(
                                                                                                    R.bool.isTablet) ? "Tablet" : "Phone") +
                " (" + BuildConfig.VERSION_CODE + ";" + BuildConfig.VERSION_NAME + ";" +
                Build.BRAND + ";" + Build.MODEL + ")"/*+" [" + Installation.id(context) + "]"*/;
    }

    public String getUserAgentHeaderValue() {
        return userAgentHeaderValue;
    }
}
