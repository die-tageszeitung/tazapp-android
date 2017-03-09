package de.thecode.android.tazreader.download;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.dd.plist.PropertyListFormatException;

import de.greenrobot.event.EventBus;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.utils.StorageManager;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import timber.log.Timber;

/**
 * Created by mate on 07.08.2015.
 */
public class DownloadFinishedResourceService extends IntentService {

    public static final String PARAM_RESOURCE_KEY = "resourceKey";

    public DownloadFinishedResourceService() {
        super(DownloadFinishedResourceService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String resourceKey = intent.getStringExtra(PARAM_RESOURCE_KEY);
        Timber.i("Start service after downloaded for resource: %s", resourceKey);
        if (!TextUtils.isEmpty(resourceKey)) {
            Resource resource = new Resource(this, resourceKey);
            Timber.i("%s",resource);
            if (resource.isDownloading()) {
                StorageManager storageManager = StorageManager.getInstance(this);

                try {

                    UnzipResource unzipResource = new UnzipResource(storageManager.getDownloadFile(resource),storageManager.getResourceDirectory(resource.getKey()),true);
                    unzipResource.start();
                    saveResource(resource, null);
                } catch (UnzipCanceledException|IOException | PropertyListFormatException | ParseException | SAXException | ParserConfigurationException e) {
                    saveResource(resource,e);
                }
            }
        }
        Timber.i("Finished service after download for resource: %s", resourceKey);
    }


    private void saveResource(Resource resource, Exception e) {
        if (e == null) {
            resource.setDownloadId(0);
            resource.setDownloaded(true);
            getContentResolver().update(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()), resource.getContentValues(), null, null);
            EventBus.getDefault()
                    .post(new ResourceDownloadEvent(resource.getKey()));
        } else {
            getContentResolver().delete(Uri.withAppendedPath(Resource.CONTENT_URI, resource.getKey()), null, null);
            Timber.e(e);
            //AnalyticsWrapper.getInstance().logException(e);
            EventBus.getDefault()
                    .post(new ResourceDownloadEvent(resource.getKey(), e));
        }
    }
}
