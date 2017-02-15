package de.thecode.android.tazreader.retrofit;

import de.thecode.android.tazreader.BuildConfig;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by mate on 07.11.2016.
 */

public interface TazClient {
    @GET(BuildConfig.PLIST)
    Call<String> getIssues(@Query("start") String startDate, @Query("end") String endDate);

    @GET(BuildConfig.CHECKLOGIN)
    Call<ResponseBody> checkLogin();
}
