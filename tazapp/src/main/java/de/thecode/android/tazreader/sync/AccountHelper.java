package de.thecode.android.tazreader.sync;

import android.content.Context;
import android.content.SharedPreferences.Editor;

import de.thecode.android.tazreader.analytics.AnalyticsWrapper;
import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.secure.Installation;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class AccountHelper {

    public static final String ACCOUNT_DEMO_USER = "demo";
    public static final String ACCOUNT_DEMO_PASS = "demo";

    private static AccountHelper instance;

    public static synchronized AccountHelper getInstance(Context context) {
        if (instance == null) instance = new AccountHelper(context.getApplicationContext());
        return instance;
    }


    private TazSettings                      preferences;
    private String                           cipherPassword;
    private List<CredentialsChangedListener> credentialsChangedListenerList = new ArrayList<>();

    private AccountHelper(Context context) {
        preferences = TazSettings.getInstance(context);
        cipherPassword = Installation.id(context);
        // Migrtion von vorhandenen User-Daten aus Version vor 2
        if (TazSettings.getInstance(context)
                       .getSharedPreferences()
                       .contains("credentialsWorking")) {
            Timber.d("Migration vorhandener Credentials");
            String user = TazSettings.getInstance(context)
                                     .getOldDecryptedPrefString(TazSettings.PREFKEY.USER, AccountHelper.ACCOUNT_DEMO_USER);
            String pass = TazSettings.getInstance(context)
                                     .getOldDecryptedPrefString(TazSettings.PREFKEY.PASS, AccountHelper.ACCOUNT_DEMO_PASS);
            Editor edit = TazSettings.getInstance(context)
                                     .getSharedPreferences()
                                     .edit();
            edit.remove("credentialsWorking");
            edit.apply();
            setUser(user, pass);
        }
    }

    public String getUser(String defValue) {
        return preferences.getDecrytedPrefString(cipherPassword, TazSettings.PREFKEY.USER, defValue);
    }

    public String getPassword(String defValue) {
        return preferences.getDecrytedPrefString(cipherPassword, TazSettings.PREFKEY.PASS, defValue);
    }

    public void setUser(String user, String password) {
        AnalyticsWrapper.getInstance().setUserEncrypted(user);
        preferences.setEncrytedPrefString(cipherPassword, TazSettings.PREFKEY.USER, user);
        preferences.setEncrytedPrefString(cipherPassword, TazSettings.PREFKEY.PASS, password);
//        setAuthenticated(true);

        for (CredentialsChangedListener listener : credentialsChangedListenerList) {
            listener.onCredentialsChanged(this);
        }
    }



    public void addCredentialsChangedListener(CredentialsChangedListener listener) {
        if (!credentialsChangedListenerList.contains(listener)) credentialsChangedListenerList.add(listener);
    }

    public void removeCredentialsChangedListener(CredentialsChangedListener listener) {
        credentialsChangedListenerList.remove(listener);
    }


    public interface CredentialsChangedListener {
        void onCredentialsChanged(AccountHelper accountHelper);
    }
}
