package de.thecode.android.tazreader.reader;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.StoreRepository;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.reader.usertoc.UserTocItem;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by mate on 01.03.18.
 */

public class ReaderViewModel extends AndroidViewModel {

    private StoreRepository storeRepository;

    private Resource      resource;
    private Store         currentKeyStore;
    private PaperLiveData paperLiveData;
    private boolean       filterBookmarks;
    private boolean       expanded;
    MutableLiveData<Boolean> indexVerboseLiveData;
    TazSettings              settings;
    private UserTocLiveData           userTocLiveData;
    private MutableLiveData<ITocItem> currentKeyLiveData;
    private PagesTocLiveData          pagesTocLiveData;
    private PagesLiveData pagesLiveData;


    public ReaderViewModel(@NonNull Application application, String bookId, String resourceKey) {
        super(application);
        settings = TazSettings.getInstance(application);
        expanded = settings.isIndexAlwaysExpanded();
        storeRepository = StoreRepository.getInstance(application);
        resource = ResourceRepository.getInstance(application)
                                     .getWithKey(resourceKey);
        currentKeyStore = storeRepository.getStore(bookId, Paper.STORE_KEY_CURRENTPOSITION);
        currentKeyLiveData = new MutableLiveData<>();
        userTocLiveData = new UserTocLiveData();
        pagesTocLiveData = new PagesTocLiveData();
        pagesLiveData = new PagesLiveData();
        paperLiveData = new PaperLiveData(application, bookId);
        paperLiveData.observeForever(new Observer<Paper>() {
            @Override
            public void onChanged(@Nullable Paper paper) {
                pagesLiveData.create();
                userTocLiveData.create();
                pagesTocLiveData.create();
                userTocLiveData.expand(expanded);
                String currentKey = currentKeyStore.getValue(paper.getPlist()
                                                                  .getSources()
                                                                  .get(0)
                                                                  .getBooks()
                                                                  .get(0)
                                                                  .getCategories()
                                                                  .get(0)
                                                                  .getPages()
                                                                  .get(0)
                                                                  .getKey());
                currentKey = StringUtils.substringBefore(currentKey, "?");
                currentKeyLiveData.setValue(paper.getPlist()
                                                 .getIndexItem(currentKey));
            }
        });
        settings = TazSettings.getInstance(application);
        indexVerboseLiveData = new MutableLiveData<>();
        indexVerboseLiveData.setValue(settings.getPrefBoolean(TazSettings.PREFKEY.CONTENTVERBOSE, true));
    }

    public StoreRepository getStoreRepository() {
        return storeRepository;
    }

    public Resource getResource() {
        return resource;
    }

    public PaperLiveData getPaperLiveData() {
        return paperLiveData;
    }

    public Paper getPaper() {
        Paper paper = paperLiveData.getValue();
        if (paper == null) {
            throw new IllegalStateException("paper not loaded, something must be wrong");
        }
        return paper;
    }

    public void setCurrentKey(String currentKey) {
        currentKeyStore.setValue(currentKey);
        storeRepository.saveStore(currentKeyStore);
        if (!expanded) userTocLiveData.expand(false);
        userTocLiveData.expandParent(currentKey);
        userTocLiveData.publish();
        ITocItem item = getPaper().getPlist()
                                  .getIndexItem(currentKey);
        currentKeyLiveData.setValue(item);
    }

    public MutableLiveData<ITocItem> getCurrentKeyLiveData() {
        return currentKeyLiveData;
    }

    public String getCurrentKey() {
        if (currentKeyLiveData.getValue() != null) {
            return currentKeyLiveData.getValue()
                                     .getKey();
        }
        return null;
    }

    public boolean isFilterBookmarks() {
        return filterBookmarks;
    }

    public void setFilterBookmarks(boolean filterBookmarks) {
        if (filterBookmarks == this.filterBookmarks) return;
        this.filterBookmarks = filterBookmarks;
        userTocLiveData.publish();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        settings.setIndexAlwaysExpanded(expanded);
        userTocLiveData.expand(expanded);
        userTocLiveData.publish();
    }

    public static ReaderViewModelFactory createFactory(@NonNull Application application, String bookId, String resourceKey){
        return new ReaderViewModelFactory(application,bookId,resourceKey);
    }

    public static class ReaderViewModelFactory implements ViewModelProvider.Factory {


        private final Application application;
        private final String      bookId;
        private final String      resourceKey;

        /**
         * Creates a {@code AndroidViewModelFactory}
         *
         * @param application an application to pass in {@link AndroidViewModel}
         */
        public ReaderViewModelFactory(@NonNull Application application, String bookId, String resourceKey) {
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

    public MutableLiveData<Boolean> getIndexVerboseLiveData() {
        return indexVerboseLiveData;
    }

    public void setIndexVerbose(boolean indexVerbose) {
        settings.setPref(TazSettings.PREFKEY.CONTENTVERBOSE, indexVerbose);
        indexVerboseLiveData.setValue(indexVerbose);
    }

    public UserTocLiveData getUserTocLiveData() {
        return userTocLiveData;
    }

    public PagesTocLiveData getPagesTocLiveData() {
        return pagesTocLiveData;
    }

    public PagesLiveData getPagesLiveData() {
        return pagesLiveData;
    }

    public class PagesLiveData extends LiveData<List<Paper.Plist.Page>> {
        public void create() {
            List<Paper.Plist.Page> pages = new ArrayList<>();
            for (Paper.Plist.Source source : getPaper().getPlist()
                                                       .getSources()) {
                for (Paper.Plist.Book book : source.getBooks()) {
                    for (Paper.Plist.Category category : book.getCategories()) {
                        pages.addAll(category.getPages());
                    }
                }
            }
            setValue(pages);
        }
    }

    public class PagesTocLiveData extends LiveData<List<ITocItem>> {
        public void create() {
            List<ITocItem> index = new ArrayList<>();
            for (Paper.Plist.Source source : getPaper().getPlist()
                                                       .getSources()) {
                index.add(source);
                for (Paper.Plist.Book book : source.getBooks()) {
                    for (Paper.Plist.Category category : book.getCategories()) {
                        index.addAll(category.getPages());
                    }
                }
            }
            setValue(index);
        }
    }

    public class UserTocLiveData extends LiveData<ArrayList<UserTocItem>> {

        private Map<String,UserTocItem> userTocMap = new LinkedHashMap<>();
        private Map<String,Integer> keyPositionMap = new HashMap<>();

        public void create() {
            Timber.d("creating Index");
            userTocMap.clear();
            for (Paper.Plist.Source source : getPaper().getPlist()
                                                       .getSources()) {
                for (Paper.Plist.Book book : source.getBooks()) {
                    for (Paper.Plist.Category category : book.getCategories()) {
                        UserTocItem first = new UserTocItem(null, category);
                        userTocMap.put(first.getKey(),first);
                        if (category.hasIndexChilds()) {
                            for (ITocItem indexItemChild : category.getIndexChilds()) {
                                UserTocItem child = new UserTocItem(first,indexItemChild);
                                userTocMap.put(child.getKey(),child);
                            }
                        }
                    }
                }
            }
            for (ITocItem toplink : getPaper().getPlist()
                                              .getToplinks()) {
                UserTocItem item = new UserTocItem(null, toplink);
                userTocMap.put("toplink_"+item.getKey(),item);
            }
        }

        public void expand(boolean expand) {
            for (Map.Entry<String, UserTocItem> tocMapEntry: userTocMap.entrySet()) {
                tocMapEntry.getValue().setChildsVisible(expand);
            }
        }

        public void expandParent(String key) {
            ITocItem item = getPaper().getPlist()
                                      .getIndexItem(key);
            if (item != null) {
                ITocItem parent = item.getIndexParent();
                UserTocItem tocItem = userTocMap.get(parent.getKey());
                if (tocItem != null) {
                    tocItem.setChildsVisible(true);
                }
            }
        }

        public void toogleExpantion(String key) {
            UserTocItem tocItem = userTocMap.get(key);
            if (tocItem != null) {
                tocItem.setChildsVisible(!tocItem.areChildsVisible());
            }
        }

        public void publish() {
            keyPositionMap.clear();
            ArrayList<UserTocItem> shownIndex = new ArrayList<>();
            for (Map.Entry<String,UserTocItem> tocItemEntry : userTocMap.entrySet()) {
                UserTocItem tocItem = tocItemEntry.getValue();
                if (tocItem.isVisible() || (filterBookmarks && tocItem.getIndexItem()
                                                                      .hasBookmarkedChilds())) {
                    if (!filterBookmarks || tocItem.getIndexItem()
                                                   .isBookmarked() || tocItem.getIndexItem()
                                                                             .hasBookmarkedChilds()) {
                        keyPositionMap.put(tocItemEntry.getKey(),shownIndex.size());
                        shownIndex.add(tocItem);
                    }
                }
            }
            setValue(shownIndex);
        }

        public int getPositionForKey(String key) {
            Integer result = keyPositionMap.get(key);
            return result != null ? result : -1;
        }

        public UserTocItem getUserTocItemForKey (String key) {
            return userTocMap.get(key);
        }
    }
}
