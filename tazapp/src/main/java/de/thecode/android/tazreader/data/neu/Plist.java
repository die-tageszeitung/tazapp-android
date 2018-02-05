package de.thecode.android.tazreader.data.neu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.reader.index.IIndexItem;
import de.thecode.android.tazreader.utils.PlistHelper;
import de.thecode.android.tazreader.utils.StorageManager;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;

import timber.log.Timber;

/**
 * Created by mate on 15.01.2018.
 */

public class Plist {

    public static final String CONTENT_PLIST_FILENAME = "content.plist";

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

    private List<Plist.Source>  sources;
    private List<Plist.TopLink> toplinks;
    private Map<String, IIndexItem> indexMap = new LinkedHashMap<>();

    private Paper paper;

    private Map<String, Integer> articleCollectionOrder = new HashMap<>();
    private Map<Integer, String> articleCollectionPositionIndex = new HashMap<>();

    Map<String, IIndexItem> userIndex = new LinkedHashMap<>();


    public static Plist parsePlist(File file, Paper paper) throws IOException, PropertyListFormatException, ParseException,
            ParserConfigurationException, SAXException {
        return parsePlist(file, true, paper);
    }

    public static Plist parsePlist(File file, boolean parseIndex, Paper paper) throws ParserConfigurationException, ParseException, SAXException,
            PropertyListFormatException, IOException {
        FileInputStream fis = new FileInputStream(file);
        return parsePlist(fis, parseIndex, paper);
    }

    public static Plist parsePlist(InputStream is, Paper paper) throws ZipException, IOException, PropertyListFormatException, ParseException,
            ParserConfigurationException, SAXException {
        return parsePlist(is, true, paper);
    }

    public static Plist parsePlist(InputStream is, boolean parseIndex, Paper paper) throws ZipException, IOException, PropertyListFormatException,
            ParseException, ParserConfigurationException, SAXException {
        Timber.i("Start parsing Plist - parse Index: %s", parseIndex);
        return new Plist(is, parseIndex, paper);
    }

    private Plist(InputStream is, boolean parseIndex, Paper paper) throws IOException, PropertyListFormatException, ParseException,
            ParserConfigurationException, SAXException {

        this.paper = paper;
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
                    Plist.Source source = new Plist.Source(key, (NSArray) sourceDict.objectForKey(key));
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
                    Plist.TopLink toplink = new Plist.TopLink(key, PlistHelper.getString(toplinkDict, key));
                    boolean foundDefaultPageLink = false;
                    for (Plist.Source source : getSources()) {
                        for (Plist.Book book : source.getBooks()) {
                            for (Plist.Category category : book.getCategories()) {
                                for (Plist.Page page : category.getPages()) {
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

    public List<Plist.Source> getSources() {
        return sources;
    }

    public List<Plist.TopLink> getToplinks() {
        return toplinks;
    }

    public ArrayList<Plist.Page> getAllPages() {
        ArrayList<Plist.Page> result = new ArrayList<>();
        for (Plist.Source aSource : getSources()) {
            for (Plist.Book aBook : aSource.getBooks()) {
                for (Plist.Category aCategory : aBook.getCategories()) {
                    result.addAll(aCategory.getPages());
                }
            }
        }
        return result;
    }

    public IIndexItem getIndexItem(String key) {
        return indexMap.get(key);
    }

    public class Source extends Plist.IndexItemTemplate {

        String                 key;
        NSArray                array;
        List<Plist.Book> books;

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
                    Plist.Book book = new Plist.Book(this, key, (NSArray) sourceBookDict.objectForKey(key));

                    book.parseCategories();
                    books.add(book);
                }
            }
        }

        @Override
        public String getTitle() {
            return getKey();
        }

        public List<Plist.Book> getBooks() {
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
                for (Plist.Book book : getBooks()) {
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
                for (Plist.Book book : getBooks()) {
                    resultList.addAll(book.getCategories());
                }
                return resultList;
            }
            return null;
        }

    }

    public class Book {

        Plist.Source         source;
        String                     key;
        NSArray                    array;
        List<Plist.Category> categories;

        public Book(Plist.Source source, String key, NSArray array) {
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
                    Plist.Category category = new Plist.Category(this, key, (NSArray) sourceBookCategoryDict.objectForKey(key));
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

        public Plist.Source getSource() {
            return source;
        }

        public List<Plist.Category> getCategories() {
            if (categories == null)
                throw new IllegalStateException("Categories not parsed yet, call parseCategeories() before");
            return categories;
        }

    }

    public class Category extends Plist.IndexItemTemplate {

        Plist.Book       book;
        String                 key;
        String                 title;
        NSArray                array;
        List<Plist.Page> pages;

        public Category(Plist.Book book, String key, NSArray array) {
            this.book = book;

            //Da Key nicht einzigartig in PList
            this.key = book.getKey() + "_" + key + "_" + book.getCategories()
                                                             .size();
            this.title = key;

            this.array = array;
        }

        public Plist.Book getBook() {
            return book;
        }

        private void parsePages() {
            pages = new ArrayList<>();
            NSObject[] sourceBookCategoryPages = array.getArray();
            if (sourceBookCategoryPages != null) {
                for (NSObject sourceBookCategoryPage : sourceBookCategoryPages) {
                    NSDictionary sourceBookCategoryPageDict = (NSDictionary) sourceBookCategoryPage;
                    String key = sourceBookCategoryPageDict.allKeys()[0];
                    Plist.Page page = new Plist.Page(this, key, (NSDictionary) sourceBookCategoryPageDict.objectForKey(key));
                    indexMap.put(page.getKey(), page);
                    for (Plist.Page.Article article : page.getArticles()) {
                        indexMap.put(article.getKey(), article);
                    }
                    pages.add(page);
                }
            }
        }

        private void parseRealPagesForArticles() {
            List<Plist.Page.Article> allArticlesFromCategory = new ArrayList<>();
            for (Plist.Page aPage : getPages()) {
                allArticlesFromCategory.addAll(aPage.getArticles());
            }
            for (Plist.Page.Article aArticle : allArticlesFromCategory) {
                for (Plist.Page aPage : getPages()) {
                    for (Plist.Page.Geometry aGeometry : aPage.getGeometries()) {
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

        public List<Plist.Page> getPages() {
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
                for (Plist.Page page : getPages()) {
                    //resultList.add(page);
                    resultList.addAll(page.getArticles());
                }
                return resultList;
            }
            return null;
        }

    }

    public class Page extends Plist.IndexItemTemplate {

        private static final String KEY_GEOMETRY    = "geometry";
        private static final String KEY_PAGINA      = "SeitenNummer";
        private static final String KEY_DEFAULTLINK = "defaultLink";
        private static final String KEY_ARTICLE     = "Artikel";
        private static final String KEY_RIGHT       = "right";
        private static final String KEY_LEFT        = "left";

        Plist.Category category;
        String               key;
        String               pagina;
        String               defaultLink;
        String               left;
        String               right;

        NSArray geometryArray;
        NSArray articleArray;

        List<Plist.Page.Geometry> geometries;
        List<Plist.Page.Article>  articles;

        public Page(Plist.Category category, String key, NSDictionary dict) {
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
                    geometries.add(new Plist.Page.Geometry(sourceBookCategoryPageGeometryDict));
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
                    Plist.Page.Article article = new Plist.Page.Article(key, (NSDictionary) sourceBookCategoryPageArticleDict.get(key));
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


        public List<Plist.Page.Article> getArticles() {
            if (articles == null) throw new IllegalStateException("Articles not parsed yet, call parseArticles() before");
            return articles;
        }

        public List<Plist.Page.Geometry> getGeometries() {
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

        public Plist.Category getCategory() {
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

            File pdfFile = new File(storage.getPaperDirectory(paper), getKey());
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
                intent.putExtra(Intent.EXTRA_SUBJECT, paper.getTitelWithDate(context) + ": " + getTitle());
                intent.putExtra(Intent.EXTRA_TEXT, paper.getTitelWithDate(context) + "\n" + getTitle());

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

            public Plist.Page getPage() {
                return Plist.Page.this;
            }

            public boolean checkCoordinates(float x, float y) {
                return x >= x1 && x <= x2 && y >= y1 && y <= y2;
            }

            //                public Article getArticle() {
            //                    return article;
            //                }
        }

        public class Article extends Plist.IndexItemTemplate //implements ArticleReaderItem
        {

            private static final String KEY_TITLE      = "Titel";
            private static final String KEY_SUBTITLE   = "Untertitel";
            private static final String KEY_ONLINELINK = "OnlineLink";

            private String           key;
            private String           title;
            private String           subtitle;
            private String           onlinelink;
            private Plist.Page realPage;

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

            public Plist.Category getCategory() {
                return category;
            }

            public Plist.Page getRealPage() {
                return realPage;
            }

            public void setRealPage(Plist.Page realPage) {
                this.realPage = realPage;
            }

            public Plist.Page getPage() {
                return Plist.Page.this;
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
                StringBuilder text = new StringBuilder(paper.getTitelWithDate(context)).append("\n")
                                                                                            .append(getTitle())
                                                                                            .append("\n\n");
                if (!TextUtils.isEmpty(getSubtitle())) {
                    text.append(getSubtitle())
                        .append("\n\n");
                }
                text.append(getOnlinelink());


                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, paper.getTitelWithDate(context) + ": " + getTitle());
                intent.putExtra(Intent.EXTRA_TEXT, text.toString());

                return Intent.createChooser(intent, context.getString(R.string.reader_action_share));
            }
        }

    }

    public class TopLink extends Plist.IndexItemTemplate //implements ArticleReaderItem
    {

        private String title;
        private String key;

        private Plist.Page page;

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

        public Plist.Page getPage() {
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
                if (this instanceof Plist.Source) type = Type.SOURCE;
                else if (this instanceof Plist.Category) type = Type.CATEGORY;
                else if (this instanceof Plist.Page) type = Type.PAGE;
                else if (this instanceof Plist.Page.Article) type = Type.ARTICLE;
                else if (this instanceof Plist.TopLink) type = Type.TOPLINK;
                else type = Type.UNKNOWN;
            }
            return type;
        }

        public int getIndexChildCount() {
            if (!hasIndexChilds()) return 0;
            return getIndexChilds().size();
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

    public void setUserIndex(Map<String, IIndexItem> index) {
        this.userIndex = index;
    }

    public Map<String, IIndexItem> getUserIndex() {
        return userIndex;
    }
}
