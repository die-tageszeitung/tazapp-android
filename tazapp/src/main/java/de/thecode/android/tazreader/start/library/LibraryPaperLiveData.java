package de.thecode.android.tazreader.start.library;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.content.Context;
import android.support.annotation.Nullable;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.download.DownloadManager;
import de.thecode.android.tazreader.download.UnzipProgressEvent;
import de.thecode.android.tazreader.room.AppDatabase;
import de.thecode.android.tazreader.utils.ParametrizedRunnable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class LibraryPaperLiveData extends MutableLiveData<List<LibraryPaper>> {

    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());

    private final AppDatabase           appDatabase;
    private final DownloadManager       downloadManager;
    private final LiveData<List<Paper>> livePapers;
    private final Map<String, Integer> progressMap      = new HashMap<>();
    private final List<Paper>          papersData       = new ArrayList<>();
    private final Map<String, Long>    runningDownloads = new HashMap<>();
    private final List<String>         selected         = new ArrayList<>();

    private final Observer<List<Paper>> livePaperObserver = new Observer<List<Paper>>() {
        @Override
        public void onChanged(@Nullable List<Paper> papers) {
            executor.execute(new ParametrizedRunnable<List<Paper>>() {
                @Override
                public void run(List<Paper> parameter) {
                    papersData.clear();
                    if (parameter != null) {
                        for (Paper paper : parameter) {
                            progressMap.remove(paper.getBookId());
                            if (paper.isDownloaded()) {
                                progressMap.put(paper.getBookId(), 100);
                            } else if (paper.isDownloading()) {
                                runningDownloads.put(paper.getBookId(), paper.getDownloadId());
                            } else {

                            }
                        }
                        papersData.addAll(parameter);
                    }
                    publish();
                    if (!runningDownloads.isEmpty()) requestDownloadUpdate();
                }
            }.set(papers));
        }
    };

    private void requestDownloadUpdate() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final Map<String, Long> runningDownloadsCopy = new HashMap<>(runningDownloads);
                for (Map.Entry<String, Long> runningDownload : runningDownloadsCopy.entrySet()) {
                    DownloadManager.DownloadState state = downloadManager.getDownloadState(runningDownload.getValue());
                    Timber.i("runningDownload %s %s", runningDownload.getKey(), state.getDownloadProgress());
                    switch (state.getStatus()) {
                        case DownloadManager.DownloadState.STATUS_SUCCESSFUL:
                        case DownloadManager.DownloadState.STATUS_NOTFOUND:
                        case DownloadManager.DownloadState.STATUS_FAILED:
                            runningDownloads.remove(runningDownload.getKey());
                            break;
                        default:
                            int oldProgress = -1;
                            if (progressMap.containsKey(runningDownload.getKey()))
                                oldProgress = progressMap.get(runningDownload.getKey());
                            int newProgress = state.getDownloadProgress() / 2;
                            if (oldProgress != newProgress) {
                                progressMap.put(runningDownload.getKey(), newProgress);
                            }
                            break;
                    }

                }
                publish();
                if (!runningDownloads.isEmpty()) requestDownloadUpdate();
            }
        });
    }


    public LibraryPaperLiveData(Context context, LiveData<Boolean> demoModeLiveData) {
        this.appDatabase = AppDatabase.getInstance(context);
        this.downloadManager = DownloadManager.getInstance(context);
        livePapers = Transformations.switchMap(demoModeLiveData, demoMode -> {
            if (demoMode) {
                return appDatabase.paperDao()
                                  .getLivePapersForDemoLibrary();
            }
            return appDatabase.paperDao()
                              .getLivePapersForLibrary();
        });

    }

    @Override
    protected void onActive() {
        EventBus.getDefault()
                .register(this);
        livePapers.observeForever(livePaperObserver);
    }

    @Override
    protected void onInactive() {
        EventBus.getDefault()
                .unregister(this);
        livePapers.removeObserver(livePaperObserver);
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onUnzipProgress(UnzipProgressEvent event) {
        int oldProgress = -1;
        if (progressMap.containsKey(event.getBookId())) oldProgress = progressMap.get(event.getBookId());
        int newProgress = 50 + (event.getProgress() / 2);
        if (oldProgress != newProgress) {
            progressMap.put(event.getBookId(), newProgress);
            publish();
        }
    }

    private void publish() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<LibraryPaper> result = new ArrayList<>();

                for (Paper paper : papersData) {
                    int progress = 0;
                    if (progressMap.containsKey(paper.getBookId())) progress = progressMap.get(paper.getBookId());
                    if (paper.getValidUntil() >= System.currentTimeMillis() / 1000 || paper.isDownloading() || paper.isDownloaded() || paper.isKiosk() || paper.isImported()) {
//                        Timber.d("book: %s progress %d",paper.getBookId(), progress);
                        result.add(new LibraryPaper(paper, selected.contains(paper.getBookId()), progress));
                    }
                }
                postValue(result);
            }
        });
    }

    public void toggleSelection(String bookId) {
        executor.execute(new ParametrizedRunnable<String>() {
            @Override
            public void run(String bookIdParam) {
                toggleSelectionInternal(bookIdParam);
                publish();
            }
        }.set(bookId));
    }

    private void toggleSelectionInternal(String bookId) {
        if (selected.contains(bookId)) selected.remove(bookId);
        else selected.add(bookId);
    }

    public int getSelectionSize() {
        return selected.size();
    }

    public List<String> getSelected() {
        return selected;
    }

    public void invertSelection() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<LibraryPaper> currentList = LibraryPaperLiveData.this.getValue();
                if (currentList != null) {
                    for (LibraryPaper libraryPaper : currentList) {
                        toggleSelectionInternal(libraryPaper.getBookId());
                    }
                    publish();
                }
            }
        });
    }

    public void selectNone() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                selected.clear();
                publish();
            }
        });
    }

    public void selectAll() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<LibraryPaper> currentList = LibraryPaperLiveData.this.getValue();
                if (currentList != null) {
                    for (LibraryPaper libraryPaper : currentList) {
                        selected.add(libraryPaper.getBookId());
                    }
                    publish();
                }
            }
        });
    }

    public void selectNotDownloaded() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<LibraryPaper> currentList = LibraryPaperLiveData.this.getValue();
                if (currentList != null) {
                    for (LibraryPaper libraryPaper : currentList) {
                        if (libraryPaper.getPaper()
                                        .isDownloading() || libraryPaper.getPaper()
                                                                        .isDownloaded())
                            selected.remove(libraryPaper.getBookId());
                        else selected.add(libraryPaper.getBookId());
                    }
                    publish();
                }
            }
        });
    }

}
