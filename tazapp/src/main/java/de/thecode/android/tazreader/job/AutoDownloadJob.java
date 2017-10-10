package de.thecode.android.tazreader.job;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.TazSettings;

/**
 * Created by mate on 10.10.2017.
 */

public class AutoDownloadJob extends Job {

    public static final String TAG = BuildConfig.FLAVOR + "_autodownload_job";

    private static final String ARG_PAPER_ID = "paper_id";

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        return Result.SUCCESS;
    }

    public static void scheduleJob(@NonNull Paper paper, TazSettings settings) {
        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putLong(ARG_PAPER_ID, paper.getId());

        boolean onlyOnWifi = settings.getPrefBoolean(TazSettings.PREFKEY.AUTOLOAD_WIFI, false);

        new JobRequest.Builder(TAG).setExtras(extras)
                                   .setRequiredNetworkType(onlyOnWifi ? JobRequest.NetworkType.UNMETERED : JobRequest.NetworkType.CONNECTED)
                                   .setRequirementsEnforced(true)
                                   .startNow()
                                   .build()
                                   .schedule();
    }
}
