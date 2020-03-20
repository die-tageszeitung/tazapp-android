package de.thecode.android.tazreader.start;

import android.app.Application;
import android.text.TextUtils;

import de.thecode.android.tazreader.data.Download;
import de.thecode.android.tazreader.data.DownloadState;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.PaperWithDownloadState;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.TazDownloadManager;
import de.thecode.android.tazreader.start.library.NewLibraryAdapter;
import de.thecode.android.tazreader.utils.AsyncTaskListener;
import de.thecode.android.tazreader.utils.SingleLiveEvent;
import de.thecode.android.tazreader.utils.StorageManager;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

public class StartViewModel extends AndroidViewModel {


    private final MutableLiveData<Boolean>                      demoModeLiveData            = new MutableLiveData<>();
    private final LiveData<List<PaperWithDownloadState>>        livePapers;
    private final TazSettings                                   settings;
    private final StorageManager                                storageManager;
    private final PaperRepository                               paperRepository;
    private final List<String>                                  downloadQueue               = new ArrayList<>();
    private final SingleLiveEvent<TazDownloadManager.Result>    downloadErrorLiveSingleData = new SingleLiveEvent<>();
    private final List<NavigationDrawerFragment.NavigationItem> navBackstack                = new ArrayList<>();
    private       boolean                                       mobileDownloadAllowed       = false;
    private       String                                        openPaperIdAfterDownload;
    private       boolean                                       openReaderAfterDownload     = false;
                  String                                        resourceKeyWaitingForDownload;
    private       boolean                                       actionMode                  = false;
    private       NewLibraryAdapter.PaperMetaData               paperMetaDataMap            = new NewLibraryAdapter.PaperMetaData();


    private final TazSettings.OnPreferenceChangeListener<Boolean> demoModeListener = demoModeLiveData::setValue;

    public StartViewModel(@NonNull Application application) {
        super(application);
        paperRepository = PaperRepository.getInstance(application);
        storageManager = StorageManager.getInstance(application);
        settings = TazSettings.getInstance(application);
        settings.addDemoModeListener(demoModeListener);
        demoModeLiveData.setValue(settings.isDemoMode());
        LiveData<List<PaperWithDownloadState>> sourceLivePapers = Transformations.switchMap(demoModeLiveData,
                                                                                            demoMode -> demoMode ? paperRepository.getLivePapersForDemoLibrary() : paperRepository.getLivePapersForLibrary());
        livePapers = Transformations.map(sourceLivePapers, this::filterLibraryList);
    }

    private List<PaperWithDownloadState> filterLibraryList(List<PaperWithDownloadState> input) {
        List<PaperWithDownloadState> result = new ArrayList<>();
        for (PaperWithDownloadState paper : input) {
            if ((paper.getValidUntil() >= System.currentTimeMillis() / 1000) || paper.getDownloadState() != DownloadState.NONE)
                result.add(paper);
        }
        return result;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        settings.removeOnPreferenceChangeListener(demoModeListener);
    }

    void addToNavBackstack(NavigationDrawerFragment.NavigationItem item) {
        navBackstack.remove(item);
        navBackstack.add(item);
    }

    List<NavigationDrawerFragment.NavigationItem> getNavBackstack() {
        return navBackstack;
    }

    public LiveData<List<PaperWithDownloadState>> getLivePapers() {
        return livePapers;
    }

    public void setPaperMetaDataMap(NewLibraryAdapter.PaperMetaData paperMetaDataMap) {
        this.paperMetaDataMap = paperMetaDataMap;
    }

    public NewLibraryAdapter.PaperMetaData getPaperMetaDataMap() {
        return paperMetaDataMap;
    }

    List<String> getDownloadQueue() {
        return downloadQueue;
    }

    SingleLiveEvent<TazDownloadManager.Result> getDownloadErrorLiveSingleData() {
        return downloadErrorLiveSingleData;
    }

    void startDownloadQueue() {
        new AsyncTaskListener<>(aVoid -> {
                while (downloadQueue.size() > 0) {
                    String bookId = downloadQueue.get(0);
                    TazDownloadManager.Result result = TazDownloadManager.Companion.getInstance()
                                                                                   .downloadPaper(bookId, false);
                    if (result.getState() != TazDownloadManager.Result.STATE.SUCCESS) {
                        downloadErrorLiveSingleData.postValue(result);
                    }
                    downloadQueue.remove(bookId);
                }
                return null;
            }
        ).execute();
    }

    public PaperRepository getPaperRepository() {
        return paperRepository;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public TazSettings getSettings() {
        return settings;
    }

    boolean isMobileDownloadAllowed() {
        return mobileDownloadAllowed;
    }

    void setMobileDownloadAllowed(boolean mobileDownloadAllowed) {
        this.mobileDownloadAllowed = mobileDownloadAllowed;
    }

    void setOpenPaperIdAfterDownload(String bookId) {
        if (TextUtils.isEmpty(openPaperIdAfterDownload)) {
            openPaperIdAfterDownload = bookId;
            openReaderAfterDownload = true;
        } else {
            openReaderAfterDownload = false;
        }
    }

    String getOpenPaperIdAfterDownload() {
        return openPaperIdAfterDownload;
    }

    public void removeOpenPaperIdAfterDownload() {
        openPaperIdAfterDownload = null;
        openReaderAfterDownload = true;
    }

    boolean isOpenReaderAfterDownload() {
        return openReaderAfterDownload;
    }

    public void deletePaper(String... bookIds) {
        new AsyncTaskListener<String, Void>(bookIdsParam -> {
            List<Paper> papersToDelete = paperRepository.getPapersWithBookId(bookIdsParam);
            for (Paper paperToDelete : papersToDelete) {
                Download download = paperRepository.getDownloadForPaper(paperToDelete.getBookId());
                if (download.getState() == DownloadState.DOWNLOADING) {
                    TazDownloadManager.Companion.getInstance()
                                                .cancelDownload(download.getDownloadManagerId());
                }
                paperRepository.deletePaper(paperToDelete);
            }
            return null;
        }).execute(bookIds);
    }

    public boolean isActionMode() {
        return actionMode;
    }

    public void setActionMode(boolean actionMode) {
        this.actionMode = actionMode;
    }

}
