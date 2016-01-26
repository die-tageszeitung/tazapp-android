package de.thecode.android.tazreader.volley;

import android.content.Context;

/**
 * Created by mate on 03.12.2015.
 */
public class RequestManager {

    private static RequestManager instance;
    private RequestProxy mRequestProxy;

    private RequestManager(Context context) {
        mRequestProxy = new RequestProxy(context);
    }


    // This method should be called first to do singleton initialization
    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new RequestManager(context);
        }
    }

    public static synchronized RequestManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(RequestManager.class.getSimpleName() +
                    " is not initialized, call init(context) method first.");
        }
        return instance;
    }

    public RequestProxy doRequest() {
        return mRequestProxy;
    }

    public void cancelAll() {
        doRequest().cancelAll();
    }


}
