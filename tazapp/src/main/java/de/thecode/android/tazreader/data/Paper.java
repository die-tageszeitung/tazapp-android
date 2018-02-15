package de.thecode.android.tazreader.data;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import com.squareup.picasso.Picasso;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper.Plist.Page.Article;
import de.thecode.android.tazreader.download.PaperDeletedEvent;
import de.thecode.android.tazreader.provider.TazProvider;
import de.thecode.android.tazreader.reader.index.IIndexItem;
import de.thecode.android.tazreader.utils.PlistHelper;
import de.thecode.android.tazreader.utils.ReadableException;
import de.thecode.android.tazreader.utils.StorageManager;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;

import timber.log.Timber;

public class Paper {

    public static final  String STORE_KEY_BOOKMARKS           = "bookmarks";
    public static final  String STORE_KEY_CURRENTPOSITION     = "currentPosition";
    private static final String STORE_KEY_POSITION_IN_ARTICLE = "positionInArticle";
    private static final String STORE_KEY_RESOURCE_PARTNER    = "resource";
    private static final String STORE_KEY_AUTO_DOWNLOADED    = "auto_download";

    public static       String TABLE_NAME        = "PAPER";
    public static final Uri    CONTENT_URI       = Uri.parse("content://" + TazProvider.AUTHORITY + "/" + TABLE_NAME);
    public static final String CONTENT_TYPE      = "vnd.android.cursor.dir/vnd.taz." + TABLE_NAME;
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.taz." + TABLE_NAME;

    public static final String CONTENT_PLIST_FILENAME = "content.plist";

    public static Paper getLatestPaper(Context context) {
        Cursor cursor = context.getContentResolver()
                               .query(CONTENT_URI, null, null, null, Columns.DATE + " DESC LIMIT 1");
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    return new Paper(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    public static List<Paper> getAllPapers(Context context) {
        List<Paper> result = new ArrayList<>();
        Cursor cursor = context.getApplicationContext()
                               .getContentResolver()
                               .query(CONTENT_URI, null, null, null, Columns.DATE + " DESC");
        try {
            while (cursor.moveToNext()) {
                result.add(new Paper(cursor));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    public static Paper getPaperWithId(Context context, long id) {
        Cursor cursor = context.getApplicationContext()
                               .getContentResolver()
                               .query(ContentUris.withAppendedId(CONTENT_URI, id), null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return new Paper(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public static Paper getPaperWithBookId(Context context, String bookId) {
        Uri bookIdUri = CONTENT_URI.buildUpon()
                                   .appendPath(bookId)
                                   .build();
        Cursor cursor = context.getApplicationContext()
                               .getContentResolver()
                               .query(bookIdUri, null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return new Paper(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }


    public static final class Columns implements BaseColumns {

        public static final String DATE            = "date";
        public static final String IMAGE           = "image";
        public static final String IMAGEHASH       = "imageHash";
        public static final String LINK            = "link";
        public static final String FILEHASH        = "fileHash";
        public static final String LEN             = "len";
        public static final String LASTMODIFIED    = "lastModified";
        public static final String BOOKID          = "bookId";
        public static final String ISDEMO          = "isDemo";
        public static final String HASUPDATE       = "hasUpdate";
        public static final String DOWNLOADID      = "downloadId";
        public static final String ISDOWNLOADED    = "isDownloaded";
        public static final String KIOSK           = "kiosk";
        public static final String IMPORTED        = "imported";
        public static final String TITLE           = "title";
        public static final String PUBLICATIONID   = "publicationId";
        public static final String RESOURCE        = "resource";
        //        public static final String RESOURCEFILEHASH = "resourceFileHash";
//        public static final String RESOURCEURL      = "resourceUrl";
//        public static final String RESOURCELEN      = "resourceLen";
        public static final String VALIDUNTIL      = "validUntil";
        public static final String FULL_VALIDUNTIL = TABLE_NAME + "." + VALIDUNTIL;
    }

    public final static int NOT_DOWNLOADED        = 1;
    public final static int DOWNLOADED_READABLE   = 2;
    public final static int DOWNLOADED_BUT_UPDATE = 3;
    public final static int IS_DOWNLOADING        = 4;
    public final static int NOT_DOWNLOADED_IMPORT = 5;

    private Long    id;
    private String  date;
    private String  image;
    private String  imageHash;
    private String  link;
    private String  fileHash;
    private long    len;
    private long    lastModified;
    private String  bookId;
    private boolean isDemo;
    private boolean hasupdate;
    private long    downloadId;
    private boolean isdownloaded;
    private boolean kiosk;
    private boolean imported;
    private String  title;
    private Long    publicationId;
    private String  resource;
    //    private String  resourceFileHash;
//    private String  resourceUrl;
//    private long    resourceLen;
    private long    validUntil;
    private int progress = 0;

    private Map<String, Integer> articleCollectionOrder;
    private Map<Integer, String> articleCollectionPositionIndex;


    public Paper() {
    }


    public Paper(Cursor cursor) {
        setData(cursor);
    }

    private void setData(Cursor cursor) {
        this.id = cursor.getLong(cursor.getColumnIndex(Columns._ID));
        this.date = cursor.getString(cursor.getColumnIndex(Columns.DATE));
        this.image = cursor.getString(cursor.getColumnIndex(Columns.IMAGE));
        this.imageHash = cursor.getString(cursor.getColumnIndex(Columns.IMAGEHASH));
        this.link = cursor.getString(cursor.getColumnIndex(Columns.LINK));
        this.fileHash = cursor.getString(cursor.getColumnIndex(Columns.FILEHASH));
        this.len = cursor.getLong(cursor.getColumnIndex(Columns.LEN));
        this.lastModified = cursor.getLong(cursor.getColumnIndex(Columns.LASTMODIFIED));
        this.bookId = cursor.getString(cursor.getColumnIndex(Columns.BOOKID));
        this.isDemo = getBoolean(cursor, cursor.getColumnIndex(Columns.ISDEMO));
        // this.filename = cursor.getString(cursor.getColumnIndex(Columns.FILENAME));
        // this.tempfilepath = cursor.getString(cursor.getColumnIndex(Columns.TEMPFILEPATH));
        // this.tempfilename = cursor.getString(cursor.getColumnIndex(Columns.TEMPFILENAME));
        this.hasupdate = getBoolean(cursor, cursor.getColumnIndex(Columns.HASUPDATE));
        this.downloadId = cursor.getLong(cursor.getColumnIndex(Columns.DOWNLOADID));
        //this.downloadprogress = cursor.getInt(cursor.getColumnIndex(Columns.DOWNLOADPROGRESS));
        this.isdownloaded = getBoolean(cursor, cursor.getColumnIndex(Columns.ISDOWNLOADED));
        this.kiosk = getBoolean(cursor, cursor.getColumnIndex(Columns.KIOSK));
        this.imported = getBoolean(cursor, cursor.getColumnIndex(Columns.IMPORTED));
        this.title = cursor.getString(cursor.getColumnIndex(Columns.TITLE));
        this.publicationId = cursor.getLong(cursor.getColumnIndex(Columns.PUBLICATIONID));
        //if (cursor.getColumnIndex(Publication.Columns.VALIDUNTIL) != -1)
        this.validUntil = cursor.getLong(cursor.getColumnIndex(Columns.VALIDUNTIL));
        this.resource = cursor.getString(cursor.getColumnIndex(Columns.RESOURCE));
//        this.resourceFileHash = cursor.getString(cursor.getColumnIndex(Columns.RESOURCEFILEHASH));
//        this.resourceUrl = cursor.getString(cursor.getColumnIndex(Columns.RESOURCEURL));
//        this.resourceLen = cursor.getLong(cursor.getColumnIndex(Columns.RESOURCELEN));
    }

    private boolean getBoolean(Cursor cursor, int columnIndex) {
        return !(cursor.isNull(columnIndex) || cursor.getShort(columnIndex) == 0);
    }

    public Paper(NSDictionary nsDictionary) {
        this.date = PlistHelper.getString(nsDictionary, Columns.DATE);
        this.image = PlistHelper.getString(nsDictionary, Columns.IMAGE);
        this.imageHash = PlistHelper.getString(nsDictionary, Columns.IMAGEHASH);
        this.link = PlistHelper.getString(nsDictionary, Columns.LINK);
        this.fileHash = PlistHelper.getString(nsDictionary, Columns.FILEHASH);
        this.len = PlistHelper.getInt(nsDictionary, Columns.LEN);
        this.lastModified = PlistHelper.getInt(nsDictionary, Columns.LASTMODIFIED);
        this.bookId = PlistHelper.getString(nsDictionary, Columns.BOOKID);
        this.isDemo = PlistHelper.getBoolean(nsDictionary, Columns.ISDEMO);
        this.resource = PlistHelper.getString(nsDictionary, Columns.RESOURCE);
//        this.resourceFileHash = PlistHelper.getString(nsDictionary, Columns.RESOURCEFILEHASH);
//        this.resourceUrl = PlistHelper.getString(nsDictionary, Columns.RESOURCEURL);
//        this.resourceLen = PlistHelper.getInt(nsDictionary, Columns.RESOURCELEN);
    }

    public ContentValues getContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Columns.BOOKID, bookId);
        cv.put(Columns.DATE, date);
        //cv.put(Columns.DOWNLOADPROGRESS, downloadprogress);
        cv.put(Columns.FILEHASH, fileHash);
        // cv.put(Columns.FILENAME, filename);
        cv.put(Columns.HASUPDATE, hasupdate);
        cv.put(Columns.IMAGE, image);
        cv.put(Columns.IMAGEHASH, imageHash);
        cv.put(Columns.IMPORTED, imported);
        cv.put(Columns.ISDEMO, isDemo);
        cv.put(Columns.ISDOWNLOADED, isdownloaded);
        cv.put(Columns.DOWNLOADID, downloadId);
        cv.put(Columns.KIOSK, kiosk);
        cv.put(Columns.LASTMODIFIED, lastModified);
        cv.put(Columns.LEN, len);
        cv.put(Columns.LINK, link);
        // cv.put(Columns.TEMPFILENAME, tempfilename);
        // cv.put(Columns.TEMPFILEPATH, tempfilepath);
        cv.put(Columns.TITLE, title);
        cv.put(Columns.PUBLICATIONID, publicationId);
        cv.put(Columns.RESOURCE, resource);
//        cv.put(Columns.RESOURCEFILEHASH, resourceFileHash);
//        cv.put(Columns.RESOURCEURL, resourceUrl);
//        cv.put(Columns.RESOURCELEN, resourceLen);
        cv.put(Columns.VALIDUNTIL, validUntil);
        cv.put(Columns._ID, id);
        return cv;
    }

    public Uri getContentUri() {
        return ContentUris.withAppendedId(Paper.CONTENT_URI, getId());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookId() {
        return bookId;
    }

    public String getDate(int style) throws ParseException {
        SimpleDateFormat outformat = (SimpleDateFormat) DateFormat.getDateInstance(style);
        return outformat.format(parseDate());
    }

    private Date parseDate() throws ParseException {
        SimpleDateFormat informat = new SimpleDateFormat("yyyy-MM-dd");
        return informat.parse(this.date);
    }


    public String getTitelWithDate(String seperator) {
        try {
            return getTitle() + " " + seperator + " " + getDate(DateFormat.MEDIUM);
        } catch (ParseException e) {
            return getTitle();
        }
    }

    public String getTitelWithDate(Context context) {
        return getTitelWithDate(context.getString(R.string.string_titel_seperator));
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getImage() {
        return image;
    }

    public boolean isDemo() {
        return isDemo;
    }

    public boolean isDownloaded() {
        return isdownloaded;
    }

    public boolean isDownloading() {
        return downloadId != 0;
    }

    public boolean hasUpdate() {
        return hasupdate;
    }

    public boolean isImported() {
        return imported;
    }

    public boolean isKiosk() {
        return kiosk;
    }

    public int getState() {

        if (this.isDownloaded() && !this.isDownloading() && !this.hasUpdate()) {
            return DOWNLOADED_READABLE;
        } else if (this.isDownloading()) {
            return IS_DOWNLOADING;
        } else if (this.isDownloaded() && this.hasUpdate() && !this.isDownloading()) {
            return DOWNLOADED_BUT_UPDATE;
        } else if (!this.isDownloaded() && (this.isKiosk() || this.isImported())) {
            return NOT_DOWNLOADED_IMPORT;
        }

        return NOT_DOWNLOADED;
    }

    public String getDate() {
        return date;
    }

    public long getDateInMillis() throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).parse(getDate())
                                                                 .getTime();
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void setDemo(boolean isDemo) {
        this.isDemo = isDemo;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public void setLen(long len) {
        this.len = len;
    }

    public long getLen() {
        return len;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setDownloaded(boolean isdownloaded) {
        this.isdownloaded = isdownloaded;
    }

    public void setHasupdate(boolean hasupdate) {
        this.hasupdate = hasupdate;
    }

    public void setIsdownloaded(boolean isdownloaded) {
        this.isdownloaded = isdownloaded;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public void setKiosk(boolean kiosk) {
        this.kiosk = kiosk;
    }

    public String getTitle() {
        if (title == null) return "";
        return title;
    }

    public Long getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(Long publicationId) {
        this.publicationId = publicationId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(long validUntil) {
        this.validUntil = validUntil;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

//    public void setResourceFileHash(String resourceFileHash) {
//        this.resourceFileHash = resourceFileHash;
//    }
//
//    public void setResourceUrl(String resourceUrl) {
//        this.resourceUrl = resourceUrl;
//    }
//
//    public void setResourceLen(long resourceLen) {
//        this.resourceLen = resourceLen;
//    }

    public String getResource() {
        return resource;
    }

//    public String getResourceFileHash() {
//        return resourceFileHash;
//    }
//
//    public String getResourceUrl() {
//        return resourceUrl;
//    }
//
//    public long getResourceLen() {
//        return resourceLen;
//    }

    public int getArticleCollectionOrderPosition(String key) {
        return articleCollectionOrder.get(key);
    }

    public int getArticleCollectionSize() {
        return articleCollectionOrder.size();
    }

    public String getArticleCollectionOrderKey(int postion) {
        return articleCollectionPositionIndex.get(postion);
    }

    public void setArticleCollectionOrder(Map<String, Integer> articleCollectionOrder) {
        this.articleCollectionOrder = articleCollectionOrder;
    }

    public void setArticleCollectionPositionIndex(Map<Integer, String> articleCollectionPositionIndex) {
        this.articleCollectionPositionIndex = articleCollectionPositionIndex;
    }

    private Plist plist;

    public void parsePlist(File file) throws IOException, PropertyListFormatException, ParseException,
            ParserConfigurationException, SAXException {
        parsePlist(file, true);
    }

    public void parsePlist(File file, boolean parseIndex) throws ParserConfigurationException, ParseException, SAXException,
            PropertyListFormatException, IOException {
        FileInputStream fis = new FileInputStream(file);
        parsePlist(fis, parseIndex);
    }

    public void parsePlist(InputStream is) throws ZipException, IOException, PropertyListFormatException, ParseException,
            ParserConfigurationException, SAXException {
        parsePlist(is, true);
    }

    public void parsePlist(InputStream is, boolean parseIndex) throws ZipException, IOException, PropertyListFormatException,
            ParseException, ParserConfigurationException, SAXException {
        Timber.i("Start parsing Plist - parse Index: %s", parseIndex);
        plist = new Plist(is, parseIndex);
        Timber.i("Finished parsing Plist");
    }


    public Plist getPlist() {
        if (plist == null) throw new IllegalStateException("No Plist parsed. Call parsePlist() before!");
        return plist;
    }

    public boolean parseMissingAttributes(boolean overwrite) {
        boolean result = false; //Did you write somthing new?
        if (TextUtils.isEmpty(getResource()) || overwrite) {
            setResource(getPlist().getResource());
            result = true;
        }
        return result;
    }

    public class Plist {

        private static final String KEY_ARCHIVEURL = "ArchivUrl";
        private static final String KEY_BOOKID     = "BookId";
        private static final String KEY_RESOURCE   = "ressource";
        private static final String KEY_MINVERSION = "MinVersion";
        private static final String KEY_VERSION    = "Version";
        private static final String KEY_HASHVALS   = "HashVals";
        private static final String KEY_SOURCES    = "Quellen";
        private static final String KEY_TOPLINKS   = "TopLinks";

        private String              archiveUrl;
        private String              bookId;
        private String              resource;
        private String              minVersion;
        private String              version;
        private Map<String, String> hashVals;

        private NSDictionary root;

        private List<Source>  sources;
        private List<TopLink> toplinks;
        private Map<String, IIndexItem> indexMap = new LinkedHashMap<>();

        public Plist(InputStream is, boolean parseIndex) throws IOException, PropertyListFormatException, ParseException,
                ParserConfigurationException, SAXException {

            root = (NSDictionary) PropertyListParser.parse(is);

            is.close();

            archiveUrl = PlistHelper.getString(root, KEY_ARCHIVEURL);
            bookId = PlistHelper.getString(root, KEY_BOOKID);
            resource = PlistHelper.getString(root, KEY_RESOURCE);
            if (!TextUtils.isEmpty(resource)) {
                resource = resource.replace(".res", "");
            }
            minVersion = PlistHelper.getString(root, KEY_MINVERSION);
            version = PlistHelper.getString(root, KEY_VERSION);

            parseHashVals();

            if (parseIndex) {
                parseSources();
                parseToplinks();
            }

        }

        private void parseHashVals() {
            hashVals = new HashMap<>();
            if (root.containsKey(KEY_HASHVALS)) {
                NSDictionary hashValsDict = (NSDictionary) root.objectForKey(KEY_HASHVALS);
                Set<Map.Entry<String, NSObject>> set = hashValsDict.entrySet();
                for (Map.Entry<String, NSObject> stringNSObjectEntry : set) {
                    hashVals.put(stringNSObjectEntry.getKey(), ((NSString) stringNSObjectEntry.getValue()).getContent());
                }
            }
        }

        private void parseSources() {
            sources = new ArrayList<>();
            if (root.containsKey(KEY_SOURCES)) {
                NSObject[] sourcesArray = ((NSArray) root.objectForKey(KEY_SOURCES)).getArray();
                if (sourcesArray != null) {
                    // result = new Source[sources.length];
                    for (NSObject sourceObject : sourcesArray) {
                        NSDictionary sourceDict = (NSDictionary) sourceObject;
                        String key = sourceDict.allKeys()[0];
                        Source source = new Source(key, (NSArray) sourceDict.objectForKey(key));
                        indexMap.put(source.getKey(), source);
                        source.parseBooks();
                        sources.add(source);
                    }
                }
            }
        }

        private void parseToplinks() {
            toplinks = new ArrayList<>();
            if (root.containsKey(KEY_TOPLINKS)) {

                NSObject[] toplinksArray = ((NSArray) root.objectForKey(KEY_TOPLINKS)).getArray();
                if (toplinksArray != null) {
                    for (NSObject toplinkeObject : toplinksArray) {
                        NSDictionary toplinkDict = (NSDictionary) toplinkeObject;
                        String key = toplinkDict.allKeys()[0];
                        TopLink toplink = new TopLink(key, PlistHelper.getString(toplinkDict, key));
                        boolean foundDefaultPageLink = false;
                        for (Source source : getSources()) {
                            for (Book book : source.getBooks()) {
                                for (Category category : book.getCategories()) {
                                    for (Page page : category.getPages()) {
                                        if (toplink.getKey()
                                                   .equals(page.getDefaultLink())) {
                                            toplink.page = page;
                                            foundDefaultPageLink = true;
                                        }
                                        if (foundDefaultPageLink) break;
                                    }
                                    if (foundDefaultPageLink) break;
                                }
                                if (foundDefaultPageLink) break;
                            }
                            if (foundDefaultPageLink) break;
                        }
                        if (indexMap.containsKey(toplink.getKey())) {
                            toplink.setLink(indexMap.get(toplink.getKey()));
                        } else {
                            indexMap.put(toplink.getKey(), toplink);
                        }
                        toplinks.add(toplink);
                    }
                }
            }
        }

        public String getArchiveUrl() {
            return archiveUrl;
        }

        public String getBookId() {
            return bookId;
        }

        public String getResource() {
            return resource;
        }

        public String getMinVersion() {
            return minVersion;
        }

        public String getVersion() {
            return version;
        }

        public Map<String, String> getHashVals() {
            return hashVals;
        }

        public List<Source> getSources() {
            return sources;
        }

        public List<TopLink> getToplinks() {
            return toplinks;
        }

        public ArrayList<Page> getAllPages() {
            ArrayList<Page> result = new ArrayList<>();
            for (Source aSource : getSources()) {
                for (Book aBook : aSource.getBooks()) {
                    for (Category aCategory : aBook.getCategories()) {
                        result.addAll(aCategory.getPages());
                    }
                }
            }
            return result;
        }

        public IIndexItem getIndexItem(String key) {
            return indexMap.get(key);
        }

        public class Source extends IndexItemTemplate {

            String     key;
            NSArray    array;
            List<Book> books;

            public Source(String key, NSArray array) {
                this.key = key;
                this.array = array;
            }

            public String getKey() {
                return key;
            }

            private void parseBooks() {
                books = new ArrayList<>();
                NSObject[] sourceBooks = array.getArray();
                if (sourceBooks != null) {
                    for (NSObject sourceBook : sourceBooks) {
                        NSDictionary sourceBookDict = (NSDictionary) sourceBook;
                        String key = sourceBookDict.allKeys()[0];
                        Book book = new Book(this, key, (NSArray) sourceBookDict.objectForKey(key));

                        book.parseCategories();
                        books.add(book);
                    }
                }
            }

            @Override
            public String getTitle() {
                return getKey();
            }

            public List<Book> getBooks() {
                if (books == null) throw new IllegalStateException("Books not parsed yet, call parseBooks() before");
                return books;
            }

            @Override
            public IIndexItem getIndexParent() {
                return null;
            }

            @Override
            public boolean hasIndexParent() {
                return false;
            }

            @Override
            public boolean hasIndexChilds() {
                if (books != null) {
                    for (Book book : getBooks()) {
                        if (book.categories != null) {
                            if (book.categories.size() > 0) return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public List<IIndexItem> getIndexChilds() {
                List<IIndexItem> resultList = new ArrayList<>();
                if (hasIndexChilds()) {
                    for (Book book : getBooks()) {
                        resultList.addAll(book.getCategories());
                    }
                    return resultList;
                }
                return null;
            }

        }

        public class Book {

            Source         source;
            String         key;
            NSArray        array;
            List<Category> categories;

            public Book(Source source, String key, NSArray array) {
                this.source = source;
                this.key = source.getKey() + "_" + key;
                this.array = array;
            }

            private void parseCategories() {
                categories = new ArrayList<>();

                NSObject[] sourceBookCategories = array.getArray();
                if (sourceBookCategories != null) {
                    for (NSObject sourceBookCategory : sourceBookCategories) {
                        NSDictionary sourceBookCategoryDict = (NSDictionary) sourceBookCategory;
                        String key = sourceBookCategoryDict.allKeys()[0];
                        Category category = new Category(this, key, (NSArray) sourceBookCategoryDict.objectForKey(key));
                        indexMap.put(category.getKey(), category);
                        category.parsePages();
                        category.parseRealPagesForArticles();
                        categories.add(category);
                    }
                }
            }

            public String getKey() {
                return key;
            }

            public Source getSource() {
                return source;
            }

            public List<Category> getCategories() {
                if (categories == null)
                    throw new IllegalStateException("Categories not parsed yet, call parseCategeories() before");
                return categories;
            }

        }

        public class Category extends IndexItemTemplate {

            Book       book;
            String     key;
            String     title;
            NSArray    array;
            List<Page> pages;

            public Category(Book book, String key, NSArray array) {
                this.book = book;

                //Da Key nicht einzigartig in PList
                this.key = book.getKey() + "_" + key + "_" + book.getCategories()
                                                                 .size();
                this.title = key;

                this.array = array;
            }

            public Book getBook() {
                return book;
            }

            private void parsePages() {
                pages = new ArrayList<>();
                NSObject[] sourceBookCategoryPages = array.getArray();
                if (sourceBookCategoryPages != null) {
                    for (NSObject sourceBookCategoryPage : sourceBookCategoryPages) {
                        NSDictionary sourceBookCategoryPageDict = (NSDictionary) sourceBookCategoryPage;
                        String key = sourceBookCategoryPageDict.allKeys()[0];
                        Page page = new Page(this, key, (NSDictionary) sourceBookCategoryPageDict.objectForKey(key));
                        indexMap.put(page.getKey(), page);
                        for (Article article : page.getArticles()) {
                            indexMap.put(article.getKey(), article);
                        }
                        pages.add(page);
                    }
                }
            }

            private void parseRealPagesForArticles() {
                List<Article> allArticlesFromCategory = new ArrayList<>();
                for (Page aPage : getPages()) {
                    allArticlesFromCategory.addAll(aPage.getArticles());
                }
                for (Article aArticle : allArticlesFromCategory) {
                    for (Page aPage : getPages()) {
                        for (Page.Geometry aGeometry : aPage.getGeometries()) {
                            if (aGeometry.getLink()
                                         .equals(aArticle.getKey())) {
                                aArticle.setRealPage(aPage);
                                break;
                            }
                        }
                        if (aArticle.getRealPage() != null) break;
                    }
                }
            }


            @Override
            public String getTitle() {
                return title;
            }

            public List<Page> getPages() {
                if (pages == null) throw new IllegalStateException("Pages not parsed yet, call parsePages() before");
                return pages;
            }

            public String getKey() {
                return key;
            }

            @Override
            public IIndexItem getIndexParent() {
                return getBook().getSource();
            }

            @Override
            public boolean hasIndexParent() {
                return true;
            }

            @Override
            public boolean hasIndexChilds() {
                if (pages != null) {
                    if (pages.size() > 0) return true;
                }
                return false;
            }

            @Override
            public List<IIndexItem> getIndexChilds() {
                List<IIndexItem> resultList = new ArrayList<>();
                if (hasIndexChilds()) {
                    for (Page page : getPages()) {
                        //resultList.add(page);
                        resultList.addAll(page.getArticles());
                    }
                    return resultList;
                }
                return null;
            }

        }

        public class Page extends IndexItemTemplate {

            private static final String KEY_GEOMETRY    = "geometry";
            private static final String KEY_PAGINA      = "SeitenNummer";
            private static final String KEY_DEFAULTLINK = "defaultLink";
            private static final String KEY_ARTICLE     = "Artikel";
            private static final String KEY_RIGHT       = "right";
            private static final String KEY_LEFT        = "left";

            Category category;
            String   key;
            String   pagina;
            String   defaultLink;
            String   left;
            String   right;

            NSArray geometryArray;
            NSArray articleArray;

            List<Geometry> geometries;
            List<Article>  articles;

            public Page(Category category, String key, NSDictionary dict) {
                this.category = category;
                this.key = key;
                if (dict != null) {
                    pagina = PlistHelper.getString(dict, KEY_PAGINA);
                    left = PlistHelper.getString(dict, KEY_LEFT);
                    right = PlistHelper.getString(dict, KEY_RIGHT);
                    defaultLink = PlistHelper.getString(dict, KEY_DEFAULTLINK);
                    geometryArray = (NSArray) dict.get(KEY_GEOMETRY);
                    articleArray = (NSArray) dict.get(KEY_ARTICLE);
                    parseGeometries();
                    parseArticles();
                }

            }

            private void parseGeometries() {
                geometries = new ArrayList<>();
                NSObject[] sourceBookCategoryPageGeometries = geometryArray.getArray();
                if (sourceBookCategoryPageGeometries != null) {
                    for (NSObject sourceBookCategoryPageGeometry : sourceBookCategoryPageGeometries) {
                        NSDictionary sourceBookCategoryPageGeometryDict = (NSDictionary) sourceBookCategoryPageGeometry;
                        geometries.add(new Geometry(sourceBookCategoryPageGeometryDict));
                    }
                }
            }

            private void parseArticles() {
                articles = new ArrayList<>();
                NSObject[] sourceBookCategoryPageArticles = articleArray.getArray();
                if (sourceBookCategoryPageArticles != null) {
                    for (NSObject sourceBookCategoryPageArticle : sourceBookCategoryPageArticles) {
                        NSDictionary sourceBookCategoryPageArticleDict = (NSDictionary) sourceBookCategoryPageArticle;
                        String key = sourceBookCategoryPageArticleDict.allKeys()[0];
                        Article article = new Article(key, (NSDictionary) sourceBookCategoryPageArticleDict.get(key));
//                        for (Geometry geometry : getGeometries()) {
//                            if (article.getKey()
//                                       .equals(geometry.getLink())) {
//                                //geometry.article = article;
//                                article.geometries.add(geometry);
//                            }
//                        }
                        articles.add(article);
                    }
                }
            }


            public List<Article> getArticles() {
                if (articles == null) throw new IllegalStateException("Articles not parsed yet, call parseArticles() before");
                return articles;
            }

            public List<Geometry> getGeometries() {
                if (geometries == null)
                    throw new IllegalStateException("Geometries not parsed yet, call parseGeometries() before");
                return geometries;
            }

            @Override
            public String getTitle() {
                return category.getTitle() + " " + pagina;
            }


            public String getDefaultLink() {
                return defaultLink;
            }

            public Category getCategory() {
                return category;
            }

            public String getKey() {
                return key;
            }

            @Override
            public IIndexItem getIndexParent() {
                return getCategory();
            }

            @Override
            public boolean hasIndexParent() {
                return true;
            }

            @Override
            public boolean hasIndexChilds() {
                return false;
            }

            @Override
            public List<IIndexItem> getIndexChilds() {
                return null;
            }

            @Override
            public boolean isShareable() {
                return true;
            }

            @Override
            public Intent getShareIntent(Context context) {
                StorageManager storage = StorageManager.getInstance(context);

                File pdfFile = new File(storage.getPaperDirectory(getPaper()), getKey());
                File papersDir = storage.get(StorageManager.PAPER);

                try {
                    String pagePath = pdfFile.getCanonicalPath()
                                             .replace(papersDir.getCanonicalPath(), "papers");

                    Uri contentUri = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".streamprovider")
                                        .buildUpon()
                                        .appendEncodedPath(pagePath)
                                        .build();

                    //share Intent
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    intent.putExtra(Intent.EXTRA_SUBJECT, getPaper().getTitelWithDate(context) + ": " + getTitle());
                    intent.putExtra(Intent.EXTRA_TEXT, getPaper().getTitelWithDate(context) + "\n" + getTitle());

                    //get extra intents to view pdf
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    viewIntent.setData(contentUri);
                    PackageManager packageManager = context.getPackageManager();
                    List<ResolveInfo> resInfo = packageManager.queryIntentActivities(viewIntent, 0);
                    Intent[] extraIntents = new Intent[resInfo.size()];
                    for (int i = 0; i < resInfo.size(); i++) {
                        // Extract the label, append it, and repackage it in a LabeledIntent
                        ResolveInfo ri = resInfo.get(i);
                        String packageName = ri.activityInfo.packageName;
                        Intent extraViewIntent = new Intent();
                        extraViewIntent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
                        extraViewIntent.setAction(Intent.ACTION_VIEW);
                        extraViewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        extraViewIntent.setData(contentUri);
                        CharSequence label = ri.loadLabel(packageManager);
                        extraIntents[i] = new LabeledIntent(extraViewIntent, packageName, label, ri.icon);
                    }
                    Intent chooserIntent = Intent.createChooser(intent, context.getString(R.string.reader_action_share_open));
                    if (extraIntents.length > 0) chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
                    return chooserIntent;

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            public class Geometry {

                private static final String KEY_X1   = "x1";
                private static final String KEY_Y1   = "y1";
                private static final String KEY_X2   = "x2";
                private static final String KEY_Y2   = "y2";
                private static final String KEY_LINK = "link";

                private float  x1;
                private float  y1;
                private float  x2;
                private float  y2;
                private String link;

                //private Article article;

                public Geometry(NSDictionary dict) {
                    this.x1 = PlistHelper.getFloat(dict, KEY_X1);
                    this.y1 = PlistHelper.getFloat(dict, KEY_Y1);
                    this.x2 = PlistHelper.getFloat(dict, KEY_X2);
                    this.y2 = PlistHelper.getFloat(dict, KEY_Y2);
                    this.link = PlistHelper.getString(dict, KEY_LINK);
                }

                public float getX1() {
                    return x1;
                }

                public float getY1() {
                    return y1;
                }

                public float getX2() {
                    return x2;
                }

                public float getY2() {
                    return y2;
                }

                public String getLink() {
                    return link;
                }

                public Page getPage() {
                    return Page.this;
                }

                public boolean checkCoordinates(float x, float y) {
                    return x >= x1 && x <= x2 && y >= y1 && y <= y2;
                }

                //                public Article getArticle() {
                //                    return article;
                //                }
            }

            public class Article extends IndexItemTemplate //implements ArticleReaderItem
            {

                private static final String KEY_TITLE      = "Titel";
                private static final String KEY_SUBTITLE   = "Untertitel";
                private static final String KEY_ONLINELINK = "OnlineLink";

                private String key;
                private String title;
                private String subtitle;
                private String onlinelink;
                private Page   realPage;

                public Article(String key, NSDictionary dict) {
                    this.key = key;
                    this.title = PlistHelper.getString(dict, KEY_TITLE);
                    this.subtitle = PlistHelper.getString(dict, KEY_SUBTITLE);
                    this.onlinelink = PlistHelper.getString(dict, KEY_ONLINELINK);
                }

                public String getKey() {
                    return key;
                }

                public String getTitle() {
                    if (title == null || "".equals(title)) return getRealPage().getTitle();
                    return title;
                }

                public String getSubtitle() {
                    return subtitle;
                }

                public String getOnlinelink() {
                    return onlinelink;
                }

                public Category getCategory() {
                    return category;
                }

                public Page getRealPage() {
                    return realPage;
                }

                public void setRealPage(Page realPage) {
                    this.realPage = realPage;
                }

                public Page getPage() {
                    return Page.this;
                }

                //                public void setBookmarked(boolean bookmarked) {
                //                    this.bookmarked = bookmarked;
                //                }
                //
                //                public boolean isBookmarked() {
                //                    return bookmarked;
                //                }


                @Override
                public boolean isBookmarkable() {
                    return true;
                }

                @Override
                public IIndexItem getIndexParent() {
                    return getPage().getCategory();
                }

                @Override
                public boolean hasIndexParent() {
                    return true;
                }

                @Override
                public boolean hasIndexChilds() {
                    return false;
                }

                @Override
                public List<IIndexItem> getIndexChilds() {
                    return null;
                }

                @Override
                public boolean isShareable() {
                    return !TextUtils.isEmpty(onlinelink);
                }

                @Override
                public Intent getShareIntent(Context context) {
                    StringBuilder text = new StringBuilder(getPaper().getTitelWithDate(context)).append("\n")
                                                                                                .append(getTitle())
                                                                                                .append("\n\n");
                    if (!TextUtils.isEmpty(getSubtitle())) {
                        text.append(getSubtitle())
                            .append("\n\n");
                    }
                    text.append(getOnlinelink());


                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_SUBJECT, getPaper().getTitelWithDate(context) + ": " + getTitle());
                    intent.putExtra(Intent.EXTRA_TEXT, text.toString());

                    return Intent.createChooser(intent, context.getString(R.string.reader_action_share));
                }
            }

        }

        public class TopLink extends IndexItemTemplate //implements ArticleReaderItem
        {

            private String title;
            private String key;

            private Page page;

            public TopLink(String key, String title) {
                this.key = key;
                this.title = title;
            }

            public String getKey() {
                return key;
            }

            @Override
            public String getTitle() {
                return title;
            }

            public Page getPage() {
                return page;
            }

            @Override
            public IIndexItem getIndexParent() {
                return null;
            }

            @Override
            public boolean hasIndexParent() {
                return false;
            }

            @Override
            public boolean hasIndexChilds() {
                return false;
            }

            @Override
            public List<IIndexItem> getIndexChilds() {
                return null;
            }

        }

        public abstract class IndexItemTemplate implements de.thecode.android.tazreader.reader.index.IIndexItem {

            boolean childsVisible = true;

            Type type;

            private boolean    bookmarked;
            private IIndexItem link;

            public abstract String getTitle();

            public void setIndexChildsVisible(boolean childsVisible) {

                this.childsVisible = childsVisible;

                if (!childsVisible && hasIndexChilds()) {
                    for (IIndexItem childItem : getIndexChilds()) {
                        childItem.setIndexChildsVisible(false);
                    }
                }
            }

            public boolean areIndexChildsVisible() {
                return childsVisible;
            }

            public boolean isVisible() {
                IIndexItem indexParent = getIndexParent();
                return indexParent == null || indexParent.areIndexChildsVisible();
            }

            public boolean hasBookmarkedChilds() {
                if (getIndexChilds() != null) {
                    for (IIndexItem child : getIndexChilds()) {

                        if (child.isBookmarked()) {
                            return true;
                        } else {
                            if (child.hasBookmarkedChilds()) return true;
                        }
                    }
                }
                return false;
            }

            public IIndexItem getIndexAncestorWithKey(String key) {
                if (hasIndexParent()) {
                    if (getIndexParent().getKey()
                                        .equals(key)) return getIndexParent();
                    else return getIndexParent().getIndexAncestorWithKey(key);

                }
                return null;
            }


            public Type getType() {
                if (type == null) {
                    if (this instanceof Source) type = Type.SOURCE;
                    else if (this instanceof Category) type = Type.CATEGORY;
                    else if (this instanceof Page) type = Type.PAGE;
                    else if (this instanceof Article) type = Type.ARTICLE;
                    else if (this instanceof TopLink) type = Type.TOPLINK;
                    else type = Type.UNKNOWN;
                }
                return type;
            }

            public int getIndexChildCount() {
                if (!hasIndexChilds()) return 0;
                return getIndexChilds().size();
            }

            public Paper getPaper() {
                return Paper.this;
            }

            @Override
            public boolean isBookmarkable() {
                return false;
            }

            @Override
            public void setBookmark(boolean bookmarked) {
                if (isBookmarkable()) this.bookmarked = bookmarked;
            }

            @Override
            public boolean isBookmarked() {
                return bookmarked;
            }

            @Override
            public boolean isShareable() {
                return false;
            }

            @Override
            public Intent getShareIntent(Context context) {
                return null;
            }

            public IIndexItem getLink() {
                return link;
            }

            public boolean isLink() {
                if (link != null) return true;
                return false;
            }

            @Override
            public void setLink(IIndexItem link) {
                this.link = link;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(getClass().getSimpleName())
                   .append("@")
                   .append(Integer.toHexString(hashCode()));
            builder.append("|")
                   .append(bookId);
            if (getSources() != null) {
                builder.append("|")
                       .append("sources:")
                       .append(getSources().size());
            }
            if (getToplinks() != null) {
                builder.append("|")
                       .append("toplinks:")
                       .append(getToplinks().size());
            }
            return builder.toString();
        }
    }

    public JSONArray getBookmarkJson() {
        JSONArray array = new JSONArray();
        try {


            for (Map.Entry<String, IIndexItem> entry : getPlist().indexMap.entrySet()) {
                switch (entry.getValue()
                             .getType()) {
                    case ARTICLE:
                        if (((Article) entry.getValue()).isBookmarked()) {
                            array.put(entry.getValue()
                                           .getKey());
                        }
                        break;
                }
            }
        } catch (Exception e) {

        }
        return array;
    }

    public boolean savePositionInArticle(Context context, IIndexItem article, String position) {
        return saveStoreValue(context, STORE_KEY_POSITION_IN_ARTICLE + "_" + article.getKey(), position);
    }

    public String getPositionInArticle(Context context, IIndexItem article) {
        String result = getStoreValue(context, STORE_KEY_POSITION_IN_ARTICLE + "_" + article.getKey());
        return (TextUtils.isEmpty(result)) ? "0" : result;
    }

    public boolean saveAutoDownloaded(Context context, boolean isAutoDownload) {
        return saveStoreValue(context, STORE_KEY_AUTO_DOWNLOADED, String.valueOf(isAutoDownload));
    }

    public boolean isAutoDownloaded(Context context) {
        String resultString = getStoreValue(context, STORE_KEY_AUTO_DOWNLOADED);
        return Boolean.parseBoolean(resultString);
    }

    public boolean saveResourcePartner(Context context, Resource resource) {
        return saveStoreValue(context, STORE_KEY_RESOURCE_PARTNER, resource.getKey());
    }

    public void deleteResourcePartner(Context context) {
        deleteStoreKey(context, STORE_KEY_RESOURCE_PARTNER);
    }

    public Resource getResourcePartner(Context context) {
        String resource = getStoreValue(context, STORE_KEY_RESOURCE_PARTNER);
        if (TextUtils.isEmpty(resource)) resource = getResource(); //Fallback;
        return Resource.getWithKey(context, resource);
    }

    public String getStoreValue(Context context, String key) {
        String path = getBookId() + "/" + key;
        return Store.getValueForKey(context, path);
    }

    public void deleteStoreKey(Context context, String key) {
        String path = getBookId() + "/" + key;
        Store.deleteKey(context, path);
    }

    public boolean saveStoreValue(Context context, String key, String value) {
        String path = getBookId() + "/" + key;
        return Store.saveValueForKey(context, path, value);
    }

    public void delete(Context context) {
        StorageManager storage = StorageManager.getInstance(context);
        storage.deletePaperDir(this);
        Picasso.with(context)
               .invalidate(getImage());
        deleteResourcePartner(context);
        if (isImported() || isKiosk()) {
            context.getContentResolver()
                   .delete(ContentUris.withAppendedId(Paper.CONTENT_URI, getId()), null, null);
        } else {
            setDownloadId(0);
            setDownloaded(false);
            setHasupdate(false);
            if (BuildConfig.BUILD_TYPE.equals("staging"))
                setValidUntil(0); //Wunsch von Ralf, damit besser im Staging getestet werden kann
            int affected = context.getContentResolver()
                                  .update(ContentUris.withAppendedId(Paper.CONTENT_URI, getId()), getContentValues(), null, null);
            if (affected >= 1) EventBus.getDefault()
                                       .post(new PaperDeletedEvent(getId()));
        }

    }

    @Override
    public String toString() {
        //return Log.toString(this, ":", "; ");
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        Paper rhs = (Paper) obj;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(date, rhs.date);
        equalsBuilder.append(image, rhs.image);
        equalsBuilder.append(imageHash, rhs.imageHash);
        equalsBuilder.append(link, rhs.link);
        equalsBuilder.append(fileHash, rhs.fileHash);
        equalsBuilder.append(len, rhs.len);
        equalsBuilder.append(resource, rhs.resource);
//        equalsBuilder.append(resourceFileHash, rhs.resourceFileHash);
//        equalsBuilder.append(resourceLen, rhs.resourceLen);
//        equalsBuilder.append(resourceUrl, rhs.resourceUrl);
        equalsBuilder.append(lastModified, rhs.lastModified);
        equalsBuilder.append(bookId, rhs.bookId);
        equalsBuilder.append(isDemo, rhs.isDemo);
        equalsBuilder.append(publicationId, rhs.publicationId);
        equalsBuilder.append(validUntil, rhs.validUntil);
        return equalsBuilder.isEquals();
    }

    public static class PaperNotFoundException extends ReadableException {
        public PaperNotFoundException() {
        }

        public PaperNotFoundException(String detailMessage) {
            super(detailMessage);
        }

        public PaperNotFoundException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public PaperNotFoundException(Throwable throwable) {
            super(throwable);
        }
    }
}
