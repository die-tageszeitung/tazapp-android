package de.thecode.android.tazreader.data;

import android.content.Context;
import android.support.annotation.WorkerThread;

import de.thecode.android.tazreader.room.AppDatabase;
import de.thecode.android.tazreader.utils.StorageManager;

import java.util.List;

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

    private final StoreRepository storeRepository;
//    private final ContentResolver contentResolver;
    private final AppDatabase appDatabase;
    private final StorageManager storageManager;

    private ResourceRepository(Context context) {
        storeRepository = StoreRepository.getInstance(context);
        storageManager = StorageManager.getInstance(context);
//        contentResolver = context.getContentResolver();
        appDatabase = AppDatabase.getInstance(context);
    }

    @WorkerThread
    public Resource getResourceForPaper(Paper paper) {
        String resource = storeRepository.getStore(paper.getBookId(), Paper.STORE_KEY_RESOURCE_PARTNER)
                                         .getValue(paper.getResource()); //default value as Fallback
        return getWithKey(resource);
//        if (TextUtils.isEmpty(resource)) resource = getResource(); //Fallback;
//        return Resource.getWithKey(context, resource);
    }



    @WorkerThread
    public Resource getWithKey(String key) {
        return appDatabase.resourceDao().resourceWithKey(key);
    }

    @WorkerThread
    public Resource getWithDownloadId(long downloadId) {
        return appDatabase.resourceDao().resourceWithDownloadId(downloadId);
    }

    @WorkerThread
    public List<Resource> getAllResources(){
        return appDatabase.resourceDao().resources();
    }

    @WorkerThread
    public void saveResource(Resource resource) {
        appDatabase.resourceDao().insert(resource);
    }

    @WorkerThread
    public void deleteResource(Resource resource){
        storageManager.deleteResourceDir(resource.getKey());
        resource.setDownloadId(0);
        resource.setDownloaded(false);
        saveResource(resource);
    }
}
