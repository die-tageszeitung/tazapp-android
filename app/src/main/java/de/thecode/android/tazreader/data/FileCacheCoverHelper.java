package de.thecode.android.tazreader.data;

import de.thecode.android.tazreader.utils.StorageManager;


public class FileCacheCoverHelper extends FileCacheImageHelper {

    public static final String SUB_LIBRARYIMAGES = "library";
    
    public FileCacheCoverHelper(StorageManager storage) {
        super(storage, SUB_LIBRARYIMAGES);
    }

}
