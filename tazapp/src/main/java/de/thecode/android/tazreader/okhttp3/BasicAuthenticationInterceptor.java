package de.thecode.android.tazreader.okhttp3;

import android.support.annotation.NonNull;
import android.util.Base64;

import de.thecode.android.tazreader.sync.AccountHelper;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mate on 07.11.2016.
 */

public class BasicAuthenticationInterceptor implements Interceptor {

    //    final String basic;
    private final AccountHelper accountHelper;

    public BasicAuthenticationInterceptor(@NonNull AccountHelper accountHelper) {
        this.accountHelper = accountHelper;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request authorisedRequest = chain.request()
                                         .newBuilder()
                                         .header("Authorization", createBasicAuthValue())
                                         .build();
        return chain.proceed(authorisedRequest);
    }

    private String createBasicAuthValue() {
        String credentials = accountHelper.getUser(AccountHelper.ACCOUNT_DEMO_USER) + ":" + accountHelper.getPassword(
                AccountHelper.ACCOUNT_DEMO_PASS);
        return "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
    }
}
