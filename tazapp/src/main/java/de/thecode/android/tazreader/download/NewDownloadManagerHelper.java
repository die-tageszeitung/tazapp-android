package de.thecode.android.tazreader.download;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.novoda.downloadmanager.Batch;
import com.novoda.downloadmanager.DownloadBatchIdCreator;
import com.novoda.downloadmanager.DownloadBatchStatus;
import com.novoda.downloadmanager.DownloadBatchStatusCallback;
import com.novoda.downloadmanager.DownloadManager;
import com.novoda.downloadmanager.DownloadManagerBuilder;
import com.novoda.downloadmanager.HttpClient;
import com.novoda.downloadmanager.LogHandle;
import com.novoda.downloadmanager.NetworkRequest;
import com.novoda.downloadmanager.NetworkResponse;
import com.novoda.downloadmanager.StorageRoot;

import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.okhttp3.OkHttp3Helper;
import de.thecode.android.tazreader.okhttp3.RequestHelper;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.utils.StorageManager;

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
    private StorageRoot     downloadStorage;
    private RequestHelper   requestHelper;

    private NewDownloadManagerHelper(Context context) {
        requestHelper = RequestHelper.getInstance(context);
        downloadStorage = new DownloadStorage(StorageManager.getInstance(context)
                                                            .getDownloadCache()
                                                            .getAbsolutePath());
        Handler handler = new Handler(Looper.getMainLooper());
        downloadManager = DownloadManagerBuilder.newInstance(context, handler, R.drawable.ic_today_24dp)
                                                .withCustomHttpClient(new DownloadHttpClient(context))
                                                .withLogHandle(new DownloadLogHandle())
                                                .build();
        downloadManager.addDownloadBatchCallback(new DownloadBatchStatusCallback() {
            @Override
            public void onUpdate(DownloadBatchStatus downloadBatchStatus) {
                Timber.d("downloaded: %d", downloadBatchStatus.percentageDownloaded());
            }

        });
    }

    public void startPaperDownload(Paper paper) {
        Uri downloadUri = requestHelper.addToUri(Uri.parse(paper.getLink()));
        Batch downloadBatch = Batch.with(downloadStorage,
                                         DownloadBatchIdCreator.createSanitizedFrom(paper.getBookId()),
                                         paper.getTitle())
                                   .downloadFrom(downloadUri.toString())
                                   .apply()
                                   .build();
        downloadManager.download(downloadBatch);
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
            Request.Builder requestBuilder = new Request.Builder().url(networkRequest.url());

            if (networkRequest.method() == NetworkRequest.Method.HEAD) {
                requestBuilder = requestBuilder.head();
            }

            for (Map.Entry<String, String> entry : networkRequest.headers()
                                                                 .entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }

            Call call = okHttpClient.newCall(requestBuilder.build());

            Response response = call.execute();
            if (!response.isSuccessful()) throw new IOException(response.message());
            return new DownloadHttpResponse(response);
        }
    }

    private static class DownloadStorage implements StorageRoot {

        private final String path;

        public DownloadStorage(String path) {
            this.path = path;
        }

        @Override
        public String path() {
            return path;
        }
    }

    private static class DownloadHttpResponse implements NetworkResponse {
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


    private static class DownloadLogHandle implements LogHandle {

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
