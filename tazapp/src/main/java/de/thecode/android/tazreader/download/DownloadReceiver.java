package de.thecode.android.tazreader.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.start.StartActivity;
import de.thecode.android.tazreader.worker.DownloadFinishedWorker;

import timber.log.Timber;

public class DownloadReceiver extends BroadcastReceiver {

    //    Context mContext;
    //    ExternalStorage mStorage;

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        Timber.i("DownloadReceiver received intent: %s", intent);
        long downloadId = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        OldDownloadManager.DownloadManagerInfo downloadManagerInfo = OldDownloadManager.getInstance(context)
                                                                                       .getDownloadManagerInfo(downloadId);
        String downloadStateUri = downloadManagerInfo.getUri() == null ? null : downloadManagerInfo.getUri()
                                                                                                   .toString();
        boolean isAppUpdate = false;
        if (!TextUtils.isEmpty(downloadStateUri) && downloadStateUri.startsWith(BuildConfig.APKURL)) isAppUpdate = true;
        if (android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            if (!isAppUpdate) DownloadFinishedWorker.scheduleNow(downloadId);
        } else if (android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
            if (!isAppUpdate) {
                Intent libIntent = new Intent(context, StartActivity.class);
                libIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(libIntent);
            }
        }
    }
}
