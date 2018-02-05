package de.thecode.android.tazreader.reader;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModelProvider;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.neu.Plist;
import de.thecode.android.tazreader.reader.index.IIndexItem;
import de.thecode.android.tazreader.room.AppDatabase;
import de.thecode.android.tazreader.room.DataRepository;
import de.thecode.android.tazreader.room.StoreDao;
import de.thecode.android.tazreader.utils.AsyncTaskWithCallback;
import de.thecode.android.tazreader.utils.StorageManager;

import org.json.JSONArray;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by mate on 15.01.2018.
 */

public class ReaderViewModel extends AndroidViewModel {

    private final String bookId;
    private final String resourceKey;
    private       Paper  paper;
    private MutableLiveData<Plist> plistLiveData = new MutableLiveData<>();
    private LiveData<String> currentKeyLiveData;
    private boolean                           filterBookmarks   = false;
    private MutableLiveData<List<IIndexItem>> userIndexLiveData = new MutableLiveData<>();

    public ReaderViewModel(@NonNull Application application, String bookId, String resourceKey) {
        super(application);
        this.bookId = bookId;
        this.resourceKey = resourceKey;
        currentKeyLiveData = AppDatabase.getInstance(application)
                                        .getStoreDao()
                                        .getLiveValueByPath(StoreDao.getPathForKey(bookId, Paper.STORE_KEY_CURRENTPOSITION));

        PlistParsingTask plistParsingTask = new PlistParsingTask(getApplication());
        plistParsingTask.setCallback(new AsyncTaskWithCallback.Callback<Plist>() {
            @Override
            public void onData(Plist plist) {
                makeUserIndex(plist.getUserIndex());
                plistLiveData.setValue(plist);
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception);
            }
        });
        plistParsingTask.execute(bookId);
    }

    public MutableLiveData<List<IIndexItem>> getUserIndexLiveData() {
        return userIndexLiveData;
    }

    private void makeUserIndex(Map<String, IIndexItem> userIndex) {
        UserIndexTask task = new UserIndexTask(userIndex);
        task.setCallback(new AsyncTaskWithCallback.Callback<List<IIndexItem>>() {
            @Override
            public void onData(List<IIndexItem> iIndexItems) {
                ReaderViewModel.this.userIndexLiveData.setValue(iIndexItems);
            }

            @Override
            public void onError(Exception exception) {
                Timber.e(exception);
            }
        });
        task.execute(filterBookmarks);
    }


    public LiveData<String> getCurrentKeyLiveData() {
        return currentKeyLiveData;
    }

    public LiveData<Plist> getPlistLiveData() {
        return plistLiveData;
    }

    public static class UserIndexTask extends AsyncTaskWithCallback<Boolean, Void, List<IIndexItem>> {

        private final Map<String, IIndexItem> userIndex;

        public UserIndexTask(Map<String, IIndexItem> userIndex) {
            this.userIndex = userIndex;
        }

        @Override
        protected List<IIndexItem> doInBackgroundWithException(Boolean... booleans) throws Exception {
            if (booleans != null && booleans.length == 1) {
                boolean filter = booleans[0];
                List<IIndexItem> result = new ArrayList<>();

                for (Map.Entry<String, IIndexItem> entry : userIndex.entrySet()) {
                    IIndexItem item = entry.getValue();
                    if (!filter || item.hasBookmarkedChilds() || item.isBookmarked()) result.add(item);
                }
                return result;
            }
            return null;
        }
    }

    public static class PlistParsingTask extends AsyncTaskWithCallback<String, Void, Plist> {

        StorageManager storageManager;
        StoreDao       storeDao;
        DataRepository dataRepository;

        public PlistParsingTask(Context context) {
            storeDao = AppDatabase.getInstance(context)
                                  .getStoreDao();
            storageManager = StorageManager.getInstance(context);
            dataRepository = DataRepository.getInstance(context);
        }

        @Override
        protected Plist doInBackgroundWithException(String... bookIds) throws Exception {
            if (bookIds != null && bookIds.length == 1) {
                Paper paper = dataRepository.getPaperByBookId(bookIds[0]);
                Plist plist = Plist.parsePlist(new File(storageManager.getPaperDirectory(paper), Plist.CONTENT_PLIST_FILENAME),
                                               paper);
                String bookmarkJsonString = storeDao.getValueByPath(StoreDao.getPathForPaperAndKey(paper,
                                                                                                   Paper.STORE_KEY_BOOKMARKS));
                if (!TextUtils.isEmpty(bookmarkJsonString)) {
                    JSONArray bookmarksJsonArray = new JSONArray(bookmarkJsonString);
                    for (int i = 0; i < bookmarksJsonArray.length(); i++) {
                        IIndexItem item = plist.getIndexItem(bookmarksJsonArray.getString(i));
                        if (item != null) {
                            item.setBookmark(true);
                        }
                    }
                }

                Map<String, Integer> articleCollectionOrder = new HashMap<>();
                Map<Integer, String> articleCollectionPositionIndex = new HashMap<>();

                //Reihenfolge der Artikel festlegen
                List<Plist.TopLink> toplinksToSortIn = new ArrayList<>(plist.getToplinks());
                int position = 0;
                for (Plist.Source source : plist.getSources()) {
                    for (Plist.Book book : source.getBooks()) {
                        for (Plist.Category category : book.getCategories()) {
                            for (Plist.Page page : category.getPages()) {

                                Iterator<Plist.TopLink> i = toplinksToSortIn.iterator();


                                while (i.hasNext()) {
                                    Plist.TopLink topLink = i.next();
                                    if (!topLink.isLink()) {
                                        if (topLink.getKey()
                                                   .equals(page.getDefaultLink())) {
                                            articleCollectionOrder.put(topLink.getKey(), position);
                                            articleCollectionPositionIndex.put(position, topLink.getKey());
                                            position++;
                                            //articleItems.add(topLink);
                                            i.remove();
                                        }
                                    }
                                }
                                for (Plist.Page.Article article : page.getArticles()) {
                                    if (!article.isLink()) {
                                        articleCollectionOrder.put(article.getKey(), position);
                                        articleCollectionPositionIndex.put(position, article.getKey());
                                        position++;
                                        //articleItems.add(article);
                                    }
                                }
                            }
                        }
                    }
                }
                for (Plist.TopLink topLink : toplinksToSortIn) {
                    if (!topLink.isLink()) {
                        articleCollectionOrder.put(topLink.getKey(), position);
                        articleCollectionPositionIndex.put(position, topLink.getKey());
                        position++;
                    }
                }

                //            articleItems.addAll(toplinksToSortIn);

                plist.setArticleCollectionOrder(articleCollectionOrder);
                plist.setArticleCollectionPositionIndex(articleCollectionPositionIndex);


                //Index erstellen für Inhaltsverzeichnis
                Map<String, IIndexItem> userIndex = new LinkedHashMap<>();
                for (Plist.Source source : plist.getSources()) {
                    //index.add(source);
                    for (Plist.Book book : source.getBooks()) {
                        for (Plist.Category category : book.getCategories()) {
                            userIndex.put(category.getKey(), category);
                            if (category.hasIndexChilds()) {
                                for (IIndexItem categoryChild : category.getIndexChilds()) {
                                    userIndex.put(categoryChild.getKey(), categoryChild);
                                }
                            }
                        }
                    }
                }

                for (Plist.TopLink toplink : plist.getToplinks()) {
                    userIndex.put("toplink_" + toplink.getKey(), toplink);
                }
                plist.setUserIndex(userIndex);


                return plist;
            }
            return null;
        }
    }

    public static class ReaderViewModelFactory implements ViewModelProvider.Factory {

        private final String      bookId;
        private final String      resourceKey;
        private final Application application;

        public ReaderViewModelFactory(Application application, String bookId, String resourceKey) {
            this.application = application;
            this.bookId = bookId;
            this.resourceKey = resourceKey;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public ReaderViewModel create(@NonNull Class modelClass) {
            return new ReaderViewModel(application, bookId, resourceKey);
        }
    }
}
