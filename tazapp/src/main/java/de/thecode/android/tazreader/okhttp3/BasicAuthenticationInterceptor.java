package de.thecode.android.tazreader.okhttp3;

import androidx.annotation.NonNull;
import android.util.Base64;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mate on 07.11.2016.
 */

public class BasicAuthenticationInterceptor implements Interceptor {

    private final String basic;

    BasicAuthenticationInterceptor(@NonNull String username, @NonNull String password) {
        String credentials = username + ":" + password;
        basic = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request authorisedRequest = chain.request().newBuilder()

                                         .header("Authorization", basic).build();
        return chain.proceed(authorisedRequest);
    }
}
