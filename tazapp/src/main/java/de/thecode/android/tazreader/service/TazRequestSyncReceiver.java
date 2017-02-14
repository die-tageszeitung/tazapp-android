package de.thecode.android.tazreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.sync.SyncHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TazRequestSyncReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(TazRequestSyncReceiver.class);
    @Override
    public void onReceive(Context context, Intent intent) {
        log.trace("");
        SyncHelper.requestSync(context);
    }

}
