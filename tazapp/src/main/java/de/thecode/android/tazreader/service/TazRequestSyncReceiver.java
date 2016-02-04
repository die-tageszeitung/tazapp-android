package de.thecode.android.tazreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thecode.android.tazreader.sync.SyncHelper;


public class TazRequestSyncReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(TazRequestSyncReceiver.class);
    @Override
    public void onReceive(Context context, Intent intent) {
        log.trace("");
        SyncHelper.requestManualSync(context);
    }

}
