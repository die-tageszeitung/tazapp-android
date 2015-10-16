package de.thecode.android.tazreader.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import de.thecode.android.tazreader.utils.Log;

public class AuthenticatorService extends Service {

    // Instance field that stores the authenticator object
    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        Log.v();
        mAuthenticator = new Authenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.v();
        return mAuthenticator.getIBinder();
    }
    
    

}
