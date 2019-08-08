package de.thecode.android.tazreader.dialognew

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.TazSettings

class AskForHelpDialog : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        context?.let {
            return MaterialDialog(it).title(R.string.dialog_ask_for_help_title)
                    .message(res = R.string.dialog_ask_for_help_message)
                    .positiveButton { _ ->
                        TazSettings.getInstance(it)
                                .crashlyticsAlwaysSend = true
                        TazSettings.getInstance(it).isAskForHelpAllowed = false
                    }
                    .negativeButton(R.string.dialog_button_no) { _ ->
                        TazSettings.getInstance(it).isAskForHelpAllowed = false
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