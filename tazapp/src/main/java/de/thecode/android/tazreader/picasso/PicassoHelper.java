package de.thecode.android.tazreader.picasso;

import android.content.Context;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.okhttp3.OkHttp3Helper;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

/**
 * Created by mate on 17.10.2016.
 */

public class PicassoHelper {

    private static final int IMAGES_DISK_USAGE_BYTES = 50 * 1024 * 1024;
    private static final String IMAGES_CACHE_DIR = "picasso";

    public static void initPicasso(Context context) {
        OkHttpClient.Builder picassoClientBuilder = OkHttp3Helper.getInstance(context).getOkHttpClientBuilder();
        picassoClientBuilder.cache(new Cache(new File(context.getExternalCacheDir(), IMAGES_CACHE_DIR), IMAGES_DISK_USAGE_BYTES));
        OkHttpClient picassoClient = picassoClientBuilder.build();

        Picasso.Builder picassoBuilder = new Picasso.Builder(context);
        picassoBuilder.downloader(new OkHttp3Downloader(picassoClient));
        Picasso picasso = picassoBuilder.build();
        picasso.setLoggingEnabled(BuildConfig.DEBUG);
        try {
            Picasso.setSingletonInstance(picasso);
        } catch (IllegalStateException e) {
            //already initialized
        }
    }
}
