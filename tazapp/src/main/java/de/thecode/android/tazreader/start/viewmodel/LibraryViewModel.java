package de.thecode.android.tazreader.start.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;

import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.persistence.room.TazappDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mate on 14.12.2017.
 */

public class LibraryViewModel extends AndroidViewModel {

    private final LiveData<List<Paper>> papers;
    private       boolean               demoMode;
    private MutableLiveData<List<Paper>> selectedPapers = new MutableLiveData<>();

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        demoMode = TazSettings.getInstance(application)
                              .isDemoMode();
        TazSettings.getInstance(application)
                   .addOnPreferenceChangeListener(TazSettings.PREFKEY.DEMOMODE,
                                                  new TazSettings.OnPreferenceChangeListener<Boolean>() {
                                                      @Override
                                                      public void onPreferenceChanged(Boolean changedValue) {
                                                          demoMode = changedValue;
                                                      }
                                                  });
        TazappDatabase database = TazappDatabase.getInstance(application);

        papers = Transformations.map(database.paperDao()
                                             .getPapers(), papers -> {
            if (papers == null) return null;
            List<Paper> result = new ArrayList<>();
            for (Paper paper : papers) {
                if (paper.isDownloaded() || paper.isDownloading() || paper.isKiosk() || paper.isImported()) {
                    result.add(paper);
                } else {
                    if (!(demoMode && !paper.isDemo()) && paper.getValidUntil() >= System.currentTimeMillis() / 1000) {
                        result.add(paper);
                    }

                }
            }
            return result;
        });

    }

    public LiveData<List<Paper>> getPapers() {
        return papers;
    }

    public MutableLiveData<List<Paper>> getSelectedPapers() {
        return selectedPapers;
    }
}
