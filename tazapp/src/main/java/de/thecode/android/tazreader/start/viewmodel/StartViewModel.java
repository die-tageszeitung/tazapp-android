package de.thecode.android.tazreader.start.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

/**
 * Created by mate on 15.12.2017.
 */

public class StartViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean>                   actionMode      = new MutableLiveData<>();
    private MutableLiveData<Class<? extends Fragment>> currentFragment = new MutableLiveData<>();

    public StartViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<Boolean> getActionMode() {
        return actionMode;
    }

    public MutableLiveData<Class<? extends Fragment>> getCurrentFragment() {
        return currentFragment;
    }
}
