package de.thecode.android.tazreader.start;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import de.mateware.dialog.Dialog;
import de.mateware.dialog.DialogIndeterminateProgress;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.retrofit.LoginCallback;
import de.thecode.android.tazreader.retrofit.RetrofitHelper;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.utils.BaseFragment;

import java.io.IOException;
import java.lang.ref.WeakReference;

import okhttp3.ResponseBody;
import retrofit2.Call;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends BaseFragment {

    public static final String DIALOG_CHECK_CREDENTIALS = "checkCrd";
    public static final String DIALOG_ERROR_CREDENTIALS = "errorCrd";
    private EditText                      editUser;
    private EditText                      editPass;
    private WeakReference<IStartCallback> callback;

    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        callback = new WeakReference<>((IStartCallback) getActivity());
        if (hasCallback()) getCallback().onUpdateDrawer(this);

        View view = inflater.inflate(R.layout.start_login, container, false);
        Button loginButton = (Button) view.findViewById(R.id.buttonLogin);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLogin();
            }
        });
        Button orderButton = (Button) view.findViewById(R.id.buttonOrder);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(BuildConfig.ABOURL));
                startActivity(i);
            }
        });

        editUser = (EditText) view.findViewById(R.id.editUser);
        editPass = (EditText) view.findViewById(R.id.editPass);
        editPass.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    checkLogin();
                    return true;
                }
                return false;
            }
        });

        if (!AccountHelper.getInstance(getContext())
                          .isDemoMode()) {
            editUser.setText(AccountHelper.getInstance(getContext())
                                          .getUser());
            editPass.setText(AccountHelper.getInstance(getContext())
                                          .getPassword());
        }

        return view;
    }

    private boolean hasCallback() {
        return callback.get() != null;
    }

    private IStartCallback getCallback() {
        return callback.get();
    }

    private void blockUi() {
        new DialogIndeterminateProgress.Builder().setCancelable(false)
                                                 .setMessage(R.string.dialog_check_credentials)
                                                 .buildSupport()
                                                 .show(getFragmentManager(), DIALOG_CHECK_CREDENTIALS);
        editUser.setEnabled(false);
        editPass.setEnabled(false);
    }

    private void unblockUi() {
        editUser.setEnabled(true);
        editPass.setEnabled(true);
        DialogIndeterminateProgress.dismissDialog(getFragmentManager(), DIALOG_CHECK_CREDENTIALS);
    }

    private void checkLogin() {

        String username = editUser.getText()
                                  .toString();
        String password = editPass.getText()
                                  .toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            new Dialog.Builder().setIcon(R.drawable.ic_alerts_and_states_warning)
                                .setTitle(R.string.dialog_error_title)
                                .setMessage(R.string.dialog_error_no_credentials)
                                .setPositiveButton()
                                .buildSupport()
                                .show(getFragmentManager(), DIALOG_ERROR_CREDENTIALS);
            return;
        }
        if (AccountHelper.ACCOUNT_DEMO_USER.equalsIgnoreCase(username)) {
            new Dialog.Builder().setIcon(R.drawable.ic_alerts_and_states_warning)
                                .setTitle(R.string.dialog_error_title)
                                .setMessage(R.string.dialog_error_credentials_not_allowed)
                                .setPositiveButton()
                                .buildSupport()
                                .show(getFragmentManager(), DIALOG_ERROR_CREDENTIALS);
            return;
        }


        blockUi();

        Call<ResponseBody> checkLoginCall = RetrofitHelper.getInstance(getContext())
                                                          .createService(username, password)
                                                          .checkLogin();

        checkLoginCall.enqueue(new LoginCallback<ResponseBody>(username, password) {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response, String username,
                                   String password) {
                unblockUi();
                if (response.isSuccessful()) {
                    AccountHelper.getInstance(getContext())
                                 .setUser(username, password);
                    if (hasCallback()) getCallback().loginFinished();
                } else {
                    String message = getString(R.string.dialog_error_unknown);
                    if (response.errorBody() != null) {
                        try {
                            String errorMessage = response.errorBody()
                                                          .string();
                            if (!TextUtils.isEmpty(errorMessage)) message = errorMessage;
                        } catch (IOException ignored) {
                        }
                    }
                    //TODO remove user or not if checked wrong credentials?
                    onFailure(call, new Exception(message), username, password);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t, String username, String password) {
                unblockUi();
                new Dialog.Builder().setIcon(R.drawable.ic_alerts_and_states_warning)
                                    .setTitle(R.string.dialog_error_title)
                                    .setMessage(t.getMessage())
                                    .setPositiveButton()
                                    .buildSupport()
                                    .show(getFragmentManager(), DIALOG_ERROR_CREDENTIALS);
            }
        });
    }


}
