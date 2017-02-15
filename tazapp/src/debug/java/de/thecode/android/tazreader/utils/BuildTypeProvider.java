package de.thecode.android.tazreader.utils;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by mate on 10.05.2016.
 */
public class BuildTypeProvider {

    public static void installStetho(Application application) {
        Stetho.initialize(
                Stetho.newInitializerBuilder(application)
                      .enableDumpapp(Stetho.defaultDumperPluginsProvider(application))
                      .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(application))
                      .build());
    }

    public static void addStethoInterceptor(OkHttpClient.Builder builder) {
        builder.addNetworkInterceptor(new StethoInterceptor());
    }

    public static void addLoggingInterceptor(OkHttpClient.Builder builder) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            private final Logger log = LoggerFactory.getLogger(BuildTypeProvider.class);
            @Override
            public void log(String message) {
                log.debug("{}", message);
            }
        });
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addNetworkInterceptor(logging);
    }

}
