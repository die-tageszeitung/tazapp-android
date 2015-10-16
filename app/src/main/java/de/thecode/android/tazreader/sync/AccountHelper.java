package de.thecode.android.tazreader.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences.Editor;

import com.crashlytics.android.Crashlytics;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.provider.TazProvider;
import de.thecode.android.tazreader.secure.HashHelper;
import de.thecode.android.tazreader.utils.Log;

public class AccountHelper {

    public static final String ACCOUNT_TYPE = "de.thecode.android.tazreader";

    public static final String ACCOUNT_NAME = "Sync";

    public static final String ACCOUNT_DEMO_USER = "demo";
    public static final String ACCOUNT_DEMO_PASS = "demo";

    public static final String KEY_AUTH = "auth";
    public static final String KEY_USER = "user";

    AccountManager mAccountManager;
    Account mAccount;

    public AccountHelper(Context context) throws CreateAccountException {
        Log.v();
        mAccountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account[] accounts = mAccountManager.getAccountsByType(ACCOUNT_TYPE);

        if (accounts != null) {
            if (accounts.length > 0) {
                mAccount = accounts[0];
                if (ACCOUNT_DEMO_USER.equals(getUser())) setAuthenticated(false);
            } else {
                String user = ACCOUNT_DEMO_USER;
                String pass = ACCOUNT_DEMO_PASS;
                // Migrtion von vorhandenen User-Daten aus Version vor 2
                boolean setAuth = false;
                if (TazSettings.getSharedPreferences(context)
                               .contains("credentialsWorking")) {
                    setAuth = true;
                    Log.d("Migration vorhandener Credentials");
                    user = TazSettings.getDecryptedPrefString(context, "user", AccountHelper.ACCOUNT_DEMO_USER);
                    pass = TazSettings.getDecryptedPrefString(context, "pass", AccountHelper.ACCOUNT_DEMO_PASS);
                    Editor edit = TazSettings.getSharedPreferences(context)
                                             .edit();
                    edit.remove("user");
                    edit.remove("pass");
                    edit.remove("credentialsWorking");
                    edit.commit();
                }
                addAccount(user, pass);
                setAuthenticated(setAuth);
            }
            logUser();
        } else {
            Log.sendExceptionWithCrashlytics(new CreateAccountException("accounts is null"));
        }
    }

    public AccountHelper(Context context, Account account) {
        Log.v();
        mAccountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        mAccount = account;
        if (mAccount != null && ACCOUNT_DEMO_USER.equals(getUser())) {
            setAuthenticated(false);
        }
    }

    private void logUser() {
        try {
            Crashlytics.getInstance().core.setUserIdentifier(HashHelper.getHash(getUser(), HashHelper.UTF_8, HashHelper.SHA_1));
            Crashlytics.getInstance().core.setBool("isAuthenticated", isAuthenticated());
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException ignored) {
        }
    }


    public void setUser(String userName) {
        if (mAccount != null) mAccountManager.setUserData(mAccount, KEY_USER, userName);
    }

    private synchronized boolean addAccount(String username, String password) throws CreateAccountException {
        if (mAccount != null) return false;
        Account newAccount = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
        if (mAccountManager.addAccountExplicitly(newAccount, password, null)) {
            Log.d("addAccountExplicitly");
            mAccount = newAccount;
            setUser(username);
            ContentResolver.setSyncAutomatically(mAccount, TazProvider.AUTHORITY, true);
            // makeAccountUnremovable();
            return true;
        } else {
            Log.e("Fehler bei Anlegen des Accounts");
            throw new CreateAccountException();
            /*
             * The account exists or some other error occurred. Log this, report it, or handle it internally.
             */
        }
    }

    public Account getAccount() {
        return mAccount;
    }

    public String getUser() {
        if (mAccount != null) return mAccountManager.getUserData(mAccount, KEY_USER);
        return null;
    }

    public String getPassword() {
        if (mAccount != null) return mAccountManager.getPassword(mAccount);
        return null;
    }

    public void setPassword(String password) {
        if (mAccount != null) mAccountManager.setPassword(mAccount, password);
    }


    public boolean isAuthenticated() {
        if (mAccount != null) {
            try {
                if ("1".equals(mAccountManager.getUserData(mAccount, KEY_AUTH)) && !ACCOUNT_DEMO_USER.equals(getUser())) return true;
            } catch (IllegalArgumentException e) {
                Log.e(e);
                Log.sendExceptionWithCrashlytics(e);
            }
        } else {
            Log.t(mAccountManager);
            Log.sendExceptionWithCrashlytics(new IllegalStateException("isAuthenticated called, but mAccount is null"));
        }
        return false;
    }

    public void setAuthenticated(boolean auth) {
        if (mAccount != null) {
            if (auth) mAccountManager.setUserData(mAccount, KEY_AUTH, "1");
            else mAccountManager.setUserData(mAccount, KEY_AUTH, "0");
        }
    }

    public class CreateAccountException extends Exception {
        public CreateAccountException() {
        }

        public CreateAccountException(String detailMessage) {
            super(detailMessage);
        }

        public CreateAccountException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public CreateAccountException(Throwable throwable) {
            super(throwable);
        }

        private static final long serialVersionUID = 1L;
    }

}
