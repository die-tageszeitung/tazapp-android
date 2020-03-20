package de.thecode.android.tazreader.dialognew

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.TazSettings
import de.thecode.android.tazreader.utils.FileUtils
import de.thecode.android.tazreader.worker.DataFolderMigrationWorker
import java.io.File


class DataFolderDialog : DialogFragment() {


    companion object {

        fun newInstance(): DataFolderDialog {
            return DataFolderDialog()
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        if (activity != null) {
            val context = activity as Context
            val entryString = context.getString(R.string.dialog_data_folder_entry)
            val settings = TazSettings.getInstance(context)
            val currentPath = File(settings.dataFolderPath)

            val volumes = ArrayList<Volume>()
            val externalFilesDir = context.getExternalFilesDir(null)

            if (externalFilesDir != null) {
                volumes += Volume.fromFile(externalFilesDir)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val externalFilesDirs = context.getExternalFilesDirs(null)
                externalFilesDirs.forEach {
                    if (it != null) {
                        val volume = Volume.fromFile(it)
                        if (!volumes.contains(volume)) volumes += volume
                    }
                }
            }

            val entries = ArrayList<String>()
            var selected = -1
            volumes.forEach {
                if (it.path == currentPath.absolutePath) selected = volumes.indexOf(it)
                entries += entryString.format(it.name, FileUtils.readableSize(it.freeSpace))
            }

            return MaterialDialog(context)
                    .title(R.string.pref_title_storage_folder)
                    .negativeButton()
                    .listItemsSingleChoice(waitForPositiveButton = false, items = entries,initialSelection = selected) { dialog, index, _ ->
                        val selectedVolume = volumes[index]
                        DataFolderMigrationWorker.scheduleNow(newPath = selectedVolume.path)
                        dialog.dismiss()
                    }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    data class Volume(val path: String, val name: String, val freeSpace: Long ) {
        companion object {
            fun fromFile(file: File): Volume {
                return Volume(path = file.absolutePath, name = file.absolutePath.substringBefore("/Android"), freeSpace = file.freeSpace)
            }
        }
    }


}