package de.thecode.android.tazreader.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mate on 03.02.2016.
 */
public class Connection {

    private static final Logger log = LoggerFactory.getLogger(Connection.class);

    // Connection ++++++++++++++++++

    public static final int CONNECTION_NOT_AVAILABLE = -1;
    public static final int CONNECTION_MOBILE_ROAMING = 0;
    public static final int CONNECTION_MOBILE = 1;
    public static final int CONNECTION_FAST = 2;

    public static int getConnectionType(Context context) {
        int result = CONNECTION_NOT_AVAILABLE;
        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null) {
            switch (info.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_BLUETOOTH:
                case ConnectivityManager.TYPE_ETHERNET:
                    result = CONNECTION_FAST;
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    TelephonyManager tmanager = (TelephonyManager) context
                            .getSystemService(Context.TELEPHONY_SERVICE);
                    if (tmanager.isNetworkRoaming())
                        result = CONNECTION_MOBILE_ROAMING;
                    else
                        result = CONNECTION_MOBILE;
                    break;
            }
        }
        log.trace("ConnectionType {}",result);
        return result;
    }
}
