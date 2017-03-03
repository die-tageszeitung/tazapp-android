package de.thecode.android.tazreader.analytics;

import android.app.Application;
import android.content.Context;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.acra.NewTazCrashDialog;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;

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

        initializeAcra((Application) context);
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

    private void initializeAcra(Application application) {

        try {
            ConfigurationBuilder acraConfigBuilder = new ConfigurationBuilder(application);
            acraConfigBuilder.setSocketTimeout(30000)
                             .setHttpMethod(HttpSender.Method.PUT)
                             .setReportType(HttpSender.Type.JSON)
                             .setFormUri(BuildConfig.ACRA_FORM_URI)
                             .setReportingInteractionMode(ReportingInteractionMode.DIALOG)
                             .setResToastText(R.string.crash_toast_text)
                             .setResDialogText(R.string.crash_dialog_text)
                             .setResDialogIcon(R.drawable.ic_emoticon_dead_32dp)
                             .setResDialogTitle(R.string.crash_dialog_title)
                             .setResDialogCommentPrompt(R.string.crash_dialog_comment_prompt)
                             .setResDialogOkToast(R.string.crash_dialog_ok_toast)
                             .setReportDialogClass(NewTazCrashDialog.class)
                             .setReportField(ReportField.BUILD_CONFIG, false)
                             .setReportField(ReportField.USER_IP, false);

            final ACRAConfiguration acraConfig = acraConfigBuilder.build();
            ACRA.init(application, acraConfig);
//            ACRA.getErrorReporter()
//                .setEnabled(!BuildConfig.DEBUG);
        } catch (ACRAConfigurationException e) {
            Timber.e(e, "Cannot build ACRA configuration");
        }

    }
}
