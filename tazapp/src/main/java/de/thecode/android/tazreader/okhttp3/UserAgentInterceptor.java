package de.thecode.android.tazreader.okhttp3;

import android.content.Context;

import de.thecode.android.tazreader.utils.UserAgentHelper;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.annotations.EverythingIsNonNull;

/**
 * Created by Mate on 18.02.2017.
 */

public class UserAgentInterceptor implements Interceptor {

    private final UserAgentHelper userAgentHelper;

    UserAgentInterceptor(Context context) {
        userAgentHelper = UserAgentHelper.getInstance(context);
    }

    @Override
    @EverythingIsNonNull
    public Response intercept(Chain chain) throws IOException {
        final Request originalRequest = chain.request();
        final Request requestWithUserAgent = originalRequest.newBuilder()
                                                            .removeHeader(UserAgentHelper.USER_AGENT_HEADER_NAME)
                                                            .addHeader(UserAgentHelper.USER_AGENT_HEADER_NAME, userAgentHelper.getUserAgentHeaderValue())
                                                            .build();
        return chain.proceed(requestWithUserAgent);
    }

}
