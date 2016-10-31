package de.thecode.android.tazreader.start;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Strings;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import de.mateware.dialog.Dialog;
import de.mateware.dialog.DialogIndeterminateProgress;
import de.thecode.android.tazreader.BuildConfig;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.secure.Base64;
import de.thecode.android.tazreader.sync.AccountHelper;
import de.thecode.android.tazreader.utils.BaseFragment;
import de.thecode.android.tazreader.volley.RequestManager;
import de.thecode.android.tazreader.volley.TazStringRequest;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends BaseFragment {

    public static final String DIALOG_CHECK_CREDENTIALS = "checkCrd";
    public static final String DIALOG_ERROR_CREDENTIALS = "errorCrd";
    private EditText editUser;
    private EditText editPass;
    private Button loginButton;
    private Button orderButton;
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
        loginButton = (Button) view.findViewById(R.id.buttonLogin);
        orderButton = (Button) view.findViewById(R.id.buttonOrder);
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

        if (hasCallback() && getCallback().getAccountHelper()
                                          .isAuthenticated()) {
            setUiForLoggedIn();
            editUser.setText(getCallback().getAccountHelper()
                                          .getUser());
            editPass.setText(getCallback().getAccountHelper()
                                          .getPassword());
        } else setUiForNotLoggedIn();

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

    private void setUiForLoggedIn() {
        editUser.setEnabled(false);
        editPass.setEnabled(false);
        loginButton.setText(R.string.string_deleteAccount_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasCallback()) getCallback().getAccountHelper()
                                                .setUser(AccountHelper.ACCOUNT_DEMO_USER);
                if (hasCallback()) getCallback().getAccountHelper()
                                                .setPassword(AccountHelper.ACCOUNT_DEMO_PASS);
                if (hasCallback()) getCallback().getAccountHelper()
                                                .setAuthenticated(false);
                setUiForNotLoggedIn();
                if (hasCallback()) getCallback().logoutFinished();
            }
        });
        orderButton.setVisibility(View.INVISIBLE);
    }

    private void setUiForNotLoggedIn() {
        editUser.setEnabled(true);
        editPass.setEnabled(true);
        loginButton.setText(R.string.string_login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLogin();
            }
        });
        orderButton.setVisibility(View.VISIBLE);
    }

    private void checkLogin() {
        if (Strings.isNullOrEmpty(editUser.getText()
                                          .toString()) || Strings.isNullOrEmpty(editPass.getText()
                                                                                        .toString())) {
            new Dialog.Builder().setIcon(R.drawable.ic_alerts_and_states_warning)
                                .setTitle(R.string.dialog_error_title)
                                .setMessage(R.string.dialog_error_no_credentials)
                                .setPositiveButton()
                                .buildSupport()
                                .show(getFragmentManager(), DIALOG_ERROR_CREDENTIALS);
            return;
        }
        if (AccountHelper.ACCOUNT_DEMO_USER.equalsIgnoreCase(editUser.getText()
                                                                     .toString())) {
            new Dialog.Builder().setIcon(R.drawable.ic_alerts_and_states_warning)
                                .setTitle(R.string.dialog_error_title)
                                .setMessage(R.string.dialog_error_credentials_not_allowed)
                                .setPositiveButton()
                                .buildSupport()
                                .show(getFragmentManager(), DIALOG_ERROR_CREDENTIALS);
            return;
        }


        blockUi();

        Response.Listener<String> responseListener = new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                unblockUi();
                if (hasCallback()) getCallback().getAccountHelper()
                                                .setUser(editUser.getText()
                                                                 .toString());
                if (hasCallback()) getCallback().getAccountHelper()
                                                .setPassword(editPass.getText()
                                                                     .toString());
                if (hasCallback()) getCallback().getAccountHelper()
                                                .setAuthenticated(true);
                setUiForLoggedIn();
                if (hasCallback()) getCallback().loginFinished();
            }
        };

        TazStringRequest.MyStringErrorListener errorListener = new TazStringRequest.MyStringErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error, final String string) {

                new Dialog.Builder().setIcon(R.drawable.ic_alerts_and_states_warning)
                                    .setTitle(R.string.dialog_error_title)
                                    .setMessage(string)
                                    .setPositiveButton()
                                    .buildSupport()
                                    .show(getFragmentManager(), DIALOG_ERROR_CREDENTIALS);
                unblockUi();
                setUiForNotLoggedIn();
            }
        };

        TazStringRequest stringRequest = new TazStringRequest(Request.Method.GET, BuildConfig.CHECKLOGINURL, responseListener, errorListener) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headerMap = new HashMap<String, String>();
                String credentials = editUser.getText()
                                             .toString() + ":" + editPass.getText()
                                                                         .toString();
                String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                headerMap.put("Authorization", "Basic " + base64EncodedCredentials);
                return headerMap;
            }
        };

        RequestManager.getInstance(getContext())
                      .add(stringRequest);

    }


}
