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
		log.info("Boot complete ...");
		//TODO Check if SyncServceNextRun was in the past, if so then run Service
        long nextRunAt = TazSettings.getInstance(context).getSyncServiceNextRun();
        if (nextRunAt < System.currentTimeMillis()) {
            log.info("... last Sync should happened in the past, syncing now!");
            SyncHelper.requestSync(context);
        } else {
            log.info("... sync should happen in the future, setting AlarmManager!");
            SyncHelper.setAlarmManager(context,nextRunAt);
        }
		//TazReaderApplication.checkAutoDownload(context);
	}
}
