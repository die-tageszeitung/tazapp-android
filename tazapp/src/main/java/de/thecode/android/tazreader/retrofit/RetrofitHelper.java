package de.thecode.android.tazreader.retrofit;

import android.content.Context;
import android.text.TextUtils;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.okhttp3.BasicAuthenticationInterceptor;
import de.thecode.android.tazreader.okhttp3.HeaderHelper;
import de.thecode.android.tazreader.okhttp3.HeaderInterceptor;
import de.thecode.android.tazreader.utils.BuildTypeProvider;

import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Created by mate on 02.11.2016.
 */

public class RetrofitHelper {

    private static RetrofitHelper instance;


    private static Map<String, String> standardHeaders;

    public synchronized static RetrofitHelper getInstance(Context context) {
        if (instance == null)
            instance = new RetrofitHelper(context.getApplicationContext());
        return instance;
    }

    private RetrofitHelper(Context context) {
        standardHeaders = HeaderHelper.getInstance(context)
                                      .getStandardHeader();


    }

    public TazClient createService(String username, String password) {

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        BuildTypeProvider.addStethoInterceptor(httpClientBuilder);
        BuildTypeProvider.addLoggingInterceptor(httpClientBuilder);
        httpClientBuilder.addNetworkInterceptor(new HeaderInterceptor(standardHeaders));

        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            httpClientBuilder.addInterceptor(new BasicAuthenticationInterceptor(username, password));
        }

        Retrofit.Builder builder = new Retrofit.Builder().baseUrl(BuildConfig.BASEURL)
                                                         .addConverterFactory(ScalarsConverterFactory.create());
        Retrofit retrofit = builder.client(httpClientBuilder.build())
                                   .build();
        return retrofit.create(TazClient.class);
    }

    public TazClient createService() {
        return createService(null,null);
    }

}
