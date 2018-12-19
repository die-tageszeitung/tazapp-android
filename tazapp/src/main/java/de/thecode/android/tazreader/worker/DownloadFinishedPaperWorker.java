package de.thecode.android.tazreader.worker;

import android.content.Context;
import android.text.TextUtils;

import com.dd.plist.PropertyListFormatException;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Download;
import de.thecode.android.tazreader.data.DownloadState;
import de.thecode.android.tazreader.data.DownloadsRepository;
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

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class DownloadFinishedPaperWorker extends LoggingWorker {

    private static final String TAG_PREFIX       = BuildConfig.FLAVOR + "_" + "download_finished_paper_job_";
    private static final String ARG_PAPER_BOOKID = "paper_bookid";

    private final PaperRepository paperRepository;
    private final DownloadsRepository downloadsRepository;
    private final StorageManager  storageManager;
    private       UnzipPaper      currentUnzipPaper;

    public DownloadFinishedPaperWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        paperRepository = PaperRepository.getInstance(context);
        storageManager = StorageManager.getInstance(context);
        downloadsRepository = DownloadsRepository.Companion.getInstance(context);
    }

    @NonNull
    @Override
    public Result doBackgroundWork() {
        String bookId = getInputData().getString(ARG_PAPER_BOOKID);
        if (!TextUtils.isEmpty(bookId)) {
            Paper paper = paperRepository.getPaperWithBookId(bookId);
            Download download = paperRepository.getDownloadForPaper(bookId);
            if (paper != null) {
                try {
                    Timber.i("%s", paper);
                    download.setState(DownloadState.EXTRACTING);
                    downloadsRepository.save(download);
                    currentUnzipPaper = new UnzipPaper(paper,
                                                       storageManager.getDownloadFile(paper),
                                                       storageManager.getPaperDirectory(paper),
                                                       true);
                    currentUnzipPaper.getUnzipFile()
                                     .addProgressListener(new UnzipStream.UnzipStreamProgressListener() {
                                         private int        lastEmittedProgress = 0;
                                         private String     bookId;
                                         @Override
                                         public void onProgress(UnzipStream.Progress progress) {
                                             if (lastEmittedProgress != progress.getPercentage()) {
                                                 lastEmittedProgress = progress.getPercentage();
                                                 EventBus.getDefault()
                                                         .post(new UnzipProgressEvent(bookId, progress.getPercentage()));
                                             }
                                         }
                                         private UnzipStream.UnzipStreamProgressListener configure(String bookId) {
                                             this.bookId = bookId;
                                             return this;
                                         }
                                     }.configure(bookId));
                    currentUnzipPaper.start();
                    paper.parseMissingAttributes(false);
                    paperRepository.savePaper(paper);
                    download.setState(DownloadState.READY);
                    downloadsRepository.save(download);

                    NotificationUtils.getInstance(getApplicationContext())
                                     .showDownloadFinishedNotification(paper);
                    EventBus.getDefault()
                            .post(new PaperDownloadFinishedEvent(paper.getBookId()));
                } catch (ParserConfigurationException | IOException | SAXException | ParseException | PropertyListFormatException | UnzipCanceledException e) {
                    paperRepository.savePaper(paper);
                    download.setState(DownloadState.NONE);
                    downloadsRepository.save(download);
                    if (e instanceof UnzipCanceledException) {
                        Timber.w(e);
                    } else {
                        Timber.e(e);
                        //AnalyticsWrapper.getInstance().logException(exception);
                        NotificationUtils.getInstance(getApplicationContext())
                                         .showDownloadErrorNotification(paper, null);
                        //NotificationHelper.showDownloadErrorNotification(getContext(), null, paper.getId());
                        EventBus.getDefault()
                                .post(new PaperDownloadFailedEvent(paper, e));
                    }
                }
            }
        }
        return Result.success();
    }

    private static String getTag(@NonNull String bookId) {
        return TAG_PREFIX + bookId;
    }

    @Override
    public void onStopped() {
        if (currentUnzipPaper != null) currentUnzipPaper.cancel();
    }

    public static void scheduleNow(Paper paper) {
        String tag = getTag(paper.getBookId());

        Data data = new Data.Builder().putString(ARG_PAPER_BOOKID, paper.getBookId())
                                      .build();

        WorkRequest request = new OneTimeWorkRequest.Builder(DownloadFinishedPaperWorker.class).setInputData(data)
                                                                                               .addTag(tag)
                                                                                               .addTag(paper.getBookId())
                                                                                               .build();
        WorkManager.getInstance()
                   .enqueue(request);
    }


}
