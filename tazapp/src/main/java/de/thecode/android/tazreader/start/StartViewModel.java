package de.thecode.android.tazreader.start;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.start.library.LibraryPaperLiveData;

public class StartViewModel extends AndroidViewModel {

    private final LibraryPaperLiveData libraryPaperLiveData;

    private final MutableLiveData<Boolean> demoModeLiveData = new MutableLiveData<>();
//    private final LiveData<List<Paper>> livePapers;
    private final TazSettings           settings;
    private final TazSettings.OnPreferenceChangeListener<Boolean> demoModeListener = new TazSettings.OnPreferenceChangeListener<Boolean>() {
        @Override
        public void onPreferenceChanged(Boolean changedValue) {
            demoModeLiveData.setValue(changedValue);
        }
    };

    public StartViewModel(@NonNull Application application) {
        super(application);
        settings = TazSettings.getInstance(application);
        settings.addDemoModeListener(demoModeListener);
        demoModeLiveData.setValue(settings.isDemoMode());
        libraryPaperLiveData = new LibraryPaperLiveData(application,demoModeLiveData);
//
//
//        AppDatabase appDatabase = AppDatabase.getInstance(application);
//        PaperDao paperDao = appDatabase.paperDao();
////        livePapers = paperDao.getLivePapersForLibrary();
//        livePapers = Transformations.switchMap(demoModeLiveData,demoMode -> {
//            if (demoMode) {
//                return appDatabase.paperDao().getLivePapersForDemoLibrary();
//            }
//            return appDatabase.paperDao().getLivePapersForLibrary();
//        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        settings.removeOnPreferenceChangeListener(demoModeListener);
    }

//    public MutableLiveData<Boolean> getDemoModeLiveData() {
//        return demoModeLiveData;
//    }
//
//    public LiveData<List<Paper>> getLivePapers() {
//        return livePapers;
//    }


    public LibraryPaperLiveData getLibraryPaperLiveData() {
        return libraryPaperLiveData;
    }
}
