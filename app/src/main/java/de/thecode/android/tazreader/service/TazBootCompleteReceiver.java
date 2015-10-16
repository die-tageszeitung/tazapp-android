package de.thecode.android.tazreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.thecode.android.tazreader.TazReaderApplication;
import de.thecode.android.tazreader.utils.Log;

public class TazBootCompleteReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i( "onReceive()");
		TazReaderApplication.checkAutoDownload(context);
		
	}
}
