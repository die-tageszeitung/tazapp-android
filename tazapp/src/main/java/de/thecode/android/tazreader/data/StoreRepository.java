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


    public Store getStoreForKey(String key) {
        Cursor cursor = contentResolver
                .query(getUriForKey(key), null, null, null, null);
        Store store = new Store(key,null);
        try {
            if (cursor.moveToNext()) store = new Store(cursor);
        } finally {
            cursor.close();
        }
        Timber.d("key %s %s", key, store);
        return store;
    }

//    public String getValueForKey(String key) {
//        Store store = getStoreForKey(key);
//        if (store != null) return store.getValue();
//        return null;
//    }

//    public boolean hasKey(String key) {
//        Cursor cursor = contentResolver
//                .query(getUriForKey(key), null, null, null, null);
//        boolean result = false;
//        try {
//            if (cursor.getCount() > 0) result = true;
//        } finally {
//            cursor.close();
//        }
//        return result;
//    }

    public void deleteKey(String key) {
        int affected = contentResolver
                .delete(getUriForKey(key), null, null);
        Timber.d("key %s %d", key, affected);
    }

    public void deletePath(String path) {
        int affected = contentResolver
                .delete(getUriForKey(path), null, null);
        Timber.d("key %s %d", path, affected);
    }

    public void deleteStore(Store store) {
        deleteKey(store.getKey());
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
//
//    public boolean saveValueForKey(String key, String value) {
//        boolean result = false;
//        Store store = getStoreForKey(key);
//        if (store == null) {
//            store = new Store(key, value);
//            Uri resultUri = contentResolver
//                    .insert(Store.CONTENT_URI, store.getContentValues());
//            if (resultUri != null) result = true;
//        } else {
//            store.setValue(value);
//            int affected = contentResolver
//                    .update(Store.getUriForKey(key), store.getContentValues(), null, null);
//            if (affected > 0) result = true;
//        }
//        Timber.d("key %s %s %s", key, value, result);
//        return result;
//    }
//
    private Uri getUriForKey(String key) {
        Uri.Builder uriBuilder = Store.CONTENT_URI.buildUpon();

        if (key != null) {
            while (key.startsWith("/")) key = key.replaceFirst("/", "");
        }

        uriBuilder.appendEncodedPath(key);

        return uriBuilder.build();
    }

}
