package de.thecode.android.tazreader.job;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.dd.plist.PropertyListFormatException;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.download.ResourceDownloadEvent;
import de.thecode.android.tazreader.download.UnzipCanceledException;
import de.thecode.android.tazreader.download.UnzipResource;
import de.thecode.android.tazreader.utils.StorageManager;

import org.greenrobot.eventbus.EventBus;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import timber.log.Timber;

/**
 * Created by mate on 11.10.2017.
 */

public class DownloadFinishedResourceJob extends Job {

    public static final  String TAG              = BuildConfig.FLAVOR + "_" + "download_finished_resource_job";
    private static final String ARG_RESOURCE_KEY = "resource_key";


    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        PersistableBundleCompat extras = params.getExtras();
        String resourceKey = extras.getString(ARG_RESOURCE_KEY, null);
        if (!TextUtils.isEmpty(resourceKey)) {
            Resource resource = Resource.getWithKey(getContext(), resourceKey);
            Timber.i("%s", resource);
            if (resource != null && resource.isDownloading()) {
                StorageManager storageManager = StorageManager.getInstance(getContext());

                try {

                    UnzipResource unzipResource = new UnzipResource(storageManager.getDownloadFile(resource),
                                                                    storageManager.getResourceDirectory(resource.getKey()),
                                                                    true);
                    unzipResource.start();
                    saveResource(resource, null);
                } catch (UnzipCanceledException | IOException | PropertyListFormatException | ParseException | SAXException | ParserConfigurationException e) {
                    saveResource(resource, e);
                }
            }
        }

        return Result.SUCCESS;
    }

    private void saveResource(Resource resource, Exception e) {
        if (e == null) {
            resource.setDownloadId(0);
            resource.setDownloaded(true);
            getContext().getContentResolver()
                        .update(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()),
                                resource.getContentValues(),
                                null,
                                null);
            EventBus.getDefault()
                    .post(new ResourceDownloadEvent(resource.getKey()));
        } else {
            getContext().getContentResolver()
                        .delete(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()), null, null);
            Timber.e(e);
            //AnalyticsWrapper.getInstance().logException(e);
            EventBus.getDefault()
                    .post(new ResourceDownloadEvent(resource.getKey(), e));
        }
    }

    public static void scheduleJob(Resource resource) {
        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putString(ARG_RESOURCE_KEY, resource.getKey());

        new JobRequest.Builder(TAG).setExtras(extras)
                                   .startNow()
                                   .build()
                                   .schedule();
    }

}
