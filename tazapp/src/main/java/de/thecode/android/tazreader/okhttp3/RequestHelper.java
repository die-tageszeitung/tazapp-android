package de.thecode.android.tazreader.okhttp3;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.push.PushHelper;
import de.thecode.android.tazreader.sync.AccountHelper;

import okhttp3.FormBody;
import okhttp3.RequestBody;

/**
 * Created by mate on 31.08.2017.
 */

public class RequestHelper {

    private static final String PARAMETER_TOKEN        = "deviceToken";
    private static final String PARAMETER_OLDTOKEN     = "oldDeviceToken ";
    private static final String PARAMETER_DEVICETYPE   = "deviceType";
    private static final String PARAMETER_DEVICEFORMAT = "deviceFormat";
    private static final String PARAMETER_APPVERSION   = "appVersion";
    private static final String PARAMETER_APPBUILD     = "appBuild";
    private static final String PARAMETER_SOUND        = "deviceMessageSound";
    private static final String PARAMETER_PUSH_ACTIVE  = "Benachrichtigungen";
    private static final String PARAMETER_ABOID        = "AboId";

    private static volatile RequestHelper mInstance;

    public static RequestHelper getInstance(Context context) {
        if (mInstance == null) {
            synchronized (PushHelper.class) {
                if (mInstance == null) {
                    mInstance = new RequestHelper(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private TazSettings   settings;
    private String        deviceFormat;
    private AccountHelper accountHelper;

    private RequestHelper(Context context) {
        settings = TazSettings.getInstance(context);
        deviceFormat = context.getResources()
                              .getBoolean(R.bool.isTablet) ? "Tablet" : "Handy";
        accountHelper = AccountHelper.getInstance(context);
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

    private String getDeviceFormat() {
        return deviceFormat;
    }

    private String getAppVersion() {
        return String.valueOf(BuildConfig.VERSION_CODE);
    }

    private String getAppBuild() {
        return BuildConfig.VERSION_NAME;
    }

    private String getDeviceMessageSound() {
        Uri ringtoneUri = settings.getNotificationSoundUri(TazSettings.PREFKEY.NOTIFICATION_SOUND_PUSH);
        if (ringtoneUri != null) {
            return ringtoneUri.toString();
        }
        return "";
    }

    private boolean getPushActive() {
        return settings.getPrefBoolean(TazSettings.PREFKEY.NOTIFICATION_PUSH, true);
    }

    private String getAboID() {
        return accountHelper.getUser("");
    }


    public Uri addToUri(@NonNull Uri uri) {
        Uri.Builder uriBuilder = uri.buildUpon();
        uriBuilder.appendQueryParameter(PARAMETER_TOKEN, getDeviceToken())
                  .appendQueryParameter(PARAMETER_DEVICETYPE, getDeviceType())
                  .appendQueryParameter(PARAMETER_DEVICEFORMAT, getDeviceFormat())
                  .appendQueryParameter(PARAMETER_APPVERSION, getAppVersion())
                  .appendQueryParameter(PARAMETER_APPBUILD, getAppBuild())
                  .appendQueryParameter(PARAMETER_SOUND, getDeviceMessageSound())
                  .appendQueryParameter(PARAMETER_PUSH_ACTIVE, String.valueOf(getPushActive()))
                  .appendQueryParameter(PARAMETER_ABOID, getAboID());
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
                                                         .add(PARAMETER_PUSH_ACTIVE, String.valueOf(getPushActive()))
                                                         .add(PARAMETER_ABOID, getAboID());

        if (!TextUtils.isEmpty(oldtoken) && !oldtoken.equals(token)) {
            builder.add(PARAMETER_OLDTOKEN, oldtoken);
        }
        return builder.build();
    }

}
