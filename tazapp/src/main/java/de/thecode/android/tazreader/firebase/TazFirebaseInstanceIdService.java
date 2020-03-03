package de.thecode.android.tazreader.firebase;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;

import de.thecode.android.tazreader.data.TazSettings;

import timber.log.Timber;

/**
 * Created by mate on 24.07.2017.
 */

public class TazFirebaseInstanceIdService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String refreshedToken) {
        super.onNewToken(refreshedToken);
        // Get updated InstanceID token.
        Timber.d("Refreshed token: %s", refreshedToken);
        TazSettings.getInstance(this).setFirebaseToken(refreshedToken);
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        // sendRegistrationToServer(refreshedToken);
    }
}
