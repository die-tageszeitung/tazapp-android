package de.thecode.android.tazreader.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import de.thecode.android.tazreader.provider.TazProvider;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class Store {

    public static       String TABLE_NAME        = "STORE";
    public static final Uri    CONTENT_URI       = Uri.parse("content://" + TazProvider.AUTHORITY + "/" + TABLE_NAME);
    public static final String CONTENT_TYPE      = "vnd.android.cursor.dir/vnd.taz." + TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.taz." + TABLE_NAME;

    public static final class Columns implements BaseColumns {

        public static final String KEY   = "key";
        public static final String VALUE = "value";
    }

    private Long   id;
    private String key;
    private String value;

    public Store(String key, String value) {
        if (!key.startsWith("/")) key = "/" + key;
        this.key = key;
        this.value = value;
    }

    public Store(Cursor cursor) {
        this.id = cursor.getLong(cursor.getColumnIndex(Columns._ID));
        this.key = cursor.getString(cursor.getColumnIndex(Columns.KEY));
        this.value = cursor.getString(cursor.getColumnIndex(Columns.VALUE));
    }

    public ContentValues getContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Columns._ID, id);
        cv.put(Columns.KEY, key);
        cv.put(Columns.VALUE, value);
        return cv;
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static Store getStoreForKey(Context context, String key) {
        Cursor cursor = context.getContentResolver()
                               .query(getUriForKey(key), null, null, null, null);
        Store store = null;
        try {
            if (cursor.moveToNext()) store = new Store(cursor);
        } finally {
            cursor.close();
        }
        Timber.d("key %s %s", key, store);
        return store;
    }

    public static String getValueForKey(Context context, String key) {
        Store store = getStoreForKey(context, key);
        if (store != null) return store.getValue();
        return null;
    }

    public static boolean hasKey(Context context, String key) {
        Cursor cursor = context.getContentResolver()
                               .query(getUriForKey(key), null, null, null, null);
        boolean result = false;
        try {
            if (cursor.getCount() > 0) result = true;
        } finally {
            cursor.close();
        }
        return result;
    }

    public static void deleteKey(Context context, String key) {
        int affected = context.getContentResolver()
                              .delete(getUriForKey(key), null, null);
        Timber.d("key %s %d", key, affected);
    }

    public static void deletePath(Context context, String path) {
        int affected = context.getContentResolver()
                              .delete(getUriForKey(path), null, null);
        Timber.d("key %s %d", path, affected);
    }

    public static List<Store> getAllStores(Context context) {
        Cursor cursor = context.getContentResolver()
                               .query(CONTENT_URI, null, null, null, null);
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

    public static boolean saveValueForKey(Context context, String key, String value) {
        boolean result = false;
        Store store = Store.getStoreForKey(context, key);
        if (store == null) {
            store = new Store(key, value);
            Uri resultUri = context.getContentResolver()
                                   .insert(Store.CONTENT_URI, store.getContentValues());
            if (resultUri != null) result = true;
        } else {
            store.setValue(value);
            int affected = context.getContentResolver()
                                  .update(Store.getUriForKey(key), store.getContentValues(), null, null);
            if (affected > 0) result = true;
        }
        Timber.d("key %s %s %s", key, value, result);
        return result;
    }

    public static Uri getUriForKey(String key) {
        Uri.Builder uriBuilder = CONTENT_URI.buildUpon();

        if (key != null) {
            while (key.startsWith("/")) key = key.replaceFirst("/", "");
        }

        uriBuilder.appendEncodedPath(key);

        return uriBuilder.build();
    }
}
