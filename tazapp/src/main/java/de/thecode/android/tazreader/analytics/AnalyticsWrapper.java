package de.thecode.android.tazreader.analytics;

import android.content.Context;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.secure.Installation;
import de.thecode.android.tazreader.sync.AccountHelper;

import net.ypresto.timbertreeutils.CrashlyticsLogExceptionTree;
import net.ypresto.timbertreeutils.CrashlyticsLogTree;
import net.ypresto.timbertreeutils.LogExclusionStrategy;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

/**
 * Created by mate on 10.02.2017.
 */

public class AnalyticsWrapper {

    private static AnalyticsWrapper instance;


    public static void initialize(Context context) {
        if (instance != null) throw new IllegalStateException("AnalyticsWrapper must only be initialized once");
        instance = new AnalyticsWrapper(context);

    }

    public static AnalyticsWrapper getInstance() {
        if (instance == null) throw new IllegalStateException("AnalyticsWrapper must be initialized before");
        return instance;
    }

    private TazSettings settings;

    private AnalyticsWrapper(Context context) {
        initFabric(context);
        settings = TazSettings.getInstance(context);
    }

    private void initFabric(Context context) {
        /* if (!BuildConfig.BUILD_TYPE.equalsIgnoreCase("debug")) {
            Timber.plant(new CrashlyticsLogTree(Log.INFO));
            Timber.plant(new CrashlyticsLogExceptionTree(Log.ERROR, (int priority, String tag, String message, Throwable t) -> {
                    return !settings.getCrashlyticsAlwaysSend();
            }));
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
        setUserEncrypted(AccountHelper.getInstance(context).getUser(""));*/
    }

    public void setUserEncrypted(String user){
        /* try {
            Crashlytics.setUserName(HashHelper.getHash(user,HashHelper.UTF_8,HashHelper.SHA_1));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            Timber.e(e);
        } */
    }

    public void logData(String key, String value) {
        /*Crashlytics.setString(key,value);*/
    }
}
