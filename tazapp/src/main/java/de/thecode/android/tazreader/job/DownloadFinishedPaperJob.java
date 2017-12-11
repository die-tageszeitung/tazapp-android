package de.thecode.android.tazreader.job;

import android.content.ContentUris;
import android.support.annotation.NonNull;

import com.dd.plist.PropertyListFormatException;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.download.PaperDownloadFailedEvent;
import de.thecode.android.tazreader.download.PaperDownloadFinishedEvent;
import de.thecode.android.tazreader.download.UnzipCanceledException;
import de.thecode.android.tazreader.download.UnzipPaper;
import de.thecode.android.tazreader.download.UnzipProgressEvent;
import de.thecode.android.tazreader.download.UnzipStream;
import de.thecode.android.tazreader.notifications.NotificationUtils;
import de.thecode.android.tazreader.utils.StorageManager;

import org.greenrobot.eventbus.EventBus;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import timber.log.Timber;

/**
 * Created by mate on 11.10.2017.
 */

public class DownloadFinishedPaperJob extends Job implements UnzipStream.UnzipStreamProgressListener {

    public static final  String TAG          = BuildConfig.FLAVOR + "_" + "download_finished_paper_job";
    private static final String ARG_PAPER_ID = "paper_id";

    private long       currentPaperId = -1;
    private UnzipPaper currentUnzipPaper;

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        PersistableBundleCompat extras = params.getExtras();
        long paperId = extras.getLong(ARG_PAPER_ID, -1L);
        Paper paper = Paper.getPaperWithId(getContext(), paperId);
        if (paper != null) {
            try {
                Timber.i("%s", paper);
                StorageManager storageManager = StorageManager.getInstance(getContext());
                currentPaperId = paperId;
                currentUnzipPaper = new UnzipPaper(paper,
                                                   storageManager.getDownloadFile(paper),
                                                   storageManager.getPaperDirectory(paper),
                                                   true);
                currentUnzipPaper.getUnzipFile()
                                 .addProgressListener(this);
                currentUnzipPaper.start();
                savePaper(paper, null);
            } catch (ParserConfigurationException | IOException | SAXException | ParseException | PropertyListFormatException | UnzipCanceledException e) {
                savePaper(paper, e);
            }
        }

        return Result.SUCCESS;
    }

    @Override
    public void onProgress(UnzipStream.Progress progress) {
        EventBus.getDefault()
                .post(new UnzipProgressEvent(currentPaperId, progress.getPercentage()));
    }

    public long getCurrentPaperId() {
        return currentPaperId;
    }

    public void cancelIt() {
        currentUnzipPaper.cancel();
        cancel();
    }

    private void savePaper(Paper paper, Exception exception) {
        paper.setDownloadId(0);

        if (exception == null) {
            paper.parseMissingAttributes(false);
            paper.setHasUpdate(false);
            paper.setDownloaded(true);
        }

        getContext().getContentResolver()
                    .update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), paper.getContentValues(), null, null);

        if (exception == null) {

            //NotificationHelper.showDownloadFinishedNotification(getContext(), paper.getId());

            new NotificationUtils(getContext()).showDownloadFinishedNotification(paper);

            EventBus.getDefault()
                    .post(new PaperDownloadFinishedEvent(paper.getId()));




        } else if (exception instanceof UnzipCanceledException) {
            Timber.w(exception);
        } else {
            Timber.e(exception);
            //AnalyticsWrapper.getInstance().logException(exception);
            new NotificationUtils(getContext()).showDownloadErrorNotification(paper,null);
            //NotificationHelper.showDownloadErrorNotification(getContext(), null, paper.getId());
            EventBus.getDefault()
                    .post(new PaperDownloadFailedEvent(paper.getId(), exception));
        }
    }


    public static void scheduleJob(Paper paper) {
        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putLong(ARG_PAPER_ID, paper.getId());

        new JobRequest.Builder(TAG).setExtras(extras)
                                   .startNow()
                                   .build()
                                   .schedule();
    }

}
