package de.thecode.android.tazreader.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Authenticator extends AbstractAccountAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(Authenticator.class);
    AccountManager mAccountManager;

    // Simple constructor
    public Authenticator(Context context) {
        super(context);
        log.trace("");
        mAccountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
    }

    // Editing properties is not supported
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse r, String s) {
        throw new UnsupportedOperationException();
    }

    // Don't add additional accounts
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse r, String s, String s2, String[] strings, Bundle bundle) throws NetworkErrorException {
        log.trace("");

        Bundle responseBundle = new Bundle();
        responseBundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return responseBundle;

        // return null;
    }

    // Ignore attempts to confirm credentials
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse r, Account account, Bundle bundle) throws NetworkErrorException {
        log.trace("");
        return null;
    }

    // Getting an authentication token is not supported
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse r, Account account, String s, Bundle bundle) throws NetworkErrorException {
        log.trace("");
        throw new UnsupportedOperationException();
    }

    // Getting a label for the auth token is not supported
    @Override
    public String getAuthTokenLabel(String s) {
        log.trace("");
        throw new UnsupportedOperationException();
    }

    // Updating user credentials is not supported
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse r, Account account, String s, Bundle bundle) throws NetworkErrorException {
        log.trace("");
        throw new UnsupportedOperationException();
    }

    // Checking features for the account is not supported
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse r, Account account, String[] strings) throws NetworkErrorException {
        log.trace("");
        throw new UnsupportedOperationException();
    }

    // Account is not allowed to delete
    @NonNull
    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws NetworkErrorException {

        // if ("1".equals(mAccountManager.getUserData(account, AccountHelper.KEY_REMOVABLE)))
        // {
        // Log.d(account.name + "removable");
        // return super.getAccountRemovalAllowed(response, account);
        // }
        // else
        // {
        log.debug("{} not removable",account.name);
        Bundle responseBundle = new Bundle();
        responseBundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return responseBundle;
        // }

    }

}
