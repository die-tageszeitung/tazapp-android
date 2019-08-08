package de.thecode.android.tazreader.dialognew

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.TazSettings

class AskForHelpDialog : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        context?.let { context ->
            return MaterialDialog(context).title(R.string.dialog_ask_for_help_title)
                    .message(res = R.string.dialog_ask_for_help_message)
                    .positiveButton {
                        TazSettings.getInstance(context)
                                .crashlyticsAlwaysSend = true
                        TazSettings.getInstance(context).isAskForHelpAllowed = false
                    }
                    .negativeButton(R.string.dialog_button_no) {
                        TazSettings.getInstance(context).isAskForHelpAllowed = false
                    }
                    .neutralButton(R.string.dialog_neutral_button_later) {}
        }
        return super.onCreateDialog(savedInstanceState)
    }

    companion object {
        const val TAG = "dialogAskForHelp"
        fun newInstance() : AskForHelpDialog{
            return AskForHelpDialog()
        }
    }

}