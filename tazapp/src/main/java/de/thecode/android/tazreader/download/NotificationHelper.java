package de.thecode.android.tazreader.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.start.StartActivity;

import org.json.JSONArray;
import org.json.JSONException;

import timber.log.Timber;

/**
 * Created by mate on 09.03.2015.
 */
public class NotificationHelper {

    private static final int notificationSuccessId = 1;
    private static final int notificationErrorId   = 10;

    public static void showDownloadFinishedNotification(Context context, long paperId) {
        JSONArray notifiedPaperIds = getNotifiedPaperIds(context);
        notifiedPaperIds.put(paperId);
        TazSettings.getInstance(context)
                   .setPref(TazSettings.PREFKEY.PAPERNOTIFICATIONIDS, notifiedPaperIds.toString());
        showDownloadFinishedNotification(context, true, true);
    }

    private static JSONArray getNotifiedPaperIds(Context context) {
        JSONArray notifiedPaperIds = new JSONArray();
        try {
            notifiedPaperIds = new JSONArray(TazSettings.getInstance(context)
                                                        .getPrefString(TazSettings.PREFKEY.PAPERNOTIFICATIONIDS, "[]"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return notifiedPaperIds;
    }

    public static void clearNotifiedPaperIds(Context context) {
        TazSettings.getInstance(context)
                   .removePref(TazSettings.PREFKEY.PAPERNOTIFICATIONIDS);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationSuccessId);
    }

    public static void removeNotifiedPaperId(Context context, long paperId) {
        JSONArray notifiedPaperIds = getNotifiedPaperIds(context);
        JSONArray newNotifiedPaperIds = new JSONArray();
        for (int i = 0; i < notifiedPaperIds.length(); i++) {
            try {
                if (notifiedPaperIds.getLong(i) != paperId) {
                    newNotifiedPaperIds.put(notifiedPaperIds.getLong(i));
                }
            } catch (JSONException e) {
                Timber.w(e);
            }
        }

        if (newNotifiedPaperIds.length() == 0) {
            clearNotifiedPaperIds(context);
        } else if (newNotifiedPaperIds.length() != notifiedPaperIds.length()) {
            TazSettings.getInstance(context)
                       .setPref(TazSettings.PREFKEY.PAPERNOTIFICATIONIDS, newNotifiedPaperIds.toString());
            showDownloadFinishedNotification(context, false, false);
        }
    }

    private static void showDownloadFinishedNotification(Context context, boolean withVibration, boolean withSound) {
        JSONArray notifiedPaperIds = getNotifiedPaperIds(context);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationSuccessId);
        Intent intent = new Intent(NotificationBroadcastReceiver.NOTIFICATION_CLICKED_ACTION);
        PendingIntent contentIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        Intent intent2 = new Intent(NotificationBroadcastReceiver.NOTIFICATION_DELETED_ACTION);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0, intent2, 0);
        StringBuilder bigTextBuilder = new StringBuilder();
        for (int i = 0; i < notifiedPaperIds.length(); i++) {
            try {
                Paper paper = new Paper(context, notifiedPaperIds.getLong(i));
                if (i >= 1) bigTextBuilder.append("\n");
                bigTextBuilder.append(paper.getTitelWithDate(context));
            } catch (JSONException | Paper.PaperNotFoundException e) {
                Timber.w(e);
            }
        }


        String contentText = "";
        if (notifiedPaperIds.length() > 1) {
            contentText = String.format(context.getString(R.string.notification_content_text_summary), notifiedPaperIds.length());
        } else {
            try {
                contentText = new Paper(context, notifiedPaperIds.getLong(0)).getTitelWithDate(context);
            } catch (JSONException | Paper.PaperNotFoundException e) {
                Timber.w(e);
            }
        }

        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context);
        nBuilder.setContentTitle(context.getString(R.string.notification_message_install_finished))
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigTextBuilder.toString()))
                .setAutoCancel(true)
                .setNumber(notifiedPaperIds.length())
                .setGroup(context.getString(R.string.notification_group_name))
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setColor(ContextCompat.getColor(context, R.color.notification))
                .setSmallIcon(R.drawable.notification_icon);

        if (withSound) {
            Uri ringtoneUri = TazSettings.getInstance(context)
                                         .getNotificationSoundUri(TazSettings.PREFKEY.NOTIFICATION_SOUND_DOWNLOAD);
            if (ringtoneUri != null) nBuilder.setSound(ringtoneUri);
        }
        if (withVibration) {
            if (TazSettings.getInstance(context)
                           .getPrefBoolean(TazSettings.PREFKEY.VIBRATE, false))
                nBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }

        Notification notification = nBuilder.build();
        notificationManager.notify(notificationSuccessId, notification);
    }

    public static void showDownloadErrorNotification(Context context, String extraMessage, long paperId) {
        Paper paper;
        try {
            paper = new Paper(context, paperId);
            NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(context);

            StringBuilder message = new StringBuilder(
                    String.format(context.getString(R.string.dialog_error_download), paper.getTitelWithDate(context)));

            if (!TextUtils.isEmpty(extraMessage)) message.append("\n")
                                                         .append(extraMessage);

            Intent intent = new Intent(context, StartActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

            nBuilder.setContentTitle(context.getString(R.string.notification_error_download_title))
                    .setContentText(message.toString())
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)
                    .setGroup("taznot")
                    .setDefaults(Notification.DEFAULT_LIGHTS)
                    .setColor(context.getResources()
                                     .getColor(R.color.notification))
                    .setSmallIcon(android.R.drawable.stat_notify_error);
            Uri ringtoneUri = TazSettings.getInstance(context)
                                         .getNotificationSoundUri(TazSettings.PREFKEY.NOTIFICATION_SOUND_DOWNLOAD);
            if (ringtoneUri != null) nBuilder.setSound(ringtoneUri);
            if (TazSettings.getInstance(context)
                           .getPrefBoolean(TazSettings.PREFKEY.VIBRATE, false))
                nBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
            Notification notification = nBuilder.build();

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            int notificationId = notificationErrorId + (int) paperId;
            notificationManager.cancel(notificationId);
            notificationManager.notify(notificationId, notification);
        } catch (Paper.PaperNotFoundException e) {
            Timber.w(e);
        }


    }

    public static void cancelDownloadErrorNotification(Context context, long paperId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = notificationErrorId + (int) paperId;
        notificationManager.cancel(notificationId);
    }


}
