package de.thecode.android.tazreader.data;

import androidx.lifecycle.LiveData;
import android.content.Context;

import androidx.annotation.WorkerThread;

import de.thecode.android.tazreader.room.AppDatabase;

import java.util.Collections;
import java.util.List;

/**
 * Created by mate on 01.03.18.
 */

public class StoreRepository {

    private static volatile StoreRepository mInstance;

    public static StoreRepository getInstance(Context context) {
        if (mInstance == null) {
            synchronized (StoreRepository.class) {
                if (mInstance == null) {
                    mInstance = new StoreRepository(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private final AppDatabase appDatabase;

    private StoreRepository(Context context) {
        appDatabase = AppDatabase.getInstance(context);
    }

    @WorkerThread
    public Store getStore(String bookId, String key){
        return getStoreForPath(Store.getPath(bookId,key));
    }

    @WorkerThread
    public Store getStoreForPath(String path) {
        Store result = appDatabase.storeDao().withPath(path);
        if (result == null) result = new Store(path,null);
        return result;
    }

    public LiveData<Store> getLiveStore(String bookId, String key){
        return getLiveStoreForPath(Store.getPath(bookId,key));
    }

    public LiveData<Store> getLiveStoreForPath(String path) {
        return appDatabase.storeDao().liveWithPath(path);
    }

    public LiveData<List<Store>> getLiveAllStoresForBook(String bookId) {
        return appDatabase.storeDao().liveAllForBookId(bookId);
    }



    @WorkerThread
    public void deletePath(String path) {
        appDatabase.storeDao().deleteWithPath(path);
    }

    @WorkerThread
    public void deleteStore(Store store) {
        appDatabase.storeDao().delete(store);
    }

    @WorkerThread
    public List<Store> getAllStores() {
        List<Store> stores = appDatabase.storeDao().getAll();
        if ( stores == null) stores = Collections.emptyList();
        return stores;
    }

    @WorkerThread
    public List<Store> getAllStoresForBook(String bookId) {
        List<Store> stores = appDatabase.storeDao().getAllForBookId(bookId);
        if ( stores == null) stores = Collections.emptyList();
        return stores;
    }

    @WorkerThread
    public void saveStore(Store store) {
        appDatabase.storeDao().insert(store);
    }


}
