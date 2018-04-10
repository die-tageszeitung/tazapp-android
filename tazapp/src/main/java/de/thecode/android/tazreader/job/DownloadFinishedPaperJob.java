package de.thecode.android.tazreader.job;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.dd.plist.PropertyListFormatException;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
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

    public static final  String TAG              = BuildConfig.FLAVOR + "_" + "download_finished_paper_job";
    private static final String ARG_PAPER_BOOKID = "paper_bookid";
    //    private static final String ARG_PAPER_ID = "paper_id";
//
//    private long       currentPaperId = -1;
    private String currentPaperBookId;

    private UnzipPaper currentUnzipPaper;
    private int lastEmittedProgress = 0;

    private PaperRepository paperRepository;

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        PersistableBundleCompat extras = params.getExtras();
        String bookId = extras.getString(ARG_PAPER_BOOKID, null);
        if (!TextUtils.isEmpty(bookId)) {
            paperRepository = PaperRepository.getInstance(getContext());
            Paper paper = paperRepository.getPaperWithBookId(bookId);
            if (paper != null) {
                try {
                    Timber.i("%s", paper);
                    StorageManager storageManager = StorageManager.getInstance(getContext());
                    currentPaperBookId = bookId;
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
        }
        return Result.SUCCESS;
    }

    @Override
    public void onProgress(UnzipStream.Progress progress) {
        if (lastEmittedProgress != progress.getPercentage()) {
            lastEmittedProgress = progress.getPercentage();
            EventBus.getDefault()
                    .post(new UnzipProgressEvent(currentPaperBookId, progress.getPercentage()));
        }
    }

    public String getCurrentPaperBookId() {
        return currentPaperBookId;
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

        paperRepository.savePaper(paper);
        if (exception == null) {

            //NotificationHelper.showDownloadFinishedNotification(getContext(), paper.getId());

            new NotificationUtils(getContext()).showDownloadFinishedNotification(paper);

            EventBus.getDefault()
                    .post(new PaperDownloadFinishedEvent(paper.getBookId()));


        } else if (exception instanceof UnzipCanceledException) {
            Timber.w(exception);
        } else {
            Timber.e(exception);
            //AnalyticsWrapper.getInstance().logException(exception);
            new NotificationUtils(getContext()).showDownloadErrorNotification(paper, null);
            //NotificationHelper.showDownloadErrorNotification(getContext(), null, paper.getId());
            EventBus.getDefault()
                    .post(new PaperDownloadFailedEvent(paper, exception));
        }
    }


    public static void scheduleJob(Paper paper) {
        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putString(ARG_PAPER_BOOKID, paper.getBookId());

        new JobRequest.Builder(TAG).setExtras(extras)
                                   .startNow()
                                   .build()
                                   .schedule();
    }

}
