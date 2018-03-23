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
import de.thecode.android.tazreader.reader.pagetoc.PageTocLiveData;
import de.thecode.android.tazreader.reader.usertoc.UserTocLiveData;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mate on 01.03.18.
 */

public class ReaderViewModel extends AndroidViewModel {

    private StoreRepository storeRepository;

    private       Resource                  resource;
    private       Store                     currentKeyStore;
    private       PaperLiveData             paperLiveData;
    private       MutableLiveData<Boolean>  indexVerboseLiveData;
    private       TazSettings               settings;
    private       MutableLiveData<ITocItem> currentKeyLiveData;
    private       PagesLiveData             pagesLiveData;
    private final UserTocLiveData           userTocLiveData;
    private final PageTocLiveData           pageTocLiveData;

    public ReaderViewModel(@NonNull Application application, String bookId, String resourceKey) {
        super(application);
        settings = TazSettings.getInstance(application);
        storeRepository = StoreRepository.getInstance(application);
        resource = ResourceRepository.getInstance(application)
                                     .getWithKey(resourceKey);
        currentKeyStore = storeRepository.getStore(bookId, Paper.STORE_KEY_CURRENTPOSITION);
        currentKeyLiveData = new MutableLiveData<>();
        userTocLiveData = new UserTocLiveData(currentKeyLiveData, settings.isIndexAlwaysExpanded());
        pageTocLiveData = new PageTocLiveData(application, currentKeyLiveData);
        pagesLiveData = new PagesLiveData();
        paperLiveData = new PaperLiveData(application, bookId);
        paperLiveData.observeForever(new Observer<Paper>() {
            @Override
            public void onChanged(@Nullable Paper paper) {
                if (paper != null) {
                    userTocLiveData.create(paper.getPlist());
                    pageTocLiveData.create(paper.getPlist());
                    pagesLiveData.create();
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

    public void setExpanded(boolean expanded) {
        settings.setIndexAlwaysExpanded(expanded);
        userTocLiveData.setExpandAll(expanded);
    }

    public static ReaderViewModelFactory createFactory(@NonNull Application application, String bookId, String resourceKey) {
        return new ReaderViewModelFactory(application, bookId, resourceKey);
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

//    public UserTocLiveData getUserTocLiveData() {
//        return userTocLiveData;
//    }

    public UserTocLiveData getUserTocLiveData() {
        return userTocLiveData;
    }

    public PageTocLiveData getPageTocLiveData() {
        return pageTocLiveData;
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
}
