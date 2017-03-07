package de.thecode.android.tazreader.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.start.StartActivity;

import timber.log.Timber;

/**
 * Created by mate on 09.03.2015.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    public static final String NOTIFICATION_CLICKED_ACTION = BuildConfig.APPLICATION_ID + ".notificationclicked";
    public static final String NOTIFICATION_DELETED_ACTION = BuildConfig.APPLICATION_ID + ".notificationdeleted";

    @Override
    public void onReceive(Context context, Intent intent) {

        Timber.d(intent.getAction());

        NotificationHelper.clearNotifiedPaperIds(context);

        if (intent.getAction()
                  .equals(NOTIFICATION_CLICKED_ACTION)) {
            Intent startIntent = new Intent(context, StartActivity.class);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
        } else if (intent.getAction()
                         .equals(NOTIFICATION_DELETED_ACTION)) {

        }

    }
}
