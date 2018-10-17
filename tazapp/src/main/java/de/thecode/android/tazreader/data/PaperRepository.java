package de.thecode.android.tazreader.data;

import androidx.lifecycle.LiveData;
import android.content.Context;
import androidx.annotation.WorkerThread;

import com.squareup.picasso.Picasso;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.download.PaperDeletedEvent;
import de.thecode.android.tazreader.room.AppDatabase;
import de.thecode.android.tazreader.utils.StorageManager;

import org.greenrobot.eventbus.EventBus;

import java.util.Collections;
import java.util.List;

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

    private final AppDatabase     appDatabase;
    private final StorageManager  storageManager;
    private final Picasso         picasso;
    private final StoreRepository storeRepository;

    private PaperRepository(Context context) {
        appDatabase = AppDatabase.getInstance(context);
        storageManager = StorageManager.getInstance(context);
        picasso = Picasso.with(context);
        storeRepository = StoreRepository.getInstance(context);
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
    public Paper getPaperWithDownloadId(long downloadId) {
        return appDatabase.paperDao()
                          .getPaperWithDownloadId(downloadId);
    }

    @WorkerThread
    public void deletePaper(Paper paper) {
        WorkManager.getInstance().cancelAllWorkByTag(paper.getBookId());
        storageManager.deletePaperDir(paper);
        picasso.invalidate(paper.getImage());
        storeRepository.deletePath(Store.getPath(paper.getBookId(), Paper.STORE_KEY_RESOURCE_PARTNER));
//        if (paper.isImported() || paper.isKiosk()) {
//            appDatabase.paperDao()
//                       .delete(paper);
//        } else {
        paper.setDownloadId(0);
        paper.setState(Paper.STATE_NONE);
//            paper.setDownloaded(false);
//            paper.setHasUpdate(false);
        if (BuildConfig.BUILD_TYPE.equals("staging"))
            paper.setValidUntil(0); //Wunsch von Ralf, damit besser im Staging getestet werden kann
        savePaper(paper);
        EventBus.getDefault()
                .post(new PaperDeletedEvent(paper.getBookId()));
//        }
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


    public LiveData<List<Paper>> getLivePapersForDemoLibrary() {
        return appDatabase.paperDao()
                          .getLivePapersForDemoLibrary();
    }

    public LiveData<List<Paper>> getLivePapersForLibrary() {

        return appDatabase.paperDao()
                          .getLivePapersForLibrary();
    }
}
