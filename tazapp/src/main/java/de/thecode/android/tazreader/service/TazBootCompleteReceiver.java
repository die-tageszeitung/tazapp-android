package de.thecode.android.tazreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.sync.SyncHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TazBootCompleteReceiver extends BroadcastReceiver {
	private static final Logger log = LoggerFactory.getLogger(TazBootCompleteReceiver.class);
	@Override
	public void onReceive(Context context, Intent intent) {
		log.info("intent: {}", intent);
		//TODO Check if SyncServceNextRun was in the past, if so then run Service
        if (TazSettings.getInstance(context).getSyncServiceNextRun() < System.currentTimeMillis()) {
            SyncHelper.requestSync(context);
        }

		//TazReaderApplication.checkAutoDownload(context);
		
	}
}
