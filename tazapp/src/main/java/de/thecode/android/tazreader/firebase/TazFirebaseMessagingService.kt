package de.thecode.android.tazreader.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.thecode.android.tazreader.data.TazSettings
import de.thecode.android.tazreader.push.PushHelper
import de.thecode.android.tazreader.push.PushNotification
import timber.log.Timber

/**
 * Created by mate on 24.07.2017.
 */
class TazFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("")
        super.onMessageReceived(remoteMessage)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            val pushNotification = PushNotification(
                    remoteMessage.data[PushHelper.PAYLOAD_TYPE],
                    remoteMessage.data[PushHelper.PAYLOAD_BODY],
                    remoteMessage.data[PushHelper.PAYLOAD_URL],
                    remoteMessage.data[PushHelper.PAYLOAD_ISSUE]
            )
            PushHelper.dispatchPushNotification(this, pushNotification)
        }
    }

    override fun onDeletedMessages() {
        Timber.d("")
        super.onDeletedMessages()
    }

    override fun onNewToken(refreshedToken: String) {
        super.onNewToken(refreshedToken)
        // Get updated InstanceID token.
        Timber.d("Refreshed token: %s", refreshedToken)
        TazSettings.getInstance(this).firebaseToken = refreshedToken
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        // sendRegistrationToServer(refreshedToken);
    }
}