package de.thecode.android.tazreader.download;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.crashlytics.android.Crashlytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.start.StartActivity;
import de.thecode.android.tazreader.utils.StorageManager;

public class DownloadReceiver extends BroadcastReceiver {

    private static final Logger log = LoggerFactory.getLogger(DownloadReceiver.class);


    //    Context mContext;
    //    ExternalStorage mStorage;

    @Override
    public void onReceive(Context context, Intent intent) {

        StorageManager externalStorage = StorageManager.getInstance(context);
        DownloadManager downloadHelper = DownloadManager.getInstance(context);

        String action = intent.getAction();

        log.trace("DownloadReceiver received intent: {}", intent);

        if (android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, 0);

            DownloadManager.DownloadState state = downloadHelper.getDownloadState(downloadId);
            boolean firstOccurrenceOfState = downloadHelper.isFirstOccurrenceOfState(state);
            if (!firstOccurrenceOfState) {
                log.error("DownloadState already received: {}", state);
                return;
            }

            Cursor cursor = context.getContentResolver()
                                   .query(Paper.CONTENT_URI, null, Paper.Columns.DOWNLOADID + " = " + downloadId, null, null);
            try {
                while (cursor.moveToNext()) {
                    Paper paper = new Paper(cursor);
                    //DownloadHelper.DownloadState downloadDownloadState = downloadHelper.getDownloadState(downloadId);
                    log.trace("Download complete for paper: {}", paper);
                    log.trace("{}", state);
                    boolean failed = false;
                    if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {
                        File downloadFile = externalStorage.getDownloadFile(paper);
                        if (!downloadFile.exists()) {
                            failed = true;
                        } else {
                            if (paper.getLen() != 0 && downloadFile.length() != paper.getLen()) {
                                log.error("Wrong size of paper download");
                                failed = true;
                            } else log.trace("... checked correct size of paper download");
                            try {
                                String fileHash = HashHelper.getHash(downloadFile, HashHelper.SHA_1);
                                if (paper.getFileHash() != null && !paper.getFileHash()
                                                                         .equals(fileHash)) {
                                    log.error("Wrong paper filehash");
                                    failed = true;
                                } else log.trace("... checked correct hash of paper download");
                            } catch (NoSuchAlgorithmException e) {
                                log.warn("",e);
                                Crashlytics.logException(e);
                            } catch (IOException e) {
                                log.error("",e);
                                failed = true;
                            }
                            if (!failed) {
                                Intent unzipIntent = new Intent(context, DownloadFinishedPaperService.class);
                                unzipIntent.putExtra(DownloadFinishedPaperService.PARAM_PAPER_ID, paper.getId());
                                context.startService(unzipIntent);
                            }
                        }
                    } else if (state.getStatus() == DownloadManager.DownloadState.STATUS_FAILED) {
                        failed = true;
                    }
                    if (failed) {
                        log.error("Download failed");
                        DownloadException exception = new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                        Crashlytics.logException(exception);
                        paper.setDownloadId(0);
                        context.getContentResolver()
                               .update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), paper.getContentValues(), null, null);
                        if (externalStorage.getDownloadFile(paper)
                                           .exists()) //noinspection ResultOfMethodCallIgnored
                            externalStorage.getDownloadFile(paper)
                                           .delete();
                        NotificationHelper.showDownloadErrorNotification(context, paper.getId());
                        EventBus.getDefault()
                                .post(new PaperDownloadFailedEvent(paper.getId(), exception));
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
                    log.trace("Download complete for resource: {}", resource);
                    log.trace("{}", state);

                    boolean failed = false;
                    if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {


                        File downloadFile = externalStorage.getDownloadFile(resource);
                        if (!downloadFile.exists()) {
                            failed = true;
                        } else {
                            if (resource.getLen() != 0 && downloadFile.length() != resource.getLen()) {
                                log.error("Wrong size of resource download");
                                failed = true;
                            } else log.trace("... checked correct size of resource download");
                            try {
                                String fileHash = HashHelper.getHash(downloadFile, HashHelper.SHA_1);
                                if (resource.getFileHash() != null && !resource.getFileHash()
                                                                               .equals(fileHash)) {
                                    log.error("Wrong resource filehash");
                                    failed = true;
                                } else log.trace("... checked correct hash of resource download");
                            } catch (NoSuchAlgorithmException e) {
                                log.warn("", e);
                                Crashlytics.logException(e);
                            } catch (IOException e) {
                                log.error("",e);
                                failed = true;
                            }
                            if (!failed) {
                                Intent unzipIntent = new Intent(context, DownloadFinishedResourceService.class);
                                unzipIntent.putExtra(DownloadFinishedResourceService.PARAM_RESOURCE_KEY, resource.getKey());
                                context.startService(unzipIntent);
                            }
                        }
                    } else if (state.getStatus() == DownloadManager.DownloadState.STATUS_FAILED) {
                        failed = true;
                    }
                    if (failed) {
                        log.error("Download failed");
                        DownloadException exception = new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                        Crashlytics.logException(exception);
                        resource.setDownloadId(0);
                        context.getContentResolver()
                               .update(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()), resource.getContentValues(), null, null);
                        EventBus.getDefault()
                                .post(new ResourceDownloadEvent(resource.getKey(), exception));
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

    public class DownloadException extends Exception {
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
