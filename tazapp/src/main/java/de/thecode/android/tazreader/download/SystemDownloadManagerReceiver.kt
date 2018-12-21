package de.thecode.android.tazreader.download

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.ajalt.timberkt.Timber.d
import com.github.ajalt.timberkt.Timber.w
import de.thecode.android.tazreader.worker.DownloadReceiverWorker

class SystemDownloadManagerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        d { "SystemDownloadManagerReceiver onReceive called with $intent" }
        if (intent != null) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val action = intent.action
            DownloadReceiverWorker.scheduleNow(downloadId,action)
        } else {
            w { "no intent found" }
        }
    }

}