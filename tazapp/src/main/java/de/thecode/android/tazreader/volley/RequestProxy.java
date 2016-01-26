package de.thecode.android.tazreader.volley;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mate on 26.01.2016.
 */
public class RequestProxy {

    private static final Logger log = LoggerFactory.getLogger(RequestProxy.class);


    private RequestQueue mRequestQueue;

    RequestProxy(Context context) {
        mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public void cancelAll() {
        mRequestQueue.cancelAll(this);
    }

    public void add(@NonNull Request<?> request) {
        mRequestQueue.add(request);
    }

}
