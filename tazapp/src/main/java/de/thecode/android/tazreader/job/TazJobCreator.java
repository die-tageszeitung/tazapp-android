package de.thecode.android.tazreader.job;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by mate on 25.07.2017.
 */

public class TazJobCreator implements JobCreator {

    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case PushRestApiJob.TAG:
                return new PushRestApiJob();
//            case SyncJob.TAG:
//                return new SyncJob();
//            case AutoDownloadJob.TAG:
//                return new AutoDownloadJob();
            case DownloadFinishedPaperJob.TAG:
                return new DownloadFinishedPaperJob();
            case DownloadFinishedResourceJob.TAG:
                return new DownloadFinishedResourceJob();
//            case DownloadFinishedJob.TAG:
//                return new DownloadFinishedJob();

            default:
                return null;
        }
    }

}
