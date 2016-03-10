package de.thecode.android.tazreader.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thecode.android.tazreader.start.StartActivity;

/**
 * Created by mate on 09.03.2015.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger(NotificationBroadcastReceiver.class);

    public static final String NOTIFICATION_CLICKED_ACTION = "de.thecode.tazreader.notificationclicked";
    public static final String NOTIFICATION_DELETED_ACTION = "de.thecode.tazreader.notificationdeleted";

    @Override
    public void onReceive(Context context, Intent intent) {

        log.debug(intent.getAction());

        NotificationHelper.clearNotifiedPaperIds(context);

        if (intent.getAction().equals(NOTIFICATION_CLICKED_ACTION)) {
            Intent startIntent = new Intent(context, StartActivity.class);
            startIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
        } else if (intent.getAction().equals(NOTIFICATION_DELETED_ACTION)) {

        }

    }
}
