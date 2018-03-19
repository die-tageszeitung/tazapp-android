package de.thecode.android.tazreader.download;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

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
                    //DownloadHelper.DownloadState downloadDownloadState = downloadHelper.getDownloadState(downloadId);
                    Timber.i("Download complete for paper: %s, %s", paper, state);
                    DownloadException downloadException = null;
                    if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {
                        File downloadFile = externalStorage.getDownloadFile(paper);
                        if (!downloadFile.exists()) {
                            downloadException = new DownloadException("Downloaded paper file missing");
                        } else {
                            if (paper.getLen() != 0 && downloadFile.length() != paper.getLen()) {
                                downloadException = new DownloadException("Wrong size of paper download. expected: "+paper.getLen()+" downloaded: "+downloadFile.length());
                            } else Timber.i("... checked correct size of paper download");
                            try {
                                String fileHash = HashHelper.getHash(downloadFile, HashHelper.SHA_1);
                                if (paper.getFileHash() != null && !paper.getFileHash()
                                                                         .equals(fileHash)) {
                                    downloadException = new DownloadException("Wrong paper filehash.");
                                } else Timber.i("... checked correct hash of paper download");
                            } catch (NoSuchAlgorithmException e) {
                                Timber.w(e);
                            } catch (IOException e) {
                                Timber.e(e);
                                downloadException = new DownloadException(e);
                            }
                            if (downloadException == null) {
                                DownloadFinishedPaperJob.scheduleJob(paper);
//                                Intent unzipIntent = new Intent(context, DownloadFinishedPaperService.class);
//                                unzipIntent.putExtra(DownloadFinishedPaperService.PARAM_PAPER_ID, paper.getId());
//                                context.startService(unzipIntent);
                            }
                        }
                    } else if (state.getStatus() == DownloadManager.DownloadState.STATUS_FAILED) {
                        downloadException = new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                    }
                    if (downloadException != null) {
                        Timber.e(downloadException);
                        if (state.getReason() == 406) {
                            SyncJob.scheduleJobImmediately(false);
                            //SyncHelper.requestSync(context);
                        }
                        //AnalyticsWrapper.getInstance().logException(exception);
                        paper.setDownloadId(0);
                        context.getContentResolver()
                               .update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), paper.getContentValues(), null, null);
                        if (externalStorage.getDownloadFile(paper)
                                           .exists()) //noinspection ResultOfMethodCallIgnored
                            externalStorage.getDownloadFile(paper)
                                           .delete();
                        new NotificationUtils(context).showDownloadErrorNotification(paper, null);
                        //NotificationHelper.showDownloadErrorNotification(context, null, paper.getId());

                        EventBus.getDefault()
                                .post(new PaperDownloadFailedEvent(paper.getId(), downloadException));
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

                    DownloadException downloadException = null;
                    if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {


                        File downloadFile = externalStorage.getDownloadFile(resource);
                        if (!downloadFile.exists()) {
                            downloadException = new DownloadException("Downloaded resource file missing");
                        } else {
                            if (resource.getLen() != 0 && downloadFile.length() != resource.getLen()) {
                                downloadException = new DownloadException("Wrong size of resource download. expected: "+resource.getLen()+" downloaded: "+downloadFile.length());
                            } else Timber.i("... checked correct size of resource download");
                            try {
                                String fileHash = HashHelper.getHash(downloadFile, HashHelper.SHA_1);
                                if (resource.getFileHash() != null && !resource.getFileHash()
                                                                               .equals(fileHash)) {
                                    downloadException = new DownloadException("Wrong resource filehash.");
                                } else Timber.i("... checked correct hash of resource download");
                            } catch (NoSuchAlgorithmException e) {
                                Timber.w(e);
                                //AnalyticsWrapper.getInstance().logException(e);
                            } catch (IOException e) {
                                Timber.e(e);
                                downloadException = new DownloadException(e);
                            }
                            if (downloadException == null) {
                                DownloadFinishedResourceJob.scheduleJob(resource);
//                                Intent unzipIntent = new Intent(context, DownloadFinishedResourceService.class);
//                                unzipIntent.putExtra(DownloadFinishedResourceService.PARAM_RESOURCE_KEY, resource.getPath());
//                                context.startService(unzipIntent);
                            }
                        }
                    } else if (state.getStatus() == DownloadManager.DownloadState.STATUS_FAILED) {
                        downloadException = new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                    }
                    if (downloadException != null) {
                        Timber.e(downloadException);
                        resource.setDownloadId(0);
                        context.getContentResolver()
                               .update(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()), resource.getContentValues(), null, null);
                        EventBus.getDefault()
                                .post(new ResourceDownloadEvent(resource.getKey(), downloadException));
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
