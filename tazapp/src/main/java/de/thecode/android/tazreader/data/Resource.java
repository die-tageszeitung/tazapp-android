package de.thecode.android.tazreader.data;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.support.annotation.NonNull;

import com.dd.plist.NSDictionary;

import de.thecode.android.tazreader.sync.model.Plist;
import de.thecode.android.tazreader.utils.PlistHelper;
import de.thecode.android.tazreader.utils.ReadableException;
import de.thecode.android.tazreader.utils.StorageManager;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity(tableName = "RESOURCE")
public class Resource {

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

    public Resource(NSDictionary nsDictionary) {
        this.key = PlistHelper.getString(nsDictionary, Plist.Fields.RESOURCE);
        this.fileHash = PlistHelper.getString(nsDictionary, PLISTFIELDS.RESOURCEFILEHASH);
        this.url = PlistHelper.getString(nsDictionary, PLISTFIELDS.RESOURCEURL);
        this.len = PlistHelper.getInt(nsDictionary, PLISTFIELDS.RESOURCELEN);
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
        return downloadId != 0;
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
