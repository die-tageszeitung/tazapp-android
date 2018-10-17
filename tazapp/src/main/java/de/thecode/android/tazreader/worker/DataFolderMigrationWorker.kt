package de.thecode.android.tazreader.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.work.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import de.thecode.android.tazreader.BuildConfig
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.TazSettings
import de.thecode.android.tazreader.utils.folderSize
import timber.log.Timber
import java.io.File


class DataFolderMigrationWorker(context: Context, workerParams: WorkerParameters) : LoggingWorker(context, workerParams) {

    val settings: TazSettings by lazy {
        TazSettings.getInstance(applicationContext)
    }

    override fun doBackgroundWork(): Result {

        val newPath = File(inputData.getString(ARG_NEW_PATH))
        val oldPath = File(settings.dataFolderPath)
        Timber.i("Data migration from %s to %s ", oldPath, newPath)
        if (oldPath != newPath) {
            newPath.mkdirs()
            if (newPath.exists()) {
                if (oldPath.exists()) {
                    if (oldPath.folderSize() < newPath.freeSpace - 100 * 1024 * 1024) {
                        newPath.mkdirs()
                        var success = true
                        oldPath.copyRecursively(target = newPath, overwrite = true, onError = { file, ioException ->
                            Timber.e(ioException, "File: %s", file)
                            success = false
                            OnErrorAction.TERMINATE
                        })
                        if (success) {
                            oldPath.deleteRecursively()
                            settings.dataFolderPath = newPath.absolutePath
                            Timber.i("Data migration to %s finished", newPath)
                        } else {
                            newPath.deleteRecursively()
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            kotlin.run {
                                Toast.makeText(applicationContext, applicationContext.getString(R.string.toast_data_folder_migration_not_enough_space, newPath), Toast.LENGTH_LONG)
                                        .show()

                            }
                        }
                    }
                } else {
                    settings.dataFolderPath = newPath.absolutePath
                }
            }
        }
        return Result.SUCCESS
    }

    companion object {

        const val UNIQUE_TAG = BuildConfig.FLAVOR + "_DATA_FOLDER_MIGRATION_UNIQUE"
        private const val ARG_NEW_PATH = "newPath"

        fun scheduleNow(newPath: String) {

            val data = Data.Builder()
                    .putString(ARG_NEW_PATH, newPath)
                    .build()

            val request = OneTimeWorkRequest.Builder(DataFolderMigrationWorker::class.java)
                    .setInputData(data)
                    .build()

            WorkManager.getInstance()
                    .beginUniqueWork(UNIQUE_TAG, ExistingWorkPolicy.APPEND, request)
                    .enqueue()

        }

        fun createWaitDialog(context: Context): MaterialDialog {
            return MaterialDialog(context)
                    .title(res = R.string.dialog_data_folder_migration_title)
                    .cancelOnTouchOutside(false)
                    .cancelable(false)
                    .customView(R.layout.dialog_data_folder_migration)
        }
    }
}