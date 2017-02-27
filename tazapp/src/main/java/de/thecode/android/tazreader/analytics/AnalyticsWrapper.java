package de.thecode.android.tazreader.analytics;

import android.app.Application;
import android.content.Context;

import de.thecode.android.tazreader.BuildConfig;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;
import org.acra.sender.HttpSenderFactory;

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
        //TODO init crashlogger framework
    }


    public void logException(Throwable throwable) {

        //TODO log exception
    }

    public void logData(String key, String value) {
        //TODO log data
    }

    public static void initializeAcra(Application application) {
        try {
            final ACRAConfiguration acraConfig = new ConfigurationBuilder(application).setSocketTimeout(30000)
                                                                                      .setReportSenderFactoryClasses(
                                                                                              HttpSenderFactory.class)
                                                                                      .setReportingInteractionMode(
                                                                                              ReportingInteractionMode.DIALOG)
                                                                                      .setHttpMethod(HttpSender.Method.PUT)
                                                                                      .setReportType(HttpSender.Type.JSON)
                                                                                      .setFormUri(BuildConfig.ACRA_FORM_URI)
                                                                                      .setFormUriBasicAuthLogin(
                                                                                              BuildConfig.ACRA_FORM_URI_BASIC_AUTH_LOGIN)
                                                                                      .setFormUriBasicAuthPassword(
                                                                                              BuildConfig.ACRA_FORM_URI_BASIC_AUTH_PASSWORD)
                                                                                      .build();
            ACRA.init(application, acraConfig);
        } catch (ACRAConfigurationException e) {
            Timber.e(e, "Cannot build ACRA configuration");
        }

    }
}
