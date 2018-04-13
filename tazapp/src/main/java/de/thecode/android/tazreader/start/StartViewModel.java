package de.thecode.android.tazreader.start;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.start.library.LibraryPaperLiveData;
import de.thecode.android.tazreader.utils.AsyncTaskListener;
import de.thecode.android.tazreader.utils.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;

public class StartViewModel extends AndroidViewModel {

    private final LibraryPaperLiveData libraryPaperLiveData;

    private final MutableLiveData<Boolean> demoModeLiveData = new MutableLiveData<>();
//    private final LiveData<List<Paper>> livePapers;
    private final TazSettings           settings;
    private final PaperRepository paperRepository;
    private final DownloadManager downloadManager;
    private final List<String> downloadQueue = new ArrayList<>();
    private final SingleLiveEvent<DownloadError> downloadErrorLiveSingleData = new SingleLiveEvent<>();
    private final List<NavigationDrawerFragment.NavigationItem> navBackstack = new ArrayList<>();
    private boolean mobileDownloadAllowed = false;
    private String openPaperIdAfterDownload;
    private boolean openReaderAfterDownload = false;
    private String paperWaitingForResource;
    private boolean actionMode = false;

    private final TazSettings.OnPreferenceChangeListener<Boolean> demoModeListener = new TazSettings.OnPreferenceChangeListener<Boolean>() {
        @Override
        public void onPreferenceChanged(Boolean changedValue) {
            demoModeLiveData.setValue(changedValue);
        }
    };

    public StartViewModel(@NonNull Application application) {
        super(application);
        downloadManager = DownloadManager.getInstance(application);
        paperRepository = PaperRepository.getInstance(application);
        settings = TazSettings.getInstance(application);
        settings.addDemoModeListener(demoModeListener);
        demoModeLiveData.setValue(settings.isDemoMode());
        libraryPaperLiveData = new LibraryPaperLiveData(application,demoModeLiveData);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        settings.removeOnPreferenceChangeListener(demoModeListener);
    }

    public void addToNavBackstack(NavigationDrawerFragment.NavigationItem item) {
        if (navBackstack.contains(item)) navBackstack.remove(item);
        navBackstack.add(item);
    }

    public List<NavigationDrawerFragment.NavigationItem> getNavBackstack() {
        return navBackstack;
    }

    public MutableLiveData<Boolean> getDemoModeLiveData() {
        return demoModeLiveData;
    }

    public LibraryPaperLiveData getLibraryPaperLiveData() {
        return libraryPaperLiveData;
    }

    public List<String> getDownloadQueue() {
        return downloadQueue;
    }

    public SingleLiveEvent<DownloadError> getDownloadErrorLiveSingleData() {
        return downloadErrorLiveSingleData;
    }

    public void startDownloadQueue(){
        new AsyncTaskListener<Void,Void>(new AsyncTaskListener.OnExecute<Void, Void>() {
            @Override
            public Void execute(Void... aVoid) {
                while (downloadQueue.size() > 0) {
                    String bookId = downloadQueue.get(0);
                    DownloadManager.DownloadManagerResult result = downloadManager.downloadPaper(bookId,false);
                    if (result.getState() != DownloadManager.DownloadManagerResult.STATE.SUCCESS) {
                        String title = "";
                        Paper paper = paperRepository.getPaperWithBookId(bookId);
                        if (paper != null) title = paper.getTitelWithDate(getApplication().getResources());
                        downloadErrorLiveSingleData.postValue(new DownloadError(title,result.getDetails()));
                    }
                    downloadQueue.remove(bookId);
                }
                return null;
            }
        }).execute();
    }

    public PaperRepository getPaperRepository() {
        return paperRepository;
    }

    public TazSettings getSettings() {
        return settings;
    }

    public boolean isMobileDownloadAllowed() {
        return mobileDownloadAllowed;
    }

    public void setMobileDownloadAllowed(boolean mobileDownloadAllowed) {
        this.mobileDownloadAllowed = mobileDownloadAllowed;
    }

    public void setOpenPaperIdAfterDownload(String bookId) {
        if (TextUtils.isEmpty(openPaperIdAfterDownload)) {
            openPaperIdAfterDownload = bookId;
            openReaderAfterDownload = true;
        } else {
            openReaderAfterDownload = false;
        }
    }

    public String getOpenPaperIdAfterDownload() {
        return openPaperIdAfterDownload;
    }

    public void removeOpenPaperIdAfterDownload() {
        openPaperIdAfterDownload = null;
        openReaderAfterDownload = true;
    }

    public void setPaperWaitingForResource(String bookId) {
        this.paperWaitingForResource = bookId;
    }

    public String getPaperWaitingForResource() {
        return paperWaitingForResource;
    }

    public boolean isOpenReaderAfterDownload() {
        return openReaderAfterDownload;
    }

    public void setOpenReaderAfterDownload(boolean openReaderAfterDownload) {
        this.openReaderAfterDownload = openReaderAfterDownload;
    }

    public boolean isActionMode() {
        return actionMode;
    }

    public void setActionMode(boolean actionMode) {
        this.actionMode = actionMode;
    }

    public static class DownloadError {
        final String title;
        final String details;

        public DownloadError(String title, String details) {
            this.title = title;
            this.details = details;
        }

        public String getTitle() {
            return title;
        }

        public String getDetails() {
            return details;
        }

    }
}
