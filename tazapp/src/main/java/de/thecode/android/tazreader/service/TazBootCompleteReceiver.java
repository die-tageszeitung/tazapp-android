package de.thecode.android.tazreader.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thecode.android.tazreader.TazReaderApplication;

public class TazBootCompleteReceiver extends BroadcastReceiver {
	private static final Logger log = LoggerFactory.getLogger(TazBootCompleteReceiver.class);
	@Override
	public void onReceive(Context context, Intent intent) {
		log.info("intent: {}", intent);
		TazReaderApplication.checkAutoDownload(context);
		
	}
}
