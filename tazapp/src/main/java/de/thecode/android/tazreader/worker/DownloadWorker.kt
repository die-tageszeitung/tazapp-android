package de.thecode.android.tazreader.worker

import android.content.Context
import androidx.work.Result
import androidx.work.WorkerParameters

class DownloadWorker(context: Context, workerParams: WorkerParameters): LoggingWorker(context, workerParams) {

    override fun doBackgroundWork(): Result {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}