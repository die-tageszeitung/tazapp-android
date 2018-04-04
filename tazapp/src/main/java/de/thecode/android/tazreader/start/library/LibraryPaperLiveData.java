package de.thecode.android.tazreader.start.library;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.content.Context;
import android.support.annotation.Nullable;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.room.AppDatabase;
import de.thecode.android.tazreader.utils.ParametrizedRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LibraryPaperLiveData extends MutableLiveData<List<LibraryPaper>> {

    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());

    private final AppDatabase           appDatabase;
    private final LiveData<List<Paper>> livePapers;

    private final List<Paper>           papersData        = new ArrayList<>();
    private final Observer<List<Paper>> livePaperObserver = new Observer<List<Paper>>() {
        @Override
        public void onChanged(@Nullable List<Paper> papers) {
            executor.execute(new ParametrizedRunnable<List<Paper>>() {
                @Override
                public void run(List<Paper> parameter) {
                    papersData.clear();
                    if (parameter != null) papersData.addAll(parameter);
                    publish();
                }
            }.set(papers));
        }
    };


    public LibraryPaperLiveData(Context context, LiveData<Boolean> demoModeLiveData) {
        this.appDatabase = AppDatabase.getInstance(context);
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
        livePapers.observeForever(livePaperObserver);
    }

    @Override
    protected void onInactive() {
        livePapers.removeObserver(livePaperObserver);
    }

    private void publish() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<LibraryPaper> result = new ArrayList<>();

                for (Paper paper : papersData) {
                    if (paper.getValidUntil() >= System.currentTimeMillis() / 1000) {
                        result.add(new LibraryPaper(paper, false, 0));
                    }
                }
                postValue(result);
            }
        });
    }

}
