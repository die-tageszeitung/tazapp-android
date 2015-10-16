package de.thecode.android.tazreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.sync.SyncHelper;
import de.thecode.android.tazreader.utils.Log;


public class TazRequestSyncReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v();
        SyncHelper.requestManualSync(context);
    }

}
