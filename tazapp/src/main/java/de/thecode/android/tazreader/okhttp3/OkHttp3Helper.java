package de.thecode.android.tazreader.okhttp3;

import android.content.Context;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.utils.BuildTypeProvider;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

/**
 * Created by mate on 17.02.2017.
 */

public class OkHttp3Helper {


    private static volatile OkHttp3Helper mInstance;

    public static OkHttp3Helper getInstance(Context context) {
        if (mInstance == null) {
            synchronized (OkHttp3Helper.class) {
                if (mInstance == null) {
                    mInstance = new OkHttp3Helper(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }


    private final Map<String, String> standardHeaders;

    private final UserAgentInterceptor userAgentInterceptor;
    private final TazSettings          settings;

    private OkHttp3Helper(Context context) {
        standardHeaders =  new HashMap<>();
        userAgentInterceptor = new UserAgentInterceptor(context);
        settings = TazSettings.getInstance(context);
    }


    public OkHttpClient.Builder getOkHttpClientBuilder(String username, String password) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addNetworkInterceptor(new HeaderInterceptor(standardHeaders));
        httpClientBuilder.addNetworkInterceptor(userAgentInterceptor);
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            httpClientBuilder.addInterceptor(new BasicAuthenticationInterceptor(username, password));
        }
        BuildTypeProvider.addStethoInterceptor(httpClientBuilder);
        addLoggingInterceptor(httpClientBuilder);
        return httpClientBuilder;
    }

    private void addLoggingInterceptor(OkHttpClient.Builder builder) {
        if (BuildConfig.DEBUG || settings.isWriteLogfile()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    Timber.d("%s", message);
                }
            });
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addNetworkInterceptor(logging);
        }
    }

    public OkHttpClient.Builder getOkHttpClientBuilder() {
        return getOkHttpClientBuilder(null, null);
    }

    public Call getCall(HttpUrl url, String username, String password, RequestBody requestBody) {


        OkHttpClient client = getOkHttpClientBuilder(username, password).build();
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (requestBody != null) {
            requestBuilder.post(requestBody);
        }
        Request request = requestBuilder.build();
        return client.newCall(request);
    }

    public Call getCall(HttpUrl url, RequestBody requestBody) {
        return getCall(url, null, null, requestBody);
    }

    public Call getCall(HttpUrl url, String username, String password) {
        return getCall(url, username, password, null);
    }

    public Call getCall(HttpUrl url) {
        return getCall(url, null, null, null);
    }
}
