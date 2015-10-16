package de.thecode.android.tazreader.download;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.common.base.Strings;

import java.io.File;
import java.io.FileNotFoundException;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.Log;

/**
 * Created by mate on 12.02.2015.
 */
public class FinishResourceDownloadService extends IntentService {

    public static final String PARAM_RESOURCE_KEY = "resourceKey";

    public FinishResourceDownloadService() {
        super(FinishResourceDownloadService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String resourceKey = intent.getStringExtra(PARAM_RESOURCE_KEY);
        Log.v(resourceKey);
        if (!Strings.isNullOrEmpty(resourceKey)) {
            final Resource resource = new Resource(getApplicationContext(), resourceKey);
            Log.t("Start service for downloaded resource:",resource);
            if (resource.isDownloading()) {
                StorageManager storage = StorageManager.getInstance(getApplicationContext());
                //new RessourceUnzipTask(getApplicationContext(),storage,resource).execute();

                try {
                    new ResourceFileUnzipTask(resource, storage.getDownloadFile(resource), storage.getResourceDirectory(resource.getKey()), true, true) {

                        Context context;

                        @Override
                        public File doInBackgroundWithException(Object... params) throws Exception {
                            Log.t("... start working in background for resource",getResource());
                            context = (Context) params[0];
                            return super.doInBackgroundWithException(params);
                        }

                        @Override
                        public void onPostError(Exception exception, File sourceZipFile) {
                            Log.e(exception);
                            Log.sendExceptionWithCrashlytics(exception);
                            saveResource(false);
                            EventBus.getDefault().post(new ResourceDownloadEvent(getResource().getKey(),exception));
                        }

                        @Override
                        protected void onPostSuccess(File file) {
                            super.onPostSuccess(file);
                            saveResource(true);
                            Log.t("Finished task after download for resource:", getResource());
                            EventBus.getDefault().post(new ResourceDownloadEvent(getResource().getKey()));
                        }

                        private void saveResource(boolean success) {
                            if (success) {
                                getResource().setDownloadId(0);
                                getResource().setDownloaded(true);
                                context.getContentResolver()
                                       .update(Uri.withAppendedPath(Resource.CONTENT_URI, getResource().getKey()), getResource().getContentValues(), null, null);
                            } else {
                                context.getContentResolver()
                                       .delete(Uri.withAppendedPath(Resource.CONTENT_URI, getResource().getKey()), null, null);
                            }
                        }

                    }.execute(getApplicationContext());
                } catch (FileNotFoundException e) {
                    Log.e(e);
                }

            }
        }
    }
}
