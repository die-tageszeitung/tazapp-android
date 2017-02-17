package de.thecode.android.tazreader.okhttp3;

import android.content.Context;
import android.text.TextUtils;

import de.thecode.android.tazreader.utils.BuildTypeProvider;

import java.util.Map;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by mate on 17.02.2017.
 */

public class OkHttp3Helper {

    private static OkHttp3Helper       instance;
    private static Map<String, String> standardHeaders;

    public synchronized static OkHttp3Helper getInstance(Context context) {
        if (instance == null) instance = new OkHttp3Helper(context.getApplicationContext());
        return instance;
    }

    private OkHttp3Helper(Context context) {
        standardHeaders = HeaderHelper.getInstance(context)
                                      .getStandardHeader();


    }

    public Call getCall(HttpUrl url, String username, String password) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        BuildTypeProvider.addStethoInterceptor(httpClientBuilder);
        BuildTypeProvider.addLoggingInterceptor(httpClientBuilder);
        httpClientBuilder.addNetworkInterceptor(new HeaderInterceptor(standardHeaders));
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            httpClientBuilder.addInterceptor(new BasicAuthenticationInterceptor(username, password));
        }
        OkHttpClient client = httpClientBuilder.build();
        Request request = new Request.Builder().url(url)
                                               .build();
        return client.newCall(request);
    }

    public Call getCall(HttpUrl url) {
        return getCall(url, null, null);
    }

}
