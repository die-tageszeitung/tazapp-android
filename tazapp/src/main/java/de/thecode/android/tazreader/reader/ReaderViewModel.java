package de.thecode.android.tazreader.reader;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.data.StoreRepository;

/**
 * Created by mate on 01.03.18.
 */

public class ReaderViewModel extends AndroidViewModel {

    private StoreRepository storeRepository;
    private ResourceRepository resourceRepository;

    public ReaderViewModel(@NonNull Application application) {
        super(application);
        storeRepository = StoreRepository.getInstance(application);
        resourceRepository = ResourceRepository.getInstance(application);
    }

    public StoreRepository getStoreRepository() {
        return storeRepository;
    }

    public ResourceRepository getResourceRepository() {
        return resourceRepository;
    }
}
