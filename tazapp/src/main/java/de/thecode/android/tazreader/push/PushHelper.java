package de.thecode.android.tazreader.push;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;

import org.greenrobot.eventbus.EventBus;

import okhttp3.FormBody;
import okhttp3.RequestBody;
import timber.log.Timber;

/**
 * Created by mate on 24.07.2017.
 */

public class PushHelper {

    private static volatile PushHelper mInstance;

    public static PushHelper getInstance(Context context) {
        if (mInstance == null) {
            synchronized (PushHelper.class) {
                if (mInstance == null) {
                    mInstance = new PushHelper(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    public static final String PAYLOAD_TYPE  = "type";
    public static final String PAYLOAD_TITLE = "title";
    public static final String PAYLOAD_BODY  = "body";
    public static final String PAYLOAD_URL   = "url";
    public static final String PAYLOAD_ISSUE = "issue";

    public static final String PARAMETER_TOKEN        = "deviceToken";
    public static final String PARAMETER_OLDTOKEN     = "oldDeviceToken ";
    public static final String PARAMETER_DEVICETYPE   = "deviceType";
    public static final String PARAMETER_DEVICEFORMAT = "deviceFormat";
    public static final String PARAMETER_APPVERSION   = "appVersion";
    public static final String PARAMETER_APPBUILD     = "appBuild";
    public static final String PARAMETER_SOUND        = "deviceMessageSound";
    public static final String PARAMETER_PUSH_ACTIVE  = "benachrichtigungen";

    private TazSettings settings;

    private String deviceFormat;

    private PushHelper(Context context) {
        settings = TazSettings.getInstance(context);
        deviceFormat = context.getResources()
                              .getBoolean(R.bool.isTablet) ? "Tablet" : "Handy";
    }

    private String getDeviceToken() {
        return settings.getFirebaseToken();
    }

    private String getOldDeviceToken() {
        return settings.getOldFirebaseToken();
    }

    private String getDeviceType() {
        return "Android";
    }

    public String getDeviceFormat() {
        return deviceFormat;
    }

    public String getAppVersion() {
        return String.valueOf(BuildConfig.VERSION_CODE);
    }

    public String getAppBuild() {
        return BuildConfig.VERSION_NAME;
    }

    public String getDeviceMessageSound() {
        Uri ringtoneUri = settings.getNotificationSoundUri(TazSettings.PREFKEY.NOTIFICATION_SOUND_PUSH);
        if (ringtoneUri != null) {
            return ringtoneUri.toString();
        }
        return "";
    }

    public boolean getPushActive() {
        return settings.getPrefBoolean(TazSettings.PREFKEY.NOTIFICATION_PUSH, true);
    }


    public Uri addToUri(@NonNull Uri uri) {
        Uri.Builder uriBuilder = uri.buildUpon();
        uriBuilder.appendQueryParameter(PARAMETER_TOKEN, getDeviceToken())
                  .appendQueryParameter(PARAMETER_DEVICETYPE, getDeviceType())
                  .appendQueryParameter(PARAMETER_DEVICEFORMAT, getDeviceFormat())
                  .appendQueryParameter(PARAMETER_APPVERSION, getAppVersion())
                  .appendQueryParameter(PARAMETER_APPBUILD, getAppBuild())
                  .appendQueryParameter(PARAMETER_SOUND, getDeviceMessageSound())
                  .appendQueryParameter(PARAMETER_PUSH_ACTIVE, String.valueOf(getPushActive()));
        return uriBuilder.build();
    }

    public RequestBody getOkhttp3RequestBody() {
        String token = getDeviceToken();
        String oldtoken = getOldDeviceToken();
        FormBody.Builder builder = new FormBody.Builder().add(PARAMETER_TOKEN, token)
                                                         .add(PARAMETER_DEVICETYPE, getDeviceType())
                                                         .add(PARAMETER_DEVICEFORMAT, getDeviceFormat())
                                                         .add(PARAMETER_APPVERSION, getAppVersion())
                                                         .add(PARAMETER_APPBUILD, getAppBuild())
                                                         .add(PARAMETER_SOUND, getDeviceMessageSound())
                                                         .add(PARAMETER_PUSH_ACTIVE, String.valueOf(getPushActive()));

        if (!TextUtils.isEmpty(oldtoken) && !oldtoken.equals(token)) {
            builder.add(PARAMETER_OLDTOKEN, oldtoken);
        }
        return builder.build();
    }

    public static void checkIntentForFCMPushNotificationExtras(Intent intent) {
        if (intent.hasExtra("google.message_id")) {
            dispatchPushNotification(new PushNotification(intent.getStringExtra(PAYLOAD_TYPE),
                                                          intent.getStringExtra(PAYLOAD_TITLE),
                                                          intent.getStringExtra(PAYLOAD_BODY),
                                                          intent.getStringExtra(PAYLOAD_URL),
                                                          intent.getStringExtra(PAYLOAD_ISSUE)));
        }
    }

    public static void dispatchPushNotification(PushNotification pushNotification) {
        Timber.d("dispatching push notification %s %s",
                 pushNotification.getType()
                                 .name(),
                 pushNotification.getTitle());
        switch (pushNotification.getType()) {
            case alert:
                EventBus.getDefault()
                        .post(pushNotification);
                break;
            case debug:
                if (BuildConfig.DEBUG) {
                    EventBus.getDefault()
                            .post(pushNotification);
                }
                Timber.i("Debug notification received");
                break;
            case newIssue:
                //TODO newIssue
                Timber.i("newIssue notification received, TODO");
                break;
            default:
                Timber.e("Unknown notification received");
                break;
        }
    }
}
