package de.thecode.android.tazreader.analytics;

import android.content.Context;

/**
 * Created by mate on 10.02.2017.
 */

public class AnalyticsWrapper {

    static AnalyticsWrapper instance;

    public static void initialize(Context context, boolean optOut) {
        if (instance != null) throw new IllegalStateException("AnalyticsWrapper must only be initialized once");
        instance = new AnalyticsWrapper(context, optOut);
    }

    public static AnalyticsWrapper getInstance() {
        if (instance == null) throw new IllegalStateException("AnalyticsWrapper must be initialized before");
        return instance;
    }

    final boolean optOut;

    private AnalyticsWrapper(Context context, boolean optOut) {
        this.optOut = optOut;
        //TODO init crashlogger framework
    }


    public void logException(Throwable throwable) {
        if (!optOut){
            //TODO log exception
        }
    }

    public void logData(String key, String value){
        if (!optOut){
            //TODO log data
        }
    }
}
