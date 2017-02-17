package de.thecode.android.tazreader.okhttp3;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mate on 24.10.2016.
 */

public class HeaderHelper {

    private static HeaderHelper instance;

    public synchronized static HeaderHelper getInstance(Context context) {
        if (instance == null)
            instance = new HeaderHelper(context.getApplicationContext());
        return instance;
    }

    private HeaderHelper(Context context) {
    }

    public Map<String,String> getStandardHeader() {
        Map<String,String> result = new HashMap<>();
        return result;
    }
}
