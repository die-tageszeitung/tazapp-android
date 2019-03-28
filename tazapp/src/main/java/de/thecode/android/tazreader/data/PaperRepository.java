package de.thecode.android.tazreader.data;

import android.content.Context;

import com.squareup.picasso.Picasso;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.TazApplicationKt;
import de.thecode.android.tazreader.download.PaperDeletedEvent;
import de.thecode.android.tazreader.room.AppDatabase;
import de.thecode.android.tazreader.utils.StorageManager;

import org.greenrobot.eventbus.EventBus;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.work.WorkManager;

/**
 * Created by mate on 02.03.18.
 */

public class PaperRepository {


    private static volatile PaperRepository mInstance;

    public static PaperRepository getInstance(Context context) {
        if (mInstance == null) {
            synchronized (PaperRepository.class) {
                if (mInstance == null) {
                    mInstance = new PaperRepository(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private final AppDatabase         appDatabase;
    private final StorageManager      storageManager;
    private final Picasso             picasso;
    private final StoreRepository     storeRepository;
    private final DownloadsRepository downloadsRepository;

    private PaperRepository(Context context) {
        appDatabase = AppDatabase.getInstance(context);
        storageManager = StorageManager.getInstance(context);
        picasso = Picasso.with(context);
        storeRepository = StoreRepository.getInstance(context);
        downloadsRepository = DownloadsRepository.Companion.getInstance();
    }

    @WorkerThread
    public List<Paper> getAllPapers() {
        return appDatabase.paperDao()
                          .getAllPapers();
    }

    @WorkerThread
    public Paper getPaperWithBookId(String bookId) {
        return appDatabase.paperDao()
                          .getPaper(bookId);
    }

    @WorkerThread
    public PaperWithDownloadState getWithBookId(String bookId) {
        return appDatabase.paperDao()
                          .get(bookId);
    }

    @WorkerThread
    public List<Paper> getPapersWithBookId(List<String> bookIds) {
        if (bookIds == null) bookIds = Collections.emptyList();
        return getPapersWithBookId(bookIds.toArray(new String[bookIds.size()]));
    }

    @WorkerThread
    public List<Paper> getPapersWithBookId(String... bookIds) {
        return appDatabase.paperDao()
                          .getPapers(bookIds);
    }

    @WorkerThread
    public void deletePaper(Paper paper) {
        Download download = downloadsRepository.get(paper.getBookId());
        if (download != null) {
            if (download.getWorkerUuid() != null) WorkManager.getInstance().cancelWorkById(download.getWorkerUuid());
            downloadsRepository.delete(download);
        }
        storageManager.deletePaperDir(paper);
        picasso.invalidate(paper.getImage());
        storeRepository.deletePath(Store.getPath(paper.getBookId(), Paper.STORE_KEY_RESOURCE_PARTNER));
        if (BuildConfig.BUILD_TYPE.equals("staging")) {
            paper.setValidUntil(0); //Wunsch von Ralf, damit besser im Staging getestet werden kann
            savePaper(paper);
        }
        EventBus.getDefault()
                .post(new PaperDeletedEvent(paper.getBookId()));
    }

    @WorkerThread
    public void savePaper(Paper paper) {
        appDatabase.paperDao()
                   .insert(paper);
    }

    @WorkerThread
    public void savePapers(List<Paper> papers) {
        appDatabase.paperDao()
                   .insert(papers);
    }

    @WorkerThread
    public Paper getLatestPaper() {
        return appDatabase.paperDao()
                          .getLatestPaper();
    }


    public LiveData<List<PaperWithDownloadState>> getLivePapersForDemoLibrary() {
        return appDatabase.paperDao()
                          .getLiveForDemoLibrary();
    }

    public LiveData<List<PaperWithDownloadState>> getLivePapersForLibrary() {

        return appDatabase.paperDao()
                          .getLiveForLibrary();
    }

    public LiveData<PaperWithDownloadState> getLivePaper(String bookId) {
        return appDatabase.paperDao().getPaperLiveData(bookId);
    }

    @WorkerThread
    public DownloadState getDownloadStateForPaper(String bookId) {
        return getDownloadForPaper(bookId).getState();
    }

    @WorkerThread
    public Download getDownloadForPaper(String bookId) {
        Download download = downloadsRepository.get(bookId);
        if (download == null) {
            String title = bookId;
            Paper paper = getPaperWithBookId(bookId);
            if (paper != null) {
                try {
                    title = (TazApplicationKt.getRes()
                                             .getString(R.string.download_title_paper,
                                                        paper.getTitle(),
                                                        paper.getDate(DateFormat.MEDIUM)));
                } catch (ParseException e) {
                    title = paper.getTitle();
                }
            }
            download = Download.Companion.create(DownloadType.PAPER,
                                                 bookId,
                                                 title,
                                                 TazApplicationKt.getStorageManager()
                                                                 .getDownloadFile(bookId + ".paper.zip"));

        }
        return download;
    }
}
