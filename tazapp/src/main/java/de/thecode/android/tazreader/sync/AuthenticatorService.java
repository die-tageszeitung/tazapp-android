package de.thecode.android.tazreader.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticatorService extends Service {
    private static final Logger log = LoggerFactory.getLogger(AuthenticatorService.class);
    // Instance field that stores the authenticator object
    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        log.trace("");
        mAuthenticator = new Authenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        log.trace("");
        return mAuthenticator.getIBinder();
    }
    
    

}
