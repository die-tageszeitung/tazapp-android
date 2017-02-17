package de.thecode.android.tazreader.okhttp3;

import android.content.Context;
import android.os.Build;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.secure.Installation;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Mate on 18.02.2017.
 */

public class UserAgentInterceptor implements Interceptor {

    private static final String USER_AGENT_HEADER_NAME = "User-Agent";
    private final String userAgentHeaderValue;

    public UserAgentInterceptor(Context context) {
        this.userAgentHeaderValue = "Android" + " " + Build.VERSION.RELEASE +
                " (" + BuildConfig.VERSION_CODE + ";" + BuildConfig.VERSION_NAME + ";" +
                Build.BRAND + ";" + Build.MODEL + ") [" + Installation.id(context) + "]";
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request originalRequest = chain.request();
        final Request requestWithUserAgent = originalRequest.newBuilder()
                                                            .removeHeader(USER_AGENT_HEADER_NAME)
                                                            .addHeader(USER_AGENT_HEADER_NAME, userAgentHeaderValue)
                                                            .build();
        return chain.proceed(requestWithUserAgent);
    }

}
