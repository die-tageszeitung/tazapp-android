package de.thecode.android.tazreader.download;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Intent;

import com.dd.plist.PropertyListFormatException;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.analytics.AnalyticsWrapper;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.StorageManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by mate on 07.08.2015.
 */
public class DownloadFinishedPaperService extends IntentService implements UnzipStream.UnzipStreamProgressListener {

    private static final Logger log = LoggerFactory.getLogger(DownloadFinishedPaperService.class);

    public static final String PARAM_PAPER_ID = "paperId";
    public static final String PARAM_CANCEL_BOOL = "cancel";

    private static volatile List<Long> canceledPapersIds = new ArrayList<>();


    public DownloadFinishedPaperService() {
        super(DownloadFinishedPaperService.class.getSimpleName());
    }

    private long currentPaperId;
    private UnzipPaper currentUnzipPaper;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.debug("{} {} {}", intent, flags, startId);
        if (intent.getBooleanExtra(PARAM_CANCEL_BOOL, false)) {
            long paperId = intent.getLongExtra(PARAM_PAPER_ID, -1);
            if (paperId != -1) {
                if (currentUnzipPaper != null && currentUnzipPaper.getPaper()
                                                                  .getId() == paperId) {
                    currentUnzipPaper.cancel();
                } else if (!canceledPapersIds.contains(paperId)) canceledPapersIds.add(paperId);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getBooleanExtra(PARAM_CANCEL_BOOL, false)) return;
        long paperId = intent.getLongExtra(PARAM_PAPER_ID, -1);
        log.trace("Start service after download for paper: {}", paperId);
        if (paperId != -1) try {
            Paper paper = new Paper(this, paperId);
            if (canceledPapersIds.contains(paperId)) {
                canceledPapersIds.remove(paperId);
                paper.delete(this);
                return;
            }
            try {
                log.trace("{}", paper);
                StorageManager storageManager = StorageManager.getInstance(this);
                currentPaperId = paperId;
                currentUnzipPaper = new UnzipPaper(paper, storageManager.getDownloadFile(paper), storageManager.getPaperDirectory(paper), true);
                currentUnzipPaper.getUnzipFile()
                                 .addProgressListener(this);
                currentUnzipPaper.start();
                savePaper(paper, null);
            } catch (ParserConfigurationException | IOException | SAXException | ParseException | PropertyListFormatException | UnzipCanceledException e) {
                savePaper(paper, e);
            }
        } catch (Paper.PaperNotFoundException e) {
            log.error("", e);
        }
        log.trace("Finished service after download for paper: {}", paperId);
    }

    @Override
    public void onProgress(UnzipStream.Progress progress) {
        EventBus.getDefault()
                .post(new UnzipProgressEvent(currentPaperId, progress.getPercentage()));
    }


    public void savePaper(Paper paper, Exception exception) {
        paper.setDownloadId(0);

        if (exception == null) {
            paper.parseMissingAttributes(false);
            paper.setHasupdate(false);
            paper.setDownloaded(true);
        }

        getContentResolver().update(ContentUris.withAppendedId(Paper.CONTENT_URI, paper.getId()), paper.getContentValues(), null, null);

        if (exception == null) {
            NotificationHelper.showDownloadFinishedNotification(this, paper.getId());
            EventBus.getDefault()
                    .post(new PaperDownloadFinishedEvent(paper.getId()));

        } else if (exception instanceof UnzipCanceledException) {
            log.warn("", exception);
        } else {
            log.error("",exception);
            AnalyticsWrapper.getInstance().logException(exception);
            NotificationHelper.showDownloadErrorNotification(this, null, paper.getId());
            EventBus.getDefault()
                    .post(new PaperDownloadFailedEvent(paper.getId(), exception));
        }
    }
}
