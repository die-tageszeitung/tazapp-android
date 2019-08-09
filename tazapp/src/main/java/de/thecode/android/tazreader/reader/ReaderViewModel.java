package de.thecode.android.tazreader.reader;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.Store;
import de.thecode.android.tazreader.data.StoreRepository;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.data.ITocItem;
import de.thecode.android.tazreader.reader.pagetoc.PageTocLiveData;
import de.thecode.android.tazreader.reader.usertoc.UserTocLiveData;
import de.thecode.android.tazreader.utils.AsyncTaskListener;
import de.thecode.android.tazreader.utils.StorageManager;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mate on 01.03.18.
 */

public class ReaderViewModel extends AndroidViewModel {

    private       StoreRepository           storeRepository;
    private       PaperLiveData             paperLiveData;
    private       MutableLiveData<Boolean>  indexVerboseLiveData;
    private       TazSettings               settings;
    private       MutableLiveData<ITocItem> currentKeyLiveData;
    private       LiveData<String>          currentKeyHelperLiveData;
    private       PagesLiveData             pagesLiveData;
    private final UserTocLiveData           userTocLiveData;
    private final PageTocLiveData           pageTocLiveData;
    private final LiveData<List<Store>>     storeListLiveData;
    private final String                    bookId;
    private final StorageManager            storageManager;


    private ReaderViewModel(@NonNull Application application, String bookId) {
        super(application);
        this.bookId = bookId;
        storageManager = StorageManager.getInstance(application);
        settings = TazSettings.getInstance(application);
        storeRepository = StoreRepository.getInstance(application);
        currentKeyLiveData = new MutableLiveData<>();
        userTocLiveData = new UserTocLiveData(currentKeyLiveData, settings.isIndexAlwaysExpanded());
        pageTocLiveData = new PageTocLiveData(application, currentKeyLiveData);
        pagesLiveData = new PagesLiveData();
        paperLiveData = new PaperLiveData(application, bookId);
        storeListLiveData = storeRepository.getLiveAllStoresForBook(bookId);
        currentKeyHelperLiveData = Transformations.map(storeRepository.getLiveStore(bookId, Paper.STORE_KEY_CURRENTPOSITION),
                                                       store -> {
                                                           String value = null;
                                                           if (store != null) {
                                                               value = store.getValue();
                                                           }
                                                           if (value == null) {
                                                               value = paperLiveData.getValue()
                                                                                    .getPlist()
                                                                                    .getSources()
                                                                                    .get(0)
                                                                                    .getBooks()
                                                                                    .get(0)
                                                                                    .getCategories()
                                                                                    .get(0)
                                                                                    .getPages()
                                                                                    .get(0)
                                                                                    .getKey();
                                                           }
                                                           return value;
                                                       });

        paperLiveData.observeForever(paper -> {
                if (paper != null) {
                    userTocLiveData.create(paper.getPlist());
                    pageTocLiveData.create(paper.getPlist());
                    pagesLiveData.create();
                    currentKeyHelperLiveData.observeForever(s -> {
                            if (currentKeyLiveData.getValue() == null || !currentKeyLiveData.getValue()
                                                                                            .getKey()
                                                                                            .equals(s)) {
                                currentKeyLiveData.setValue(paperLiveData.getValue()
                                                                         .getPlist()
                                                                         .getIndexItem(s));
                            }
                    });
                }
        });
        settings = TazSettings.getInstance(application);
        indexVerboseLiveData = new MutableLiveData<>();
        indexVerboseLiveData.setValue(settings.getPrefBoolean(TazSettings.PREFKEY.CONTENTVERBOSE, true));
    }

    public File getPaperDirectory() {
        return storageManager.getPaperDirectory(paperLiveData.getValue());
    }

    public File getResourceDirectory(){
        return storageManager.getResourceDirectory(paperLiveData.getValue().getResource());
    }

    public StoreRepository getStoreRepository() {
        return storeRepository;
    }

    @WorkerThread
    public Store getStore(String key){
        return getStoreRepository().getStore(bookId,key);
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

    public Resource getResource() {
        return paperLiveData.getResource();
    }

    public void setCurrentKey(String currentKey) {
        if (currentKey != null) currentKey = StringUtils.substringBefore(currentKey, "?");
        new AsyncTaskListener<String, Void>(strings -> {
                storeRepository.saveStore(new Store(Store.getPath(bookId, Paper.STORE_KEY_CURRENTPOSITION), strings[0]));
                return null;
        }).execute(currentKey);
    }

    public LiveData<ITocItem> getCurrentKeyLiveData() {
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

    static ReaderViewModelFactory createFactory(@NonNull Application application, String bookId) {
        return new ReaderViewModelFactory(application, bookId);
    }

    public static class ReaderViewModelFactory implements ViewModelProvider.Factory {


        private final Application application;
        private final String      bookId;

        /**
         * Creates a {@code AndroidViewModelFactory}
         *
         * @param application an application to pass in {@link AndroidViewModel}
         */
        ReaderViewModelFactory(@NonNull Application application, String bookId) {
            this.application = application;
            this.bookId = bookId;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public ReaderViewModel create(@NonNull Class modelClass) {
            return new ReaderViewModel(application, bookId);
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
