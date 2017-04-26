package de.thecode.android.tazreader.download;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.start.StartActivity;
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
                Timber.w("DownloadState already received: %s", state);
                return;
            }

            Cursor cursor = context.getContentResolver()
                                   .query(Paper.CONTENT_URI, null, Paper.Columns.DOWNLOADID + " = " + downloadId, null, null);
            try {
                while (cursor.moveToNext()) {
                    Paper paper = new Paper(cursor);
                    //DownloadHelper.DownloadState downloadDownloadState = downloadHelper.getDownloadState(downloadId);
                    Timber.i("Download complete for paper: %s, %s", paper, state);
                    boolean failed = false;
                    if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {
                        File downloadFile = externalStorage.getDownloadFile(paper);
                        if (!downloadFile.exists()) {
                            failed = true;
                        } else {
                            if (paper.getLen() != 0 && downloadFile.length() != paper.getLen()) {
                                Timber.e("Wrong size of paper download");
                                failed = true;
                            } else Timber.i("... checked correct size of paper download");
                            try {
                                String fileHash = HashHelper.getHash(downloadFile, HashHelper.SHA_1);
                                if (paper.getFileHash() != null && !paper.getFileHash()
                                                                         .equals(fileHash)) {
                                    Timber.e("Wrong paper filehash");
                                    failed = true;
                                } else Timber.i("... checked correct hash of paper download");
                            } catch (NoSuchAlgorithmException e) {
                                Timber.w(e);
                            } catch (IOException e) {
                                Timber.e(e);
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
                        Timber.e("Download failed");
                        DownloadException exception = new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                        //AnalyticsWrapper.getInstance().logException(exception);
                        paper.setDownloadId(0);
                        context.getContentResolver()
                               .update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), paper.getContentValues(), null, null);
                        if (externalStorage.getDownloadFile(paper)
                                           .exists()) //noinspection ResultOfMethodCallIgnored
                            externalStorage.getDownloadFile(paper)
                                           .delete();
                        NotificationHelper.showDownloadErrorNotification(context, null, paper.getId());
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
                    Timber.i("Download complete for resource: %s, %s", resource, state);

                    boolean failed = false;
                    if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {


                        File downloadFile = externalStorage.getDownloadFile(resource);
                        if (!downloadFile.exists()) {
                            failed = true;
                        } else {
                            if (resource.getLen() != 0 && downloadFile.length() != resource.getLen()) {
                                Timber.e("Wrong size of resource download");
                                failed = true;
                            } else Timber.i("... checked correct size of resource download");
                            try {
                                String fileHash = HashHelper.getHash(downloadFile, HashHelper.SHA_1);
                                if (resource.getFileHash() != null && !resource.getFileHash()
                                                                               .equals(fileHash)) {
                                    Timber.e("Wrong resource filehash");
                                    failed = true;
                                } else Timber.i("... checked correct hash of resource download");
                            } catch (NoSuchAlgorithmException e) {
                                Timber.w(e);
                                //AnalyticsWrapper.getInstance().logException(e);
                            } catch (IOException e) {
                                Timber.e(e);
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
                        Timber.e("Download failed");
                        DownloadException exception = new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                        //AnalyticsWrapper.getInstance().logException(exception);
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
