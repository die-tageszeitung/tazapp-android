package de.thecode.android.tazreader.reporterror

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.TazSettings

class RequestFileLogDialog : DialogFragment() {

    val settings by lazy { TazSettings.getInstance(context) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        context?.let { context ->

            return MaterialDialog(context).message(R.string.error_report_request_log_dialog_message)
                    .positiveButton(R.string.error_report_request_log_ok_without) { ErrorReporter.sendErrorMail(context) }
                    .negativeButton(R.string.error_report_request_log_activate){
                        settings.isWriteLogfile = true
                    }
        }

        return super.onCreateDialog(savedInstanceState)
    }

    companion object {

        const val DIALOG_FILELOG_REQUEST = "fileLogRequest"

        fun newInstance() : RequestFileLogDialog {
            return RequestFileLogDialog()
        }
    }

}