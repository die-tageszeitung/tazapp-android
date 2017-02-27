package de.thecode.android.tazreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.data.TazSettings;
import de.thecode.android.tazreader.sync.SyncHelper;

import timber.log.Timber;

public class TazBootCompleteReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Timber.i("Boot complete ...");
		//TODO Check if SyncServceNextRun was in the past, if so then run Service
        long nextRunAt = TazSettings.getInstance(context).getSyncServiceNextRun();
        if (nextRunAt < System.currentTimeMillis()) {
            Timber.i("... last Sync should happened in the past, syncing now!");
            SyncHelper.requestSync(context);
        } else {
            Timber.i("... sync should happen in the future, setting AlarmManager!");
            SyncHelper.setAlarmManager(context,nextRunAt);
        }
		//TazReaderApplication.checkAutoDownload(context);
	}
}
