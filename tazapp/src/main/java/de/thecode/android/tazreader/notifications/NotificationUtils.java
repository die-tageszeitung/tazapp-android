package de.thecode.android.tazreader.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.start.StartActivity;

import java.io.IOException;

/**
 * Created by mate on 12.10.2017.
 */

public class NotificationUtils extends ContextWrapper {

    public static final  int    DOWNLOAD_NOTIFICTAION_ID = 1;
    private static final String DOWNLOAD_GROUP_KEY       = "notificationDownloadGroup";
    public static final  String DOWNLOAD_CHANNEL_ID      = BuildConfig.APPLICATION_ID + ".DOWNLOAD";

    public static final String NOTIFICATION_EXTRA_TYPE_ID = "typeId";
    public static final String NOTIFICATION_EXTRA_BOOKID  = "notificationBookId";

    private NotificationManager notificationManager;
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
                                                                                                .setSound(ringtoneUri);
        int defaults = Notification.DEFAULT_LIGHTS;
        if (withVibration) defaults |= Notification.DEFAULT_VIBRATE;
        builder.setDefaults(defaults);
        getManager().notify(paper.getBookId(), DOWNLOAD_NOTIFICTAION_ID, builder.build());
    }

    public void showDownloadFinishedNotification(Paper paper) {

        Uri ringtoneUri = TazSettings.getInstance(this)
                                     .getNotificationSoundUri(TazSettings.PREFKEY.NOTIFICATION_SOUND_DOWNLOAD);
        boolean withVibration = TazSettings.getInstance(this)
                                           .getPrefBoolean(TazSettings.PREFKEY.VIBRATE, true);

        Intent intent = new Intent(this, StartActivity.class);
        intent.putExtra(NOTIFICATION_EXTRA_BOOKID, paper.getBookId());
        intent.putExtra(NOTIFICATION_EXTRA_TYPE_ID, DOWNLOAD_NOTIFICTAION_ID);
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
                                                                                                .setSound(ringtoneUri)
                                                                                                .setGroup(DOWNLOAD_GROUP_KEY);
        int defaults = Notification.DEFAULT_LIGHTS;
        if (withVibration) defaults |= Notification.DEFAULT_VIBRATE;
        builder.setDefaults(defaults);

        try {
            builder.setLargeIcon(Picasso.with(this)
                                        .load(paper.getImage())
                                        .transform(new LargeIconTransformation())
                                        .transform(new CircleTransform())
                                        .get());
        } catch (IOException | IllegalStateException ignored) {

        }

        getManager().notify(paper.getBookId(), DOWNLOAD_NOTIFICTAION_ID, builder.build());
    }

    public void removeDownloadNotification(Paper paper) {
        if (paper != null) {
            getManager().cancel(paper.getBookId(), DOWNLOAD_NOTIFICTAION_ID);
        }
    }

    public void removeDownloadNotification(long paperId) {
        removeDownloadNotification(Paper.getPaperWithId(this, paperId));
    }


    private static class LargeIconTransformation implements Transformation {

        @Override
        public Bitmap transform(Bitmap source) {
            if (source != null) {
                int dim = Math.min(source.getWidth(), source.getHeight());

                Bitmap dstBmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(dstBmp);
                canvas.drawBitmap(source, 0, 0, null);
                source.recycle();
                return dstBmp;
            }
            return null;
        }

        @Override
        public String key() {
            return "largeIconTaz";
        }
    }

    private static class CircleTransform implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }

            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    }
}
