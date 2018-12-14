package de.thecode.android.tazreader.download;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;

import com.novoda.downloadmanager.DownloadBatchRequirementRule;
import com.novoda.downloadmanager.DownloadBatchStatus;
import com.novoda.downloadmanager.DownloadManager;
import com.novoda.downloadmanager.DownloadManagerBuilder;
import com.novoda.downloadmanager.HttpClient;
import com.novoda.downloadmanager.LogHandle;
import com.novoda.downloadmanager.NetworkRequest;
import com.novoda.downloadmanager.NetworkResponse;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.okhttp3.OkHttp3Helper;
import de.thecode.android.tazreader.sync.AccountHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class NewDownloadManagerHelper {

    private static volatile NewDownloadManagerHelper mInstance;

    public static NewDownloadManagerHelper getInstance(Context context) {
        if (mInstance == null) {
            synchronized (NewDownloadManagerHelper.class) {
                if (mInstance == null) {
                    mInstance = new NewDownloadManagerHelper(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private DownloadManager downloadManager;

    private NewDownloadManagerHelper(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        downloadManager = DownloadManagerBuilder.newInstance(context, handler, R.drawable.ic_today_24dp)
                                                .withCustomHttpClient(new DownloadHttpClient(context))
                                                .withLogHandle(new DownloadLogHandle())
                                                .build();

    }


    private class DownloadHttpClient implements HttpClient {

        private AccountHelper accountHelper;
        private OkHttp3Helper okHttp3Helper;
        private OkHttpClient  okHttpClient;

        DownloadHttpClient(Context context) {
            okHttp3Helper = OkHttp3Helper.getInstance(context);
            accountHelper = AccountHelper.getInstance(context);
            accountHelper.addCredentialsChangedListener(accountHelper -> createOkHttpClient());
            createOkHttpClient();
        }

        private void createOkHttpClient() {
            Timber.d("Creating now OkHttpClient for DownloadManger");
            okHttpClient = okHttp3Helper.getOkHttpClientBuilder(accountHelper.getUser(null), accountHelper.getPassword(null))
                                        .build();
        }

        @Override
        public NetworkResponse execute(NetworkRequest networkRequest) throws IOException {
            Request.Builder requestBuilder = new Request.Builder()
                    .url(networkRequest.url());

            if (networkRequest.method() == NetworkRequest.Method.HEAD) {
                requestBuilder = requestBuilder.head();
            }

            for (Map.Entry<String, String> entry : networkRequest.headers().entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            Call call = okHttpClient.newCall(requestBuilder.build());

            return new DownloadHttpResponse(call.execute());
        }
    }

    private class DownloadHttpResponse implements NetworkResponse {
        private final Response response;

        DownloadHttpResponse(Response response) {
            this.response = response;
        }

        @Override
        public int code() {
            return response.code();
        }

        @Override
        public boolean isSuccessful() {
            return response.isSuccessful();
        }

        @Override
        public String header(String name, String defaultValue) {
            return response.header(name, defaultValue);
        }

        @Override
        public InputStream openByteStream() throws IOException {
            return response.body()
                           .byteStream();
        }

        @Override
        public void closeByteStream() throws IOException {
            response.body()
                    .close();
        }

        @Override
        public long bodyContentLength() {
            return response.body()
                           .contentLength();
        }
    }


    private class DownloadLogHandle implements LogHandle {

        @Override
        public void v(Object... message) {
            Timber.v(formatString(message));
        }

        @Override
        public void i(Object... message) {
            Timber.i(formatString(message));
        }

        @Override
        public void d(Object... message) {
            Timber.d(formatString(message));
        }

        @Override
        public void d(Throwable throwable, Object... message) {
            Timber.d(throwable, formatString(message));
        }

        @Override
        public void w(Object... message) {
            Timber.w(formatString(message));
        }

        @Override
        public void w(Throwable throwable, Object... message) {
            Timber.i(throwable, formatString(message));
        }

        @Override
        public void e(Object... message) {
            Timber.e(formatString(message));
        }

        @Override
        public void e(Throwable throwable, Object... message) {
            Timber.e(throwable, formatString(message));
        }


        private String formatString(Object... msg) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Object o : msg) {
                stringBuilder.append(String.valueOf(o))
                             .append(" ");
            }
            return stringBuilder.toString();
        }
    }
}
