package de.thecode.android.tazreader.okhttp3;

import java.io.IOException;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.annotations.EverythingIsNonNull;

/**
 * Created by mate on 24.10.2016.
 */

public class HeaderInterceptor implements Interceptor {

    private final Map<String, String> headerMap;

    HeaderInterceptor(Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    @Override
    @EverythingIsNonNull
    public Response intercept(Chain chain) throws IOException {
        Request.Builder requestBuilder = chain.request()
                                              .newBuilder();
        if (headerMap != null) {
            for (final String name : headerMap.keySet()) {
                requestBuilder.addHeader(name, headerMap.get(name));
            }
        }
        Request request = requestBuilder.build();
        return chain.proceed(request);
    }
}
