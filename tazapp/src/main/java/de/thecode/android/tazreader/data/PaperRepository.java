package de.thecode.android.tazreader.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.WorkerThread;

import com.squareup.picasso.Picasso;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.download.PaperDeletedEvent;
import de.thecode.android.tazreader.room.AppDatabase;
import de.thecode.android.tazreader.utils.StorageManager;

import org.greenrobot.eventbus.EventBus;

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
    public Paper getPaperWithBookId(String bookId) {
        return appDatabase.paperDao()
                          .getPaper(bookId);
    }

    @WorkerThread
    public void deletePaper(Paper paper) {
        storageManager.deletePaperDir(paper);
        picasso.invalidate(paper.getImage());
        storeRepository.deletePath(Store.getPath(paper.getBookId(), Paper.STORE_KEY_RESOURCE_PARTNER));
        if (paper.isImported() || paper.isKiosk()) {
            appDatabase.paperDao().delete(paper);
        } else {
            paper.setDownloadId(0);
            paper.setDownloaded(false);
            paper.setHasUpdate(false);
            if (BuildConfig.BUILD_TYPE.equals("staging"))
                paper.setValidUntil(0); //Wunsch von Ralf, damit besser im Staging getestet werden kann
            appDatabase.paperDao().insert(paper);
            EventBus.getDefault()
                    .post(new PaperDeletedEvent(paper.getBookId()));
        }
    }

}
