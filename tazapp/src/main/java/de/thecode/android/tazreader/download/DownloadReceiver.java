package de.thecode.android.tazreader.download;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.job.DownloadFinishedPaperJob;
import de.thecode.android.tazreader.job.DownloadFinishedResourceJob;
import de.thecode.android.tazreader.job.SyncJob;
import de.thecode.android.tazreader.notifications.NotificationUtils;
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

        StorageManager externalStorage = StorageManager.getInstance(context);
        DownloadManager downloadHelper = DownloadManager.getInstance(context);

        String action = intent.getAction();

        Timber.i("DownloadReceiver received intent: %s", intent);

        if (android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, 0);

            DownloadManager.DownloadState state = downloadHelper.getDownloadState(downloadId);
            boolean firstOccurrenceOfState = downloadHelper.isFirstOccurrenceOfState(state);
            if (!firstOccurrenceOfState) {
                Timber.e("DownloadState already received: %s", state);
                return;
            }

            Cursor cursor = context.getContentResolver()
                                   .query(Paper.CONTENT_URI, null, Paper.Columns.DOWNLOADID + " = " + downloadId, null, null);


            try {
                while (cursor.moveToNext()) {
                    Paper paper = new Paper(cursor);
                    Timber.i("Download complete for paper: %s, %s", paper, state);
                    try {
                        if (state.getStatus() == DownloadManager.DownloadState.STATUS_FAILED) {
                            throw new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                        } else if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {
                            File downloadFile = externalStorage.getDownloadFile(paper);
                            if (!downloadFile.exists()) throw new DownloadException("Downloaded paper file missing");
                            Timber.i("... checked file existence");
                            if (paper.getLen() != 0 && downloadFile.length() != paper.getLen())
                                throw new DownloadException(String.format(Locale.GERMANY,
                                                                          "Wrong size of paper download. expected: %d, file: %d, downloaded: %d",
                                                                          paper.getLen(),
                                                                          downloadFile.length(),
                                                                          state.getBytesDownloaded()));
                            Timber.i("... checked correct size of paper download");
                            try {
                                String fileHash = HashHelper.getHash(downloadFile, HashHelper.SHA_1);
                                if (!TextUtils.isEmpty(paper.getFileHash()) && !paper.getFileHash()
                                                                                     .equals(fileHash)) {
                                    throw new DownloadException(String.format(Locale.GERMANY,
                                                                              "Wrong paper file hash. Expected: %s, calculated: %s",
                                                                              paper.getFileHash(),
                                                                              fileHash));
                                }
                                Timber.i("... checked correct hash of paper download");
                            } catch (NoSuchAlgorithmException e) {
                                Timber.w(e);
                            } catch (IOException e) {
                                Timber.e(e);
                                throw new DownloadException(e);
                            }
                            DownloadFinishedPaperJob.scheduleJob(paper);
                        }
                    } catch (DownloadException e) {
                        Timber.e(e);
                        if (state.getReason() == 406) {
                            SyncJob.scheduleJobImmediately(false);
                            //SyncHelper.requestSync(context);
                        }
                        //AnalyticsWrapper.getInstance().logException(exception);
                        paper.setDownloadId(0);
                        context.getContentResolver()
                               .update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()),
                                       paper.getContentValues(),
                                       null,
                                       null);
                        if (externalStorage.getDownloadFile(paper)
                                           .exists()) //noinspection ResultOfMethodCallIgnored
                            externalStorage.getDownloadFile(paper)
                                           .delete();
                        new NotificationUtils(context).showDownloadErrorNotification(paper, null);
                        //NotificationHelper.showDownloadErrorNotification(context, null, paper.getId());

                        EventBus.getDefault()
                                .post(new PaperDownloadFailedEvent(paper.getId(), e));

                    }
                }
            } finally {
                cursor.close();
            }

            cursor = context.getContentResolver()
                            .query(Resource.CONTENT_URI, null, Resource.Columns.DOWNLOADID + " = " + downloadId, null, null);
            try {
                while (cursor.moveToNext()) {

                    Resource resource = new Resource(cursor);
                    //DownloadHelper.DownloadState downloadDownloadState = downloadHelper.getDownloadState(downloadId);
                    Timber.i("Download complete for resource: %s, %s", resource, state);
                    try {
                        if (state.getStatus() == DownloadManager.DownloadState.STATUS_FAILED) {
                            throw new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                        } else if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {
                            File downloadFile = externalStorage.getDownloadFile(resource);
                            if (!downloadFile.exists()) throw new DownloadException("Downloaded resource file missing");
                            Timber.i("... checked file existence");
                            if (resource.getLen() != 0 && downloadFile.length() != resource.getLen())
                                throw new DownloadException(String.format(Locale.GERMANY,
                                                                          "Wrong size of resource download. expected: %d, file: %d, downloaded: %d",
                                                                          resource.getLen(),
                                                                          downloadFile.length(),
                                                                          state.getBytesDownloaded()));
                            Timber.i("... checked correct size of resource download");
                            try {
                                String fileHash = HashHelper.getHash(downloadFile, HashHelper.SHA_1);
                                if (!TextUtils.isEmpty(resource.getFileHash()) && !resource.getFileHash()
                                                                                           .equals(fileHash))
                                    throw new DownloadException(String.format(Locale.GERMANY,
                                                                              "Wrong resource file hash. Expected: %s, calculated: %s",
                                                                              resource.getFileHash(),
                                                                              fileHash));
                                Timber.i("... checked correct hash of resource download");
                            } catch (NoSuchAlgorithmException e) {
                                Timber.w(e);
                            } catch (IOException e) {
                                Timber.e(e);
                                throw new DownloadException(e);
                            }
                            DownloadFinishedResourceJob.scheduleJob(resource);
                        }
                    } catch (DownloadException e) {
                        Timber.e(e);
                        resource.setDownloadId(0);
                        context.getContentResolver()
                               .update(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()),
                                       resource.getContentValues(),
                                       null,
                                       null);
                        EventBus.getDefault()
                                .post(new ResourceDownloadEvent(resource.getKey(), e));
                    }
                }
            } finally {
                cursor.close();
            }

        } else if (android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
            Intent libIntent = new Intent(context, StartActivity.class);
            libIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(libIntent);
        }
    }

    public class DownloadException extends ReadableException {
        public DownloadException() {
        }

        public DownloadException(String detailMessage) {
            super(detailMessage);
        }

        public DownloadException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public DownloadException(Throwable throwable) {
            super(throwable);
        }
    }

}
