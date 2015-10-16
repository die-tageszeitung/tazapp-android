package de.thecode.android.tazreader.download;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileNotFoundException;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.Log;

/**
 * Created by mate on 12.02.2015.
 */
public class FinishPaperDownloadService extends IntentService {

    public static final String PARAM_PAPER_ID = "paperId";
    //    private static final int notificationId = 1;

    public FinishPaperDownloadService() {
        super(FinishPaperDownloadService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long paperId = intent.getLongExtra(PARAM_PAPER_ID, -1);
        Log.v(paperId);
        if (paperId != -1) {
            final Paper paper;
            try {
                paper = new Paper(getApplicationContext(), paperId);
                Log.t("Start service for downloaded paper:",paper);
                StorageManager storage = StorageManager.getInstance(getApplicationContext());


                new TPaperFileUnzipTask(paper, storage.getDownloadFile(paper), storage.getPaperDirectory(paper), true, true) {

                    Context context;

                    @Override
                    public File doInBackgroundWithException(Object... params) throws Exception {
                        Log.t("... start working in background for paper",getPaper());
                        context = (Context) params[0];
                        return super.doInBackgroundWithException(params);
                    }

                    @Override
                    public void onPostError(Exception exception, File sourceZipFile) {
                        Log.e(exception);
                        Log.sendExceptionWithCrashlytics(exception);
                        savePaper(false);
                        NotificationHelper.showDownloadErrorNotification(context, getPaper().getId());
                        EventBus.getDefault()
                                .post(new PaperDownloadFailedEvent(getPaper().getId(),exception));
                    }

                    @Override
                    protected void onPostSuccess(File file) {
                        super.onPostSuccess(file);
                        savePaper(true);
                        Log.t("Finished task after download for paper:",getPaper());
                        NotificationHelper.showDownloadFinishedNotification(context, getPaper().getId());
                        EventBus.getDefault()
                                .post(new PaperDownloadFinishedEvent(getPaper().getId()));
                    }

                    @Override
                    protected void onProgressUpdate(Progress... values) {
                        EventBus.getDefault().post(new UnzipProgressEvent(getPaper().getId(),values[0].getPercentage()));
                    }

                    private void savePaper(boolean success) {
                        getPaper().setDownloadId(0);

                        if (success) {
                            getPaper().parseMissingAttributes(false);
                            getPaper().setHasupdate(false);
                            getPaper().setDownloaded(true);
                        }

                        context.getContentResolver()
                                    .update(ContentUris.withAppendedId(Paper.CONTENT_URI, getPaper().getId()), getPaper().getContentValues(), null, null);
                    }
                }.execute(getApplicationContext());

            } catch (Paper.PaperNotFoundException | FileNotFoundException e) {
                Log.e(e);
            }

        }
    }
}
