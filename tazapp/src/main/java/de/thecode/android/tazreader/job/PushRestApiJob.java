package de.thecode.android.tazreader.job;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.okhttp3.OkHttp3Helper;
import de.thecode.android.tazreader.okhttp3.RequestHelper;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Response;
import timber.log.Timber;

/**
 * Created by mate on 25.07.2017.
 */

public class PushRestApiJob extends Job {

    public static final String TAG = "push_rest_api_update_job";

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        Timber.d("Running job");
        Call call = OkHttp3Helper.getInstance(getContext())
                                 .getCall(HttpUrl.parse(BuildConfig.PUSHRESTURL),
                                          RequestHelper.getInstance(getContext())
                                                       .getOkhttp3RequestBody());
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                TazSettings.getInstance(getContext())
                           .removeOldToken();
                return Result.SUCCESS;
            }
            Timber.e(response.body()
                             .string());
        } catch (Exception e) {
            Timber.e(e);
        }
        return Result.RESCHEDULE;
    }

    public static void scheduleJob() {
        new JobRequest.Builder(TAG).startNow()
                                   .setBackoffCriteria(4_000, JobRequest.BackoffPolicy.LINEAR)
                                   .setRequiredNetworkType(JobRequest.NetworkType.ANY)
                                   .setUpdateCurrent(true)
                                   .build()
                                   .schedule();
    }
}
