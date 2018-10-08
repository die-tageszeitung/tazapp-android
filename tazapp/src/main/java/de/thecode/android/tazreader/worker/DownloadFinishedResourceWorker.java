package de.thecode.android.tazreader.worker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.dd.plist.PropertyListFormatException;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.download.ResourceDownloadEvent;
import de.thecode.android.tazreader.download.UnzipCanceledException;
import de.thecode.android.tazreader.download.UnzipResource;
import de.thecode.android.tazreader.utils.StorageManager;

import org.greenrobot.eventbus.EventBus;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;
import timber.log.Timber;

public class DownloadFinishedResourceWorker extends LoggingWorker {

    public static final  String TAG_PREFIX       = BuildConfig.FLAVOR + "_" + "download_finished_resource_job_";
    private static final String ARG_RESOURCE_KEY = "resource_key";

    private final ResourceRepository resourceRepository;
    private final StorageManager storageManager;

    public DownloadFinishedResourceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        resourceRepository = ResourceRepository.getInstance(context);
        storageManager = StorageManager.getInstance(context);
    }

    @NonNull
    @Override
    public Result doBackgroundWork() {
        String resourceKey = getInputData().getString(ARG_RESOURCE_KEY);
        if (!TextUtils.isEmpty(resourceKey)) {
            Resource resource = resourceRepository.getWithKey(resourceKey);
            Timber.i("%s", resource);
            if (resource != null && resource.isDownloading()) {

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
        resource.setDownloadId(0);

        if (e == null) {
            resource.setDownloaded(true);
        } else {
            Timber.e(e);
        }
        resourceRepository.saveResource(resource);
        EventBus.getDefault()
                .post(new ResourceDownloadEvent(resource.getKey(), e));
    }


    public static String getTag(@NonNull String resourceKey) {
        return TAG_PREFIX + resourceKey;
    }

    public static void scheduleNow(Resource resource) {
        String tag = getTag(resource.getKey());

        Data data = new Data.Builder().putString(ARG_RESOURCE_KEY, resource.getKey())
                                      .build();

        WorkRequest request = new OneTimeWorkRequest.Builder(DownloadFinishedResourceWorker.class).setInputData(data)
                                                                                                  .addTag(tag)
                                                                                                  .addTag(resource.getKey())
                                                                                                  .build();

        WorkManager.getInstance()
                   .enqueue(request);
    }
}
