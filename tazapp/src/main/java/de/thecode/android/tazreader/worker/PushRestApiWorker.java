package de.thecode.android.tazreader.worker;

import android.content.Context;

import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.okhttp3.OkHttp3Helper;
import de.thecode.android.tazreader.okhttp3.RequestHelper;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Response;
import timber.log.Timber;

public class PushRestApiWorker extends LoggingWorker {

    public static final String TAG = "push_rest_api_update_job";

    public PushRestApiWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doBackgroundWork() {
        Timber.d("Running job");
        Call call = OkHttp3Helper.getInstance(getApplicationContext())
                                 .getCall(HttpUrl.parse(BuildConfig.PUSHRESTURL),
                                          RequestHelper.getInstance(getApplicationContext())
                                                       .getOkhttp3RequestBody());
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                TazSettings.getInstance(getApplicationContext())
                           .removeOldToken();
                return Result.success();
            }
            Timber.e(response.body()
                             .string());
        } catch (Exception e) {
            Timber.e(e);
        }
        return Result.retry();
    }


    public static void scheduleNow() {

        Constraints.Builder constraintsBuilder = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED);

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PushRestApiWorker.class)
                                                                              .setConstraints(constraintsBuilder.build())
                                                                              .build();

        WorkManager.getInstance().beginUniqueWork(TAG, ExistingWorkPolicy.REPLACE,request).enqueue();

    }
}
