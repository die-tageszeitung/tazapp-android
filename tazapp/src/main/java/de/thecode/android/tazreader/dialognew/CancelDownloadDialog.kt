package de.thecode.android.tazreader.dialognew

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.app
import de.thecode.android.tazreader.data.Paper

class CancelDownloadDialog : DialogFragment() {

    interface CancelDownloadDialogListener {
        fun onCancelDownload(bookId: String)
    }

    companion object {

        const val DIALOG_TAG = "cancelDownloadDialog"
        private const val BOOK_ID = "bookId"
        private const val TITLE = "title"

        fun newInstance(paper: Paper): CancelDownloadDialog {
            val args = Bundle()
            args.putString(BOOK_ID, paper.bookId)
            args.putString(TITLE, paper.getTitelWithDate(app.resources))
            val fragment = CancelDownloadDialog()
            fragment.arguments = args
            return fragment
        }
    }

    var dialog: MaterialDialog? = null


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        context?.let {
            val bookId = arguments!!.getString(BOOK_ID)
            val title = arguments!!.getString(TITLE)
            return MaterialDialog(it).title(R.string.dialog_cancel_download_title)
                    .message(text = app.getString(R.string.dialog_cancel_download_message,title))
                    .positiveButton {
                        (context as CancelDownloadDialogListener).onCancelDownload(bookId!!)
                    }
                    .negativeButton()
        }
        return super.onCreateDialog(savedInstanceState)
    }
}