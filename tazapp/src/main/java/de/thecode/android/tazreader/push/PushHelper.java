package de.thecode.android.tazreader.push;

import com.google.firebase.iid.FirebaseInstanceId;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;

import timber.log.Timber;

/**
 * Created by mate on 24.07.2017.
 */

public class PushHelper {

    public static final String PAYLOAD_TYPE  = "type";
    public static final String PAYLOAD_TITLE = "title";
    public static final String PAYLOAD_BODY  = "body";
    public static final String PAYLOAD_URL   = "url";
    public static final String PAYLOAD_ISSUE = "issue";

    public static final String PARAMETER_TOKEN        = "deviceToken";
    public static final String PARAMETER_DEVICETYPE   = "deviceType";
    public static final String PARAMETER_DEVICEFORMAT = "deviceFormat";
    public static final String PARAMETER_APPVERSION   = "appVersion";
    public static final String PARAMETER_APPBUILD     = "appBuild";
    public static final String PARAMETER_SOUND        = "deviceMessageSound";
    public static final String PARAMETER_KUNDENID     = "KundenId";
    public static final String PARAMETER_AUSGABENART  = "AusgabeArt";
    public static final String PARAMETER_TESTKANAL    = "isTestkanal";

    private static String getDeviceToken() {
        return FirebaseInstanceId.getInstance()
                                 .getToken();
    }

    public static Uri addToUri(@NonNull Uri uri) {
        Uri.Builder uriBuilder = uri.buildUpon();
        uriBuilder.appendQueryParameter(PARAMETER_TOKEN, getDeviceToken());
        return uriBuilder.build();
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
