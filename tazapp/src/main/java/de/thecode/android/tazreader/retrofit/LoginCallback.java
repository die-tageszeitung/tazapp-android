package de.thecode.android.tazreader.retrofit;


import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by mate on 15.02.2017.
 */

public abstract class LoginCallback implements Callback {

    private final String username;
    private final String password;

    protected LoginCallback(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        onResponse(call, response, username, password);
    }

    @Override
    public void onFailure(Call call, IOException e) {
        onFailure(call, e, username, password);
    }

    public abstract void onResponse(Call call, Response response, String username, String password) throws IOException;

    public abstract void onFailure(Call call, IOException e, String username, String password);
}
