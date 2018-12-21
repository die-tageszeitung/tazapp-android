package de.thecode.android.tazreader.data;

import com.dd.plist.NSDictionary;

import de.thecode.android.tazreader.sync.model.Plist;
import de.thecode.android.tazreader.utils.PlistHelper;
import de.thecode.android.tazreader.utils.ReadableException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "RESOURCE")
public class Resource extends Downloadable {

    public static final class PLISTFIELDS {
        public static final String RESOURCEFILEHASH = "resourceFileHash";
        public static final String RESOURCEURL      = "resourceUrl";
        public static final String RESOURCELEN      = "resourceLen";
    }



    @PrimaryKey
    @NonNull
    private String key;
    private String url;

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


    public void setUrl(String url) {
        this.url = url;
    }


    public String getKey() {
        return key;
    }

    public String getUrl() {
        return url;
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

        return new EqualsBuilder().appendSuper(super.equals(o))
                                  .append(key, resource.key)
                                  .append(url, resource.url)
                                  .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode())
                                          .append(key)
                                          .append(url)
                                          .toHashCode();
    }

    public static class MissingResourceException extends ReadableException {

    }
}
