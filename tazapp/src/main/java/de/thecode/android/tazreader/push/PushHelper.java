package de.thecode.android.tazreader.push;

import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.sync.SyncHelper;

import org.greenrobot.eventbus.EventBus;

import timber.log.Timber;

/**
 * Created by mate on 24.07.2017.
 */

public class PushHelper {

    public static final String PAYLOAD_TYPE  = "type";
    public static final String PAYLOAD_BODY  = "body";
    public static final String PAYLOAD_URL   = "url";
    public static final String PAYLOAD_ISSUE = "issue";

    public static void checkIntentForFCMPushNotificationExtras(Context context, Intent intent) {
        if (intent.hasExtra("google.message_id")) {
            dispatchPushNotification(context, new PushNotification(intent.getStringExtra(PAYLOAD_TYPE),
                                                          intent.getStringExtra(PAYLOAD_BODY),
                                                          intent.getStringExtra(PAYLOAD_URL),
                                                          intent.getStringExtra(PAYLOAD_ISSUE)));
        }
    }

    public static void dispatchPushNotification(Context context, PushNotification pushNotification) {
        Timber.d("dispatching push notification %s",
                 pushNotification.getType()
                                 .name());
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
                Timber.i("newIssue notification received");
                SyncHelper.requestSync(context);
                break;
            default:
                Timber.e("Unknown notification received");
                break;
        }
    }

}
