package de.thecode.android.tazreader.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mate on 01.03.18.
 */

public class ResourceRepository {


    private static volatile ResourceRepository mInstance;


    public static ResourceRepository getInstance(Context context) {
        if (mInstance == null) {
            synchronized (ResourceRepository.class) {
                if (mInstance == null) {
                    mInstance = new ResourceRepository(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    StoreRepository storeRepository;
    ContentResolver contentResolver;

    private ResourceRepository(Context context) {
        storeRepository = StoreRepository.getInstance(context);
        contentResolver = context.getContentResolver();
    }

    public Resource getResourceForPaper(Paper paper) {
        String resource = storeRepository.getStoreForKey(paper.getStorePath(Paper.STORE_KEY_RESOURCE_PARTNER))
                                         .getValue(paper.getResource()); //default value as Fallback
        return getWithKey(resource);
//        if (TextUtils.isEmpty(resource)) resource = getResource(); //Fallback;
//        return Resource.getWithKey(context, resource);
    }

    public Resource getWithKey(String key) {
        Uri resourceUri = Resource.CONTENT_URI.buildUpon()
                                              .appendPath(key)
                                              .build();
        Cursor cursor = contentResolver.query(resourceUri, null, null, null, null);
        try {
            if (cursor.moveToNext()) {
                return new Resource(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    public List<Resource> getAllResources(){
        List<Resource> result = new ArrayList<>();
        Cursor cursor = contentResolver
                               .query(Resource.CONTENT_URI, null, null, null, null);
        try {
            while (cursor.moveToNext()) {
                result.add(new Resource(cursor));
            }
        } finally {
            cursor.close();
        }
        return result;
    }
}
