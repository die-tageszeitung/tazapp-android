package de.thecode.android.tazreader.job;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.download.PaperDownloadFailedEvent;
import de.thecode.android.tazreader.download.ResourceDownloadEvent;
import de.thecode.android.tazreader.notifications.NotificationUtils;
import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.utils.ReadableException;
import de.thecode.android.tazreader.utils.StorageManager;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import timber.log.Timber;

public class DownloadFinishedJob extends Job {

    public static final  String TAG             = BuildConfig.FLAVOR + "_" + "download_finished_job";
    private static final String ARG_DOWNLOAD_ID = "downloadId";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        PersistableBundleCompat extras = params.getExtras();
        long downloadId = extras.getLong(ARG_DOWNLOAD_ID, -1);
        if (downloadId != -1) {

            StorageManager externalStorage = StorageManager.getInstance(getContext());
            DownloadManager downloadHelper = DownloadManager.getInstance(getContext());
            PaperRepository paperRepository = PaperRepository.getInstance(getContext());
            ResourceRepository resourceRepository = ResourceRepository.getInstance(getContext());


            DownloadManager.DownloadState state = downloadHelper.getDownloadState(downloadId);
            boolean firstOccurrenceOfState = downloadHelper.isFirstOccurrenceOfState(state);
            if (!firstOccurrenceOfState) {
                Timber.e("DownloadState already received: %s", state);
                return Result.SUCCESS;
            }

            Paper paper = paperRepository.getPaperWithDownloadId(downloadId);
            if (paper != null) {
                Timber.i("Download complete for paper: %s, %s", paper, state);
                paper.setDownloadId(0);

                try {
                    if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {
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
                        paper.setState(Paper.STATE_DOWNLOADED);
                        paperRepository.savePaper(paper);
                        DownloadFinishedPaperJob.scheduleJob(paper);
                    } else {
                        throw new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                    }
                } catch (DownloadException e) {
                    Timber.e(e);
                    paper.setState(Paper.STATE_NONE);
                    paperRepository.savePaper(paper);
                    if (state.getReason() == 406) {
                        SyncJob.scheduleJobImmediately(false);
                        //SyncHelper.requestSync(context);
                    }
                    //AnalyticsWrapper.getInstance().logException(exception);
//                        context.getContentResolver()
//                               .update(TazProvider.getContentUri(Paper.CONTENT_URI, paper.getBookId()),
//                                       paper.getContentValues(),
//                                       null,
//                                       null);
                    if (externalStorage.getDownloadFile(paper)
                                       .exists()) //noinspection ResultOfMethodCallIgnored
                        externalStorage.getDownloadFile(paper)
                                       .delete();
                    if (state.getStatus() != DownloadManager.DownloadState.STATUS_NOTFOUND) {
                        NotificationUtils.getInstance(getContext())
                                         .showDownloadErrorNotification(paper, getContext().getString(R.string.download_error_hints));
                        //NotificationHelper.showDownloadErrorNotification(context, null, paper.getId());

                        EventBus.getDefault()
                                .post(new PaperDownloadFailedEvent(paper, e));
                    }

                }

            }

            Resource resource = resourceRepository.getWithDownloadId(downloadId);
            if (resource != null) {

                //DownloadHelper.DownloadState downloadDownloadState = downloadHelper.getDownloadState(downloadId);
                Timber.i("Download complete for resource: %s, %s", resource, state);
                try {
                    if (state.getStatus() == DownloadManager.DownloadState.STATUS_SUCCESSFUL) {
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
                    } else {
                        throw new DownloadException(state.getStatusText() + ": " + state.getReasonText());
                    }
                } catch (DownloadException e) {
                    Timber.e(e);
                    resource.setDownloadId(0);
                    resourceRepository.saveResource(resource);
                    EventBus.getDefault()
                            .post(new ResourceDownloadEvent(resource.getKey(), e));
                }

            }

        }
        return Result.SUCCESS;
    }

    public static void scheduleJob(long downloadId) {
        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putLong(ARG_DOWNLOAD_ID, downloadId);
        new JobRequest.Builder(TAG).setExtras(extras)
                                   .startNow()
                                   .build()
                                   .schedule();
    }


    public static class DownloadException extends ReadableException {
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
