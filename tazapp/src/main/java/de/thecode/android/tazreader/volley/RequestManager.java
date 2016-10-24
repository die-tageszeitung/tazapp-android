package de.thecode.android.tazreader.volley;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.Volley;

/**
 * Created by mate on 03.12.2015.
 */
public class RequestManager {

    private static RequestManager instance;

    private final RequestQueue mRequestQueue;


    private RequestManager(Context context) {
        HttpStack stack = new OkHttp3Stack();
        mRequestQueue = Volley.newRequestQueue(context,stack);
    }


    public static synchronized RequestManager getInstance(Context context) {
        if (instance == null) {
            instance = new RequestManager(context.getApplicationContext());
        }
        return instance;
    }

    public void add(@NonNull Request<?> request) {
        mRequestQueue.add(request);
    }

}
