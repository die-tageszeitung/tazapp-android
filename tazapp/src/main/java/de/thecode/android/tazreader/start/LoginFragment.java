package de.thecode.android.tazreader.start;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import de.mateware.dialog.Dialog;
import de.mateware.dialog.DialogIndeterminateProgress;
import de.mateware.snacky.Snacky;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.okhttp3.OkHttp3Helper;
import de.thecode.android.tazreader.okhttp3.RequestHelper;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.utils.RunnableExtended;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends StartBaseFragment {

    private static final String DIALOG_CHECK_CREDENTIALS = "checkCrd";
    private static final String DIALOG_ERROR_CREDENTIALS = "errorCrd";
    private EditText                      editUser;
    private EditText                      editPass;
    private Button                        loginButton;

    public LoginFragment() {
        // Required empty public constructor
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getStartActivity().onUpdateDrawer(this);

        View view = inflater.inflate(R.layout.start_login, container, false);
        loginButton = view.findViewById(R.id.buttonLogin);
        loginButton.setOnClickListener(v -> checkLogin());
        Button orderButton = view.findViewById(R.id.buttonOrder);
        orderButton.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(BuildConfig.ABOURL));
                startActivity(i);
        });

        editUser = view.findViewById(R.id.editUser);
        editPass = view.findViewById(R.id.editPass);
        editPass.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    checkLogin();
                    return true;
                }
                return false;
        });

        editUser.setText(AccountHelper.getInstance(getContext())
                                      .getUser(""));
        editPass.setText(AccountHelper.getInstance(getContext())
                                      .getPassword(""));

        return view;
    }

    private void blockUi(boolean block) {
        editUser.setEnabled(!block);
        editPass.setEnabled(!block);
        loginButton.setEnabled(!block);
    }

    private void showWaitingDialog() {
        new DialogIndeterminateProgress.Builder().setCancelable(false)
                                                 .setMessage(R.string.dialog_check_credentials)
                                                 .buildSupport()
                                                 .show(getFragmentManager(), DIALOG_CHECK_CREDENTIALS);
    }

    private void hideWaitingDialog() {
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


        blockUi(true);
        showWaitingDialog();

        Call call = OkHttp3Helper.getInstance(getContext())
                                 .getCall(HttpUrl.parse(BuildConfig.CHECKLOGINURL),
                                          username,
                                          password,
                                          RequestHelper.getInstance(getContext())
                                                       .getOkhttp3RequestBody());

        call.enqueue(new LoginCallback(username, password) {

            Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onResponse(Call call, Response response, String username, String password) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(new RunnableExtended(username, password) {
                        @Override
                        public void run() {
                            TazSettings.getInstance(getContext())
                                       .setDemoMode(false);
                            AccountHelper.getInstance(getContext())
                                         .setUser((String) getObject(0), (String) getObject(1));
                            hideWaitingDialog();
                            blockUi(false);
                            getStartActivity().onSuccessfulCredentialsCheck();
                            //if (hasCallback()) getCallback().onDemoModeChanged(false);
                            Snacky.builder()
                                  .setView(getActivity().findViewById(R.id.content_frame))
                                  .setDuration(Snacky.LENGTH_SHORT)
                                  .setText("Nutzerdaten akzeptiert")
                                  .success()
                                  .addCallback(new Snackbar.Callback() {
                                      @Override
                                      public void onDismissed(Snackbar transientBottomBar, int event) {
                                      }
                                  })
                                  .show();
                        }
                    });
                } else {
                    onFailure(call,
                              new IOException(response.body()
                                                      .string()));
                }
            }

            @Override
            public void onFailure(Call call, IOException e, String username, String password) {
                mainHandler.post(new RunnableExtended(e) {
                    @Override
                    public void run() {
                        TazSettings.getInstance(getContext())
                                   .setDemoMode(true);
                        hideWaitingDialog();
                        Snacky.builder()
                              .setView(getActivity().findViewById(R.id.content_frame))
                              .setDuration(Snacky.LENGTH_INDEFINITE)
                              .setText(((Exception) getObject(0)).getMessage())
                              .setActionText(android.R.string.ok)
                              .error()
                              .addCallback(new Snackbar.Callback() {
                                  @Override
                                  public void onDismissed(Snackbar transientBottomBar, int event) {
                                      blockUi(false);
                                  }
                              })
                              .show();
                    }
                });
            }
        });
    }

    private static abstract class LoginCallback implements Callback {

        private final String username;
        private final String password;

        LoginCallback(String username, String password) {
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
}
