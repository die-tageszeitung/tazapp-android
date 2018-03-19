package de.thecode.android.tazreader.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

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

    private ContentResolver contentResolver;

    private StoreRepository(Context context) {
        this.contentResolver = context.getContentResolver();
    }

    public Store getStore(String bookId, String key){
        return getStoreForPath(Store.getPath(bookId,key));
    }


    public Store getStoreForPath(String path) {
        Cursor cursor = contentResolver
                .query(getUriForKey(path), null, null, null, null);
        Store store = new Store(path,null);
        try {
            if (cursor.moveToNext()) store = new Store(cursor);
        } finally {
            cursor.close();
        }
        Timber.d("path %s %s", path, store);
        return store;
    }

    public void deletePath(String path) {
        int affected = contentResolver
                .delete(getUriForKey(path), null, null);
        Timber.d("key %s %d", path, affected);
    }

    public void deleteStore(Store store) {
        deletePath(store.getPath());
    }

    public List<Store> getAllStores() {
        Cursor cursor = contentResolver
                .query(Store.CONTENT_URI, null, null, null, null);
        List<Store> result = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                result.add(new Store(cursor));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public boolean saveStore(Store store) {
        return contentResolver.insert(Store.CONTENT_URI, store.getContentValues()) != null;
    }

    private Uri getUriForKey(String key) {
        Uri.Builder uriBuilder = Store.CONTENT_URI.buildUpon();

        if (key != null) {
            while (key.startsWith("/")) key = key.replaceFirst("/", "");
        }

        uriBuilder.appendEncodedPath(key);

        return uriBuilder.build();
    }

}
