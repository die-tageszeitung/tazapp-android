package de.thecode.android.tazreader.utils;

import android.content.Context;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

/**
 * Created by mate on 10.05.2016.
 */
public class BuildTypeProvider {

    public static void installStetho(Context context) {
        Stetho.initialize(
                Stetho.newInitializerBuilder(context)
                      .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(context))
                      .build());
    }

    public static void addStethoInterceptor(OkHttpClient.Builder builder) {
        builder.addNetworkInterceptor(new StethoInterceptor());
    }

}
