package de.thecode.android.tazreader.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.dd.plist.NSDictionary;

import de.thecode.android.tazreader.provider.TazProvider;
import de.thecode.android.tazreader.utils.PlistHelper;
import de.thecode.android.tazreader.utils.ReadableException;
import de.thecode.android.tazreader.utils.StorageManager;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "RESOURCE")
public class Resource {
    public static       String TABLE_NAME        = "RESOURCE";
    public static final Uri    CONTENT_URI       = Uri.parse("content://" + TazProvider.AUTHORITY + "/" + TABLE_NAME);
    public static final String CONTENT_TYPE      = "vnd.android.cursor.dir/vnd.taz." + TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.taz." + TABLE_NAME;

    public static final class Columns {
        public static final String KEY        = "key";
        public static final String DOWNLOADID = "downloadId";
        public static final String DOWNLOADED = "downloaded";
        public static final String FILEHASH   = "fileHash";
        public static final String LEN        = "len";
        public static final String URL        = "url";
    }

    public static final class PLISTFIELDS {
        public static final String RESOURCEFILEHASH = "resourceFileHash";
        public static final String RESOURCEURL      = "resourceUrl";
        public static final String RESOURCELEN      = "resourceLen";
    }


    @PrimaryKey
    @NonNull
    private String  key;
    private long    downloadId;
    private boolean downloaded;
    private String  url;
    private String  fileHash;
    private long    len;

    public Resource() {
    }

    public Resource(Cursor cursor) {
        setData(cursor);
    }

    public Resource(NSDictionary nsDictionary) {
        this.key = PlistHelper.getString(nsDictionary, Paper.Columns.RESOURCE);
        this.fileHash = PlistHelper.getString(nsDictionary, PLISTFIELDS.RESOURCEFILEHASH);
        this.url = PlistHelper.getString(nsDictionary, PLISTFIELDS.RESOURCEURL);
        this.len = PlistHelper.getInt(nsDictionary, PLISTFIELDS.RESOURCELEN);
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

    public void delete(Context context){
        StorageManager storage = StorageManager.getInstance(context);
        storage.deleteResourceDir(getKey());
        setDownloadId(0);
        setDownloaded(false);
//        Uri resourceUri = CONTENT_URI.buildUpon()
//                                     .appendPath(getKey())
//                                     .build();
//        int affected = context.getContentResolver()
//                              .update(resourceUri, getContentValues(), null, null);
        context.getContentResolver().insert(CONTENT_URI,getContentValues());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Resource)) return false;

        Resource resource = (Resource) o;

        return new EqualsBuilder().append(downloadId, resource.downloadId)
                                  .append(downloaded, resource.downloaded)
                                  .append(len, resource.len)
                                  .append(key, resource.key)
                                  .append(url, resource.url)
                                  .append(fileHash, resource.fileHash)
                                  .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(key)
                                          .append(downloadId)
                                          .append(downloaded)
                                          .append(url)
                                          .append(fileHash)
                                          .append(len)
                                          .toHashCode();
    }

    public static class MissingResourceException extends ReadableException {

    }
}
