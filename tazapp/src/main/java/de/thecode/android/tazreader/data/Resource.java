package de.thecode.android.tazreader.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.apache.commons.lang3.builder.ToStringBuilder;

import de.thecode.android.tazreader.provider.TazProvider;


public class Resource {
    public static String TABLE_NAME = "RESOURCE";
    public static final Uri CONTENT_URI = Uri.parse("content://" + TazProvider.AUTHORITY + "/" + TABLE_NAME);
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.taz." + TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.taz." + TABLE_NAME;

    public static final class Columns {
        public static final String KEY = "key";
        public static final String DOWNLOADID = "downloadID";
        public static final String DOWNLOADED = "downloaded";
        public static final String FILEHASH = "fileHash";
        public static final String LEN = "len";
        public static final String URL = "url";
    }

    private String key;
    private long downloadId;
    private boolean downloaded;
    private String url;
    private String fileHash;
    private long len;


    public Resource(Cursor cursor) {
        setData(cursor);
    }

    public Resource(Context context, String key) {
        Cursor cursor = context.getContentResolver()
                               .query(Uri.withAppendedPath(CONTENT_URI, key), null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                setData(cursor);
            }
        } finally {
            cursor.close();
        }

    }

    private void setData(Cursor cursor) {
        this.key = cursor.getString(cursor.getColumnIndex(Columns.KEY));
        this.downloadId = cursor.getLong(cursor.getColumnIndex(Columns.DOWNLOADID));
        this.downloaded = getBoolean(cursor, cursor.getColumnIndex(Columns.DOWNLOADED));
        this.len = cursor.getLong(cursor.getColumnIndex(Columns.LEN));
        this.fileHash = cursor.getString(cursor.getColumnIndex(Columns.FILEHASH));
        this.url = cursor.getString(cursor.getColumnIndex(Columns.URL));
    }

    public ContentValues getContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Columns.KEY, key);
        cv.put(Columns.DOWNLOADID, downloadId);
        cv.put(Columns.DOWNLOADED, downloaded);
        cv.put(Columns.FILEHASH, fileHash);
        cv.put(Columns.LEN, len);
        cv.put(Columns.URL, url);
        return cv;
    }

    private boolean getBoolean(Cursor cursor, int columnIndex) {
        if (cursor.isNull(columnIndex) || cursor.getShort(columnIndex) == 0) {
            return false;
        } else {
            return true;
        }
    }


    public void setKey(String key) {
        this.key = key;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setLen(long len) {
        this.len = len;
    }

    public String getKey() {
        return key;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public boolean isDownloading() {
        if (downloadId == 0) return false;
        else return true;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public String getUrl() {
        return url;
    }

    public String getFileHash() {
        return fileHash;
    }

    public long getLen() {
        return len;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
