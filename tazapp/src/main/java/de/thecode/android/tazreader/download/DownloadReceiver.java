package de.thecode.android.tazreader.download;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.job.DownloadFinishedJob;
import de.thecode.android.tazreader.job.DownloadFinishedPaperJob;
import de.thecode.android.tazreader.job.DownloadFinishedResourceJob;
import de.thecode.android.tazreader.job.SyncJob;
import de.thecode.android.tazreader.notifications.NotificationUtils;
import de.thecode.android.tazreader.provider.TazProvider;
import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.start.StartActivity;
import de.thecode.android.tazreader.utils.ReadableException;
import de.thecode.android.tazreader.utils.StorageManager;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

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
