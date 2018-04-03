package de.thecode.android.tazreader.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import de.thecode.android.tazreader.provider.TazProvider;

@Entity(tableName = "STORE")
public class Store {

    public static       String TABLE_NAME        = "STORE";
    public static final Uri    CONTENT_URI       = Uri.parse("content://" + TazProvider.AUTHORITY + "/" + TABLE_NAME);
    public static final String CONTENT_TYPE      = "vnd.android.cursor.dir/vnd.taz." + TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.taz." + TABLE_NAME;

    public static final class Columns {

        public static final String PATH  = "path";
        public static final String VALUE = "value";
    }

    @PrimaryKey
    @NonNull
    private String path;
    private String value;

    public Store(String path, String value) {
        if (!path.startsWith("/")) path = "/" + path;
        this.path = path;
        this.value = value;
    }

    public Store(Cursor cursor) {
        this.path = cursor.getString(cursor.getColumnIndex(Columns.PATH));
        this.value = cursor.getString(cursor.getColumnIndex(Columns.VALUE));
    }

    public ContentValues getContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Columns.PATH, path);
        cv.put(Columns.VALUE, value);
        return cv;
    }

    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }

    public String getValue(String defaultValue) {
        return TextUtils.isEmpty(value) ? defaultValue : value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean hasValue() {
        return TextUtils.isEmpty(getValue());
    }

    public static String getPath(String bookId, String key) {
        return bookId + "/" + key;
    }

}
