package de.thecode.android.tazreader.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

import com.dd.plist.NSDictionary;

import de.thecode.android.tazreader.utils.PlistHelper;

@Entity(tableName = "PUBLICATION")
public class Publication {

    public static final class PLISTFIELDS {
        public static final String ISSUENAME = "issueName";
        public static final String TYPENAME = "typeName";
        public static final String NAME = "name";
        public static final String URL = "url";
        public static final String IMAGE = "image";
        public static final String CREATED = "created";
        public static final String VALIDUNTIL = "validUntil";
        public static final String APPANDROIDVERSION = "appAndroidVersion";
    }

    @PrimaryKey
    @NonNull
    private String issueName;
    private String typeName;
    private String name;
    private String url;
    private String image;
    private long created;
    private long validUntil;
    private String appAndroidVersion;

    public Publication() {
    }

    public Publication(NSDictionary nsDictionary)
    {
        this.issueName=PlistHelper.getString(nsDictionary,PLISTFIELDS.ISSUENAME);
        this.typeName=PlistHelper.getString(nsDictionary,PLISTFIELDS.TYPENAME);
        this.name=PlistHelper.getString(nsDictionary,PLISTFIELDS.NAME);
        this.url=PlistHelper.getString(nsDictionary,PLISTFIELDS.URL);
        this.image=PlistHelper.getString(nsDictionary,PLISTFIELDS.IMAGE);
        this.created=PlistHelper.getInt(nsDictionary,PLISTFIELDS.CREATED);
        this.validUntil=PlistHelper.getInt(nsDictionary,PLISTFIELDS.VALIDUNTIL);
        this.appAndroidVersion=PlistHelper.getString(nsDictionary,PLISTFIELDS.APPANDROIDVERSION);
    }

    
    public String getIssueName() {
        return issueName;
    }
    
    
    public String getTypeName() {
        return typeName;
    }
    
    
    public String getName() {
        return name;
    }
    
    public long getCreated() {
        return created;
    }
    
    public String getImage() {
        return image;
    }
    
    public String getUrl() {
        return url;
    }
    
    public long getValidUntil() {
        return validUntil;
    }
    
    public long getValidUntilInMillis() {
        return validUntil*1000;
    }


    public void setAppAndroidVersion(String appAndroidVersion) {
        this.appAndroidVersion = appAndroidVersion;
    }

    public String getAppAndroidVersion() {
        return appAndroidVersion;
    }

    public void setCreated(long created) {
        this.created = created;
    }
    
    
    public void setImage(String image) {
        this.image = image;
    }
    
    public void setIssueName(String issueName) {
        this.issueName = issueName;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public void setValidUntil(long validUntil) {
        this.validUntil = validUntil;
    }
    
    
    
}
