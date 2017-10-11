package de.thecode.android.tazreader.analytics;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.utils.Installation;

import net.ypresto.timbertreeutils.CrashlyticsLogExceptionTree;
import net.ypresto.timbertreeutils.CrashlyticsLogTree;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

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
        //TazAcraHelper.init((Application) context);
        initFabric(context);
    }

    private void initFabric(Context context) {
        if (!BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug")) {
            Timber.plant(new CrashlyticsLogExceptionTree());
            Timber.plant(new CrashlyticsLogTree(Log.INFO));
        }

        Crashlytics crashlyticsKit = new Crashlytics.Builder().core(
                new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG && !BuildConfig.BUILD_TYPE.equals("staging"))
                                             .build())
                                                              .build();

        Fabric fabric = new Fabric.Builder(context).kits(crashlyticsKit)
                                                   .debuggable(BuildConfig.DEBUG)
                                                   .build();


        Fabric.with(fabric);
        Crashlytics.setUserIdentifier(Installation.id(context));
    }

    public void logException(Throwable throwable) {
//        if (ACRA.isInitialised()) {
//            ACRA.getErrorReporter()
//                .handleException(throwable);
//        }
    }

    public void logExceptionSilent(Throwable throwable) {
//        if (ACRA.isInitialised()) {
//            ACRA.getErrorReporter()
//                .handleSilentException(throwable);
//        }
    }

    public void logData(String key, String value) {
//        if (ACRA.isInitialised()) {
//            ACRA.getErrorReporter()
//                .putCustomData(key, value);
//        }
    }

//    public void trackBreadcrumb(String event) {
//        logData("Event at " + System.currentTimeMillis(), event);
//    }

}
