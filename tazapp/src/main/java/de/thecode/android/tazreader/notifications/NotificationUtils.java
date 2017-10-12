package de.thecode.android.tazreader.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;

/**
 * Created by mate on 12.10.2017.
 */

public class NotificationUtils extends ContextWrapper {

    public static final String DOWNLOAD_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".DOWNLOAD";

    private NotificationManager notificationManagerCompat;
    private String              downloadChannelName;


    public NotificationUtils(Context base) {
        super(base);
        downloadChannelName = base.getString(R.string.notification_channel_download);
        createChannels();
    }

    private void createChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel downloadChannel = new NotificationChannel(DOWNLOAD_CHANNEL_ID,
                                                                          downloadChannelName,
                                                                          NotificationManagerCompat.IMPORTANCE_DEFAULT);
            downloadChannel.enableLights(true);
            downloadChannel.enableVibration(true);
            downloadChannel.setLightColor(ContextCompat.getColor(this, R.color.color_primary));
            downloadChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getManager().createNotificationChannel(downloadChannel);
        }
    }

    private NotificationManager getManager() {
        if (notificationManagerCompat == null) {
            notificationManagerCompat = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManagerCompat;
    }

}
