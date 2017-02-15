package de.thecode.android.tazreader.retrofit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by mate on 15.02.2017.
 */

public abstract class LoginCallback<T> implements Callback<T> {

    private final String username;
    private final String password;

    protected LoginCallback(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        onResponse(call,response,username,password);
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        onFailure(call,t,username,password);
    }

    public abstract void onResponse(Call<T> call, Response<T> response, String username, String password);
    public abstract void onFailure(Call<T> call, Throwable t, String username, String password);
}
