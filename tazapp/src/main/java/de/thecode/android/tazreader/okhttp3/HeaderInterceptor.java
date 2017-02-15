package de.thecode.android.tazreader.okhttp3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mate on 24.10.2016.
 */

public class HeaderInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(HeaderInterceptor.class);

    private final Map<String, String> headerMap;

    public HeaderInterceptor(Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    @Override
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
