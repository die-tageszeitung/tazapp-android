package de.thecode.android.tazreader.data;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;

import com.commonsware.cwac.provider.StreamProvider;
import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper.Plist.Page.Article;
import de.thecode.android.tazreader.utils.PlistHelper;
import de.thecode.android.tazreader.utils.ReadableException;
import de.thecode.android.tazreader.utils.StorageManager;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
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

import javax.xml.parsers.ParserConfigurationException;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import timber.log.Timber;

@Entity(tableName = "PAPER")
public class Paper extends Downloadable {

    public static final String STORE_KEY_BOOKMARKS           = "bookmarks";
    public static final String STORE_KEY_CURRENTPOSITION     = "currentPosition";
    public static final String STORE_KEY_POSITION_IN_ARTICLE = "positionInArticle";
    public static final String STORE_KEY_RESOURCE_PARTNER    = "resource";
    public static final String STORE_KEY_AUTO_DOWNLOADED     = "auto_download";

    public static final String CONTENT_PLIST_FILENAME = "content.plist";

    @NonNull
    @PrimaryKey
    private String               bookId;
    private String               date;
    private String               image;
    private String               imageHash;
    private String               link;
    private long                 lastModified;
    private String               resource;
    private boolean              demo;
    private String               title;
    private long                 validUntil;
    private String               publication;
    @Ignore
    private Map<String, Integer> articleCollectionOrder;
    @Ignore
    private Map<Integer, String> articleCollectionPositionIndex;


    public Paper() {
    }


    public Paper(NSDictionary nsDictionary) {
        this.date = PlistHelper.getString(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.DATE);
        this.image = PlistHelper.getString(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.IMAGE);
        this.imageHash = PlistHelper.getString(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.IMAGEHASH);
        this.link = PlistHelper.getString(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.LINK);
        this.fileHash = PlistHelper.getString(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.FILEHASH);
        this.len = PlistHelper.getInt(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.LEN);
        this.lastModified = PlistHelper.getInt(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.LASTMODIFIED);
        this.bookId = PlistHelper.getString(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.BOOKID);
        this.demo = PlistHelper.getBoolean(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.ISDEMO);
        this.resource = PlistHelper.getString(nsDictionary, de.thecode.android.tazreader.sync.model.Plist.Fields.RESOURCE);
    }

    @NonNull
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

    public String getTitelWithDate(Resources resources) {
        return getTitelWithDate(resources.getString(R.string.string_titel_seperator));
    }



    public String getImage() {
        return image;
    }

    public boolean isDemo() {
        return demo;
    }

    public String getDate() {
        return date;
    }

    public long getDateInMillis() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).parse(getDate())
                                                                     .getTime();
        } catch (ParseException e) {
            return 0;
        }
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

    public void setImage(String image) {
        this.image = image;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void setDemo(boolean isDemo) {
        this.demo = isDemo;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        if (title == null) return "";
        return title;
    }

    public void setPublication(String publication) {
        this.publication = publication;
    }

    public String getPublication() {
        return publication;
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

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }

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

    @Ignore
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

    public void parsePlist(InputStream is) throws IOException, PropertyListFormatException, ParseException,
            ParserConfigurationException, SAXException {
        parsePlist(is, true);
    }

    public void parsePlist(InputStream is, boolean parseIndex) throws IOException, PropertyListFormatException, ParseException,
            ParserConfigurationException, SAXException {
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

        private List<Source>          sources;
        private List<TopLink>         toplinks;
        private Map<String, ITocItem> indexMap = new LinkedHashMap<>();

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

        public ITocItem getIndexItem(String key) {
            return indexMap.get(key);
        }

        public class Source extends TocItemTemplate {

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
            public ITocItem getIndexParent() {
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
            public List<ITocItem> getIndexChilds() {
                List<ITocItem> resultList = new ArrayList<>();
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

        public class Category extends TocItemTemplate {

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
            public ITocItem getIndexParent() {
                return getBook().getSource();
            }

            @Override
            public boolean hasIndexParent() {
                return true;
            }

            @Override
            public boolean hasIndexChilds() {
                if (pages != null) {
                    return pages.size() > 0;
                }
                return false;
            }

            @Override
            public List<ITocItem> getIndexChilds() {
                List<ITocItem> resultList = new ArrayList<>();
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

        public class Page extends TocItemTemplate {

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
            public ITocItem getIndexParent() {
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
            public List<ITocItem> getIndexChilds() {
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

                    Uri contentUri = StreamProvider.getUriForFile(BuildConfig.APPLICATION_ID + ".streamprovider", pdfFile);

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

                public String getCleanLink() {
                    if (link!=null) return link.replaceFirst("^(\\./)+","");
                    return null;
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

            public class Article extends TocItemTemplate //implements ArticleReaderItem
            {

                private static final String KEY_TITLE      = "Titel";
                private static final String KEY_SUBTITLE   = "Untertitel";
                private static final String KEY_AUTHOR     = "Autor";
                private static final String KEY_ONLINELINK = "OnlineLink";

                private String key;
                private String title;
                private String subtitle;
                private String author;
                private String onlinelink;
                private Page   realPage;

                public Article(String key, NSDictionary dict) {
                    this.key = key;
                    this.title = PlistHelper.getString(dict, KEY_TITLE);
                    this.subtitle = PlistHelper.getString(dict, KEY_SUBTITLE);
                    this.author = PlistHelper.getString(dict, KEY_AUTHOR);
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

                public String getAuthor() {
                    return author;
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
                public ITocItem getIndexParent() {
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
                public List<ITocItem> getIndexChilds() {
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

        public class TopLink extends TocItemTemplate //implements ArticleReaderItem
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
            public ITocItem getIndexParent() {
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
            public List<ITocItem> getIndexChilds() {
                return null;
            }

        }

        public abstract class TocItemTemplate implements ITocItem {

            Type type;

            private boolean  bookmarked;
            private ITocItem link;

            public abstract String getTitle();

            public boolean hasBookmarkedChilds() {
                if (getIndexChilds() != null) {
                    for (ITocItem child : getIndexChilds()) {

                        if (child.isBookmarked()) {
                            return true;
                        } else {
                            if (child.hasBookmarkedChilds()) return true;
                        }
                    }
                }
                return false;
            }

            public ITocItem getIndexAncestorWithKey(String key) {
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

            public ITocItem getLink() {
                return link;
            }

            public boolean isLink() {
                return link != null;
            }

            @Override
            public void setLink(ITocItem link) {
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

    public @NonNull
    JSONArray getBookmarkJson() {
        JSONArray array = new JSONArray();
        try {


            for (Map.Entry<String, ITocItem> entry : getPlist().indexMap.entrySet()) {
                switch (entry.getValue()
                             .getType()) {
                    case ARTICLE:
                        if (entry.getValue()
                                 .isBookmarked()) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Paper)) return false;

        Paper paper = (Paper) o;

        return new EqualsBuilder().appendSuper(super.equals(o))
                                  .append(lastModified, paper.lastModified)
                                  .append(demo, paper.demo)
                                  .append(validUntil, paper.validUntil)
                                  .append(bookId, paper.bookId)
                                  .append(date, paper.date)
                                  .append(image, paper.image)
                                  .append(imageHash, paper.imageHash)
                                  .append(link, paper.link)
                                  .append(resource, paper.resource)
                                  .append(title, paper.title)
                                  .append(publication, paper.publication)
                                  .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode())
                                          .append(bookId)
                                          .append(date)
                                          .append(image)
                                          .append(imageHash)
                                          .append(link)
                                          .append(lastModified)
                                          .append(resource)
                                          .append(demo)
                                          .append(title)
                                          .append(validUntil)
                                          .append(publication)
                                          .toHashCode();
    }

    @Override
    public String toString() {
        //return Log.toString(this, ":", "; ");
        return ToStringBuilder.reflectionToString(this);
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
