package de.thecode.android.tazreader.analytics;

import android.app.Application;
import android.content.Context;

import de.thecode.android.tazreader.acra.TazAcraHelper;

import org.acra.ACRA;

/**
 * Created by mate on 10.02.2017.
 */

public class AnalyticsWrapper {

    static AnalyticsWrapper instance;

    public static void initialize(Context context) {
        if (instance != null) throw new IllegalStateException("AnalyticsWrapper must only be initialized once");
        instance = new AnalyticsWrapper(context);
    }

    public static AnalyticsWrapper getInstance() {
        if (instance == null) throw new IllegalStateException("AnalyticsWrapper must be initialized before");
        return instance;
    }

    private AnalyticsWrapper(Context context) {
        TazAcraHelper.init((Application) context);
    }

    public void logException(Throwable throwable) {
        if (ACRA.isInitialised()) {
            ACRA.getErrorReporter()
                .handleException(throwable);
        }
    }

    public void logExceptionSilent(Throwable throwable) {
        if (ACRA.isInitialised()) {
            ACRA.getErrorReporter()
                .handleSilentException(throwable);
        }
    }

    public void logData(String key, String value) {
        if (ACRA.isInitialised()) {
            ACRA.getErrorReporter()
                .putCustomData(key, value);
        }
    }

    public void trackBreadcrumb(String event) {
        logData("Event at " + System.currentTimeMillis(), event);
    }

}
