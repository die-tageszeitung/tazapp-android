package de.thecode.android.tazreader.acra;

import android.app.Application;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;

import timber.log.Timber;

/**
 * Created by mate on 06.03.2017.
 */

public class TazAcraHelper {

    public static void init(Application application) {

        try {
            ConfigurationBuilder acraConfigBuilder = new ConfigurationBuilder(application);
            acraConfigBuilder.setSocketTimeout(30000)
                             .setConnectionTimeout(30000)
                             .setHttpMethod(HttpSender.Method.PUT)
                             .setReportType(HttpSender.Type.JSON)
                             .setFormUri(BuildConfig.ACRA_FORM_URI)
                             .setFormUriBasicAuthLogin(BuildConfig.ACRA_FORM_URI_BASIC_AUTH_LOGIN)
                             .setFormUriBasicAuthPassword(BuildConfig.ACRA_FORM_URI_BASIC_AUTH_PASSWORD)
                             .setReportingInteractionMode(ReportingInteractionMode.DIALOG)
                             .setResToastText(R.string.crash_toast_text)
                             .setResDialogText(R.string.crash_dialog_text)
                             .setResDialogIcon(R.drawable.ic_emoticon_dead_32dp)
                             .setResDialogTitle(R.string.crash_dialog_title)
                             .setResDialogCommentPrompt(R.string.crash_dialog_comment_prompt)
                             .setResDialogOkToast(R.string.crash_dialog_ok_toast)
                             .setReportDialogClass(NewTazCrashDialog.class)
                             .setReportField(ReportField.BUILD_CONFIG, false)
                             .setReportField(ReportField.USER_IP, false)
                             .setReportSenderFactoryClasses(Okhttp3Sender.Factory.class);

            final ACRAConfiguration acraConfig = acraConfigBuilder.build();
            ACRA.init(application, acraConfig);
            ACRA.getErrorReporter()
                .setEnabled(!"debug".equalsIgnoreCase(BuildConfig.BUILD_TYPE));
        } catch (ACRAConfigurationException e) {
            Timber.e(e, "Cannot build ACRA configuration");
        }

    }

}
