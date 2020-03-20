package de.thecode.android.tazreader.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.start.StartActivity;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by mate on 12.10.2017.
 */

public class NotificationUtils extends ContextWrapper {

    public static final  int    DOWNLOAD_NOTIFICATION_ID = 1;
    public static final  int    AUDIOSERVICE_NOTIFICATION_ID = 2;
    private static final String DOWNLOAD_GROUP_KEY       = "notificationDownloadGroup";
    public static final  String DOWNLOAD_CHANNEL_ID      = BuildConfig.APPLICATION_ID + ".DOWNLOAD";
    public static final String MESSAGE_CHANNEL_ID = "MESSAGE";
    public static final  String AUDIO_CHANNEL_ID      = BuildConfig.APPLICATION_ID + ".AUDIO";

    public static final String TAG_NOTIFICATION_UPDATE = "appUpdate";

    public static final String NOTIFICATION_EXTRA_TYPE_ID = "typeId";
    public static final String NOTIFICATION_EXTRA_BOOKID  = "notificationBookId";

    private static volatile NotificationUtils mInstance;

    public static NotificationUtils getInstance(Context context) {
        if (mInstance == null) {
            synchronized (NotificationUtils.class) {
                if (mInstance == null) {
                    mInstance = new NotificationUtils(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }



    private NotificationManager notificationManager;
    private String              downloadChannelName;
    private String              messageChannelName;
    private String              audioChannelName;


    private NotificationUtils(Context base) {
        super(base);
        downloadChannelName = base.getString(R.string.notification_channel_download);
        messageChannelName = base.getString(R.string.notification_channel_message);
        audioChannelName = base.getString(R.string.notification_channel_audio);
    }

    public void createChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            List<NotificationChannel> channelList = new ArrayList<>();


            NotificationChannel downloadChannel = new NotificationChannel(DOWNLOAD_CHANNEL_ID,
                                                                          downloadChannelName,
                                                                          NotificationManager.IMPORTANCE_DEFAULT);
            downloadChannel.enableLights(true);
            downloadChannel.enableVibration(true);
            downloadChannel.setLightColor(ContextCompat.getColor(this, R.color.color_primary));
            downloadChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channelList.add(downloadChannel);

            NotificationChannel messageChannel = new NotificationChannel(MESSAGE_CHANNEL_ID,
                                                                          messageChannelName,
                                                                          NotificationManager.IMPORTANCE_DEFAULT);
            messageChannel.enableLights(true);
            messageChannel.enableVibration(true);
            messageChannel.setLightColor(ContextCompat.getColor(this, R.color.color_primary));
            messageChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channelList.add(messageChannel);


            NotificationChannel audioChannel = new NotificationChannel(AUDIO_CHANNEL_ID,
                                                                         audioChannelName,
                                                                         NotificationManager.IMPORTANCE_DEFAULT);
            audioChannel.setSound(null,null);
            audioChannel.enableLights(false);
            audioChannel.enableVibration(false);
            audioChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channelList.add(audioChannel);


            getManager().createNotificationChannels(channelList);

        }
    }

    private NotificationManager getManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    public void showDownloadErrorNotification(Paper paper, String extraMessage) {
        Uri ringtoneUri = TazSettings.getInstance(this)
                                     .getNotificationSoundUri(TazSettings.PREFKEY.NOTIFICATION_SOUND_DOWNLOAD);
        boolean withVibration = TazSettings.getInstance(this)
                                           .getPrefBoolean(TazSettings.PREFKEY.VIBRATE, true);

        StringBuilder message = new StringBuilder(String.format(getString(R.string.dialog_error_download),
                                                                paper.getTitelWithDate(this)));

        if (!TextUtils.isEmpty(extraMessage)) message.append("\n")
                                                     .append(extraMessage);

        Intent intent = new Intent(this, StartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
        PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                                                                            DOWNLOAD_CHANNEL_ID).setSmallIcon(R.drawable.ic_stat_error)
                                                                                                .setColor(ContextCompat.getColor(
                                                                                                        this,
                                                                                                        R.color.notification))
                                                                                                .setContentTitle(getString(R.string.notification_error_download_title))
                                                                                                .setContentIntent(contentIntent)
                                                                                                .setAutoCancel(true)
                                                                                                .setContentText(message.toString())
                                                                                                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                                                                                                        message.toString()))
                                                                                                .setSound(ringtoneUri);
        int defaults = Notification.DEFAULT_LIGHTS;
        if (withVibration) defaults |= Notification.DEFAULT_VIBRATE;
        builder.setDefaults(defaults);
        getManager().notify(paper.getBookId(), DOWNLOAD_NOTIFICATION_ID, builder.build());
    }

    public void showDownloadFinishedNotification(Paper paper) {

        Uri ringtoneUri = TazSettings.getInstance(this)
                                     .getNotificationSoundUri(TazSettings.PREFKEY.NOTIFICATION_SOUND_DOWNLOAD);
        boolean withVibration = TazSettings.getInstance(this)
                                           .getPrefBoolean(TazSettings.PREFKEY.VIBRATE, true);

        Intent intent = new Intent(this, StartActivity.class);
        intent.putExtra(NOTIFICATION_EXTRA_BOOKID, paper.getBookId());
        intent.putExtra(NOTIFICATION_EXTRA_TYPE_ID, DOWNLOAD_NOTIFICATION_ID);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
        PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                                                                            DOWNLOAD_CHANNEL_ID).setSmallIcon(R.drawable.notification_icon)
                                                                                                .setColor(ContextCompat.getColor(
                                                                                                        this,
                                                                                                        R.color.notification))
                                                                                                .setContentTitle(getString(R.string.notification_message_install_finished))
                                                                                                .setContentIntent(contentIntent)
                                                                                                .setAutoCancel(true)
                                                                                                .setContentText(paper.getTitelWithDate(
                                                                                                        this))
                                                                                                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                                                                                                        paper.getTitelWithDate(
                                                                                                                this)))
                                                                                                .setSound(ringtoneUri)
                                                                                                .setGroup(DOWNLOAD_GROUP_KEY);
        int defaults = Notification.DEFAULT_LIGHTS;
        if (withVibration) defaults |= Notification.DEFAULT_VIBRATE;
        builder.setDefaults(defaults);

        getManager().notify(paper.getBookId(), DOWNLOAD_NOTIFICATION_ID, builder.build());
    }

    public void removeDownloadNotification(String bookId) {
        getManager().cancel(bookId, DOWNLOAD_NOTIFICATION_ID);
    }


}
