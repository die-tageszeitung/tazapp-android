package de.thecode.android.tazreader.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.job.DownloadFinishedJob;
import de.thecode.android.tazreader.start.StartActivity;

import timber.log.Timber;

public class DownloadReceiver extends BroadcastReceiver {

    //    Context mContext;
    //    ExternalStorage mStorage;

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        Timber.i("DownloadReceiver received intent: %s", intent);

        if (android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            DownloadFinishedJob.scheduleJob(downloadId);
        } else if (android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
            Intent libIntent = new Intent(context, StartActivity.class);
            libIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(libIntent);
        }
    }
}
