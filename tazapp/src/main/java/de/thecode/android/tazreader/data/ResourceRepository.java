package de.thecode.android.tazreader.data;

import android.content.Context;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.TazApplicationKt;
import de.thecode.android.tazreader.room.AppDatabase;
import de.thecode.android.tazreader.utils.StorageManager;

import java.util.List;

import androidx.annotation.WorkerThread;

/**
 * Created by mate on 01.03.18.
 */

public class ResourceRepository {


    private static volatile ResourceRepository mInstance;


    public static ResourceRepository getInstance(Context context) {
        if (mInstance == null) {
            synchronized (ResourceRepository.class) {
                if (mInstance == null) {
                    mInstance = new ResourceRepository(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private final StoreRepository     storeRepository;
    private final DownloadsRepository downloadsRepository;
    private final AppDatabase         appDatabase;
    private final StorageManager      storageManager;

    private ResourceRepository(Context context) {
        storeRepository = StoreRepository.getInstance(context);
        storageManager = StorageManager.getInstance(context);
//        contentResolver = context.getContentResolver();
        appDatabase = AppDatabase.getInstance(context);
        downloadsRepository = DownloadsRepository.Companion.getInstance();
    }

    @WorkerThread
    public ResourceWithDownloadState getResourceForPaper(Paper paper) {
        String resource = storeRepository.getStore(paper.getBookId(), Paper.STORE_KEY_RESOURCE_PARTNER)
                                         .getValue(paper.getResource()); //default value as Fallback
        return getWithKey(resource);
//        if (TextUtils.isEmpty(resource)) resource = getResource(); //Fallback;
//        return Resource.getWithKey(context, resource);
    }

    @WorkerThread
    public ResourceWithDownloadState getWithKey(String key) {
        return appDatabase.resourceDao()
                          .resourceWithKey(key);
    }

    @WorkerThread
    public List<Resource> getAllResources() {
        return appDatabase.resourceDao()
                          .resources();
    }

    @WorkerThread
    public void saveResource(Resource resource) {
        appDatabase.resourceDao()
                   .insert(resource);
    }

    @WorkerThread
    public void deleteResource(Resource resource) {
        downloadsRepository.delete(resource.getKey());
        storageManager.deleteResourceDir(resource.getKey());
    }


    public DownloadState getDownloadState(String key) {
        return getDownload(key).getState();
    }

    public Download getDownload(String key) {
        Download download = downloadsRepository.get(key);
        if (download == null) {
            download = Download.Companion.create(DownloadType.RESOURCE,
                                                 key,
                                                 TazApplicationKt.getRes()
                                                                 .getString(R.string.download_title_resource),
                                                 TazApplicationKt.getStorageManager()
                                                                 .getDownloadFile(key + ".res.zip"));
        }
        return download;
    }
}
