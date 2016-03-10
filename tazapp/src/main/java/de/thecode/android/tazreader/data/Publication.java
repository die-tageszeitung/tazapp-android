package de.thecode.android.tazreader.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.dd.plist.NSDictionary;

import de.thecode.android.tazreader.provider.TazProvider;
import de.thecode.android.tazreader.utils.PlistHelper;


public class Publication {

    public static String TABLE_NAME = "PUBLICATION";
    public static final Uri CONTENT_URI = Uri.parse("content://" + TazProvider.AUTHORITY + "/" + TABLE_NAME);
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.taz."+TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.taz."+TABLE_NAME;

    public static final class Columns implements BaseColumns {
        public static final String ISSUENAME = "issueName";
        public static final String TYPENAME = "typeName";
        public static final String NAME = "name";
        public static final String URL = "url";
        public static final String IMAGE = "image";
        public static final String CREATED = "created";
        public static final String VALIDUNTIL = "validUntil";
        
        public static final String FULL_ID=TABLE_NAME+"."+_ID;
        //public static final String FULL_VALIDUNTIL = TABLE_NAME+"."+VALIDUNTIL;
    }
    
    private Long id;
    private String issueName;
    private String typeName;
    private String name;
    private String url;
    private String image;
    private long created;
    private long validUntil;
    
    public Publication(Cursor cursor)
    {
        this.id=cursor.getLong(cursor.getColumnIndex(Columns._ID));
        this.issueName=cursor.getString(cursor.getColumnIndex(Columns.ISSUENAME));
        this.typeName=cursor.getString(cursor.getColumnIndex(Columns.TYPENAME));
        this.name=cursor.getString(cursor.getColumnIndex(Columns.NAME));
        this.url=cursor.getString(cursor.getColumnIndex(Columns.URL));
        this.image=cursor.getString(cursor.getColumnIndex(Columns.IMAGE));
        this.created=cursor.getLong(cursor.getColumnIndex(Columns.CREATED));
        this.validUntil=cursor.getLong(cursor.getColumnIndex(Columns.VALIDUNTIL));
    }
    
    public Publication(NSDictionary nsDictionary)
    {
        this.issueName=PlistHelper.getString(nsDictionary,Columns.ISSUENAME);
        this.typeName=PlistHelper.getString(nsDictionary,Columns.TYPENAME);
        this.name=PlistHelper.getString(nsDictionary,Columns.NAME);
        this.url=PlistHelper.getString(nsDictionary,Columns.URL);
        this.image=PlistHelper.getString(nsDictionary,Columns.IMAGE);
        this.created=PlistHelper.getInt(nsDictionary,Columns.CREATED);
        this.validUntil=PlistHelper.getInt(nsDictionary,Columns.VALIDUNTIL);
    }
    
    public ContentValues getContentValues()
    {
        ContentValues cv = new ContentValues();
        cv.put(Columns._ID, id);
        cv.put(Columns.CREATED, created);
        cv.put(Columns.IMAGE, image);
        cv.put(Columns.ISSUENAME, issueName);
        cv.put(Columns.NAME, name);
        cv.put(Columns.TYPENAME, typeName);
        cv.put(Columns.URL, url);
        cv.put(Columns.VALIDUNTIL, validUntil);
        return cv;
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
    
    public Long getId() {
        return id;
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
