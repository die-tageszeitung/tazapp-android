package de.thecode.android.tazreader.download;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.Intent;

import com.dd.plist.PropertyListFormatException;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.utils.Log;
import de.thecode.android.tazreader.utils.StorageManager;

/**
 * Created by mate on 07.08.2015.
 */
public class DownloadFinishedPaperService extends IntentService implements UnzipStream.UnzipStreamProgressListener {

    public static final String PARAM_PAPER_ID = "paperId";

    public DownloadFinishedPaperService() {
        super(DownloadFinishedPaperService.class.getSimpleName());
    }

    private long currentPaperId;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(intent, flags, startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long paperId = intent.getLongExtra(PARAM_PAPER_ID, -1);
        Log.t("Start service after download for paper:", paperId);
        if (paperId != -1) try {
            Paper paper = new Paper(this, paperId);
            try {
                Log.t(paper);
                StorageManager storageManager = StorageManager.getInstance(this);

                currentPaperId = paperId;

                UnzipPaper unzipPaper = new UnzipPaper(paper, storageManager.getDownloadFile(paper), storageManager.getPaperDirectory(paper), true);
                unzipPaper.getUnzipFile()
                          .addProgressListener(this);
                unzipPaper.start();
                savePaper(paper, null);
            } catch (ParserConfigurationException | IOException | SAXException | ParseException | PropertyListFormatException | UnzipStream.UnzipCanceledException e) {
                savePaper(paper, e);
            }
        } catch (Paper.PaperNotFoundException e) {
            Log.e(e);
        }
        Log.t("Finished service after download for paper:", paperId);
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
        } else {
            Log.e(exception);
            Log.sendExceptionWithCrashlytics(exception);
            NotificationHelper.showDownloadErrorNotification(this, paper.getId());
            EventBus.getDefault()
                    .post(new PaperDownloadFailedEvent(paper.getId(), exception));
        }
    }
}
