package de.thecode.android.tazreader.firebase;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.push.PushHelper;
import de.thecode.android.tazreader.push.PushNotification;

import timber.log.Timber;

/**
 * Created by mate on 24.07.2017.
 */

public class TazFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Timber.d("");
        super.onMessageReceived(remoteMessage);

        // Check if message contains a data payload.
        if (remoteMessage.getData()
                         .size() > 0) {
            PushNotification pushNotification = new PushNotification(remoteMessage.getData()
                                                                                  .get(PushHelper.PAYLOAD_TYPE),
                                                                     remoteMessage.getData()
                                                                                            .get(PushHelper.PAYLOAD_BODY),
                                                                     remoteMessage.getData()
                                                                                            .get(PushHelper.PAYLOAD_URL),
                                                                     remoteMessage.getData()
                                                                                            .get(PushHelper.PAYLOAD_ISSUE));

            PushHelper.dispatchPushNotification(this, pushNotification);


        }
    }

    @Override
    public void onDeletedMessages() {
        Timber.d("");
        super.onDeletedMessages();
    }

    @Override
    public void onNewToken(String refreshedToken) {
        Timber.d("Refreshed token: %s", refreshedToken);
        TazSettings.getInstance(this).setFirebaseToken(refreshedToken);
    }
}
