package de.thecode.android.tazreader.dialognew

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.github.ajalt.timberkt.d
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.Download
import de.thecode.android.tazreader.data.DownloadState
import de.thecode.android.tazreader.data.Paper
import de.thecode.android.tazreader.paperRepository
import de.thecode.android.tazreader.utils.Connection
import de.thecode.android.tazreader.utils.ConnectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CancelDownloadDialog : DialogFragment() {

    interface CancelDownloadDialogListener {
        fun onCancelDownload(bookId: String)
    }

    companion object {

        const val DIALOG_TAG = "cancelDownloadDialog"
        private const val BOOK_ID = "bookId"

        fun newInstance(paper: Paper): CancelDownloadDialog {
            val args = Bundle()
            args.putString(BOOK_ID, paper.bookId)
            val fragment = CancelDownloadDialog()
            fragment.arguments = args
            return fragment
        }
    }

    val bookId: String by lazy {
        arguments!!.getString(BOOK_ID)
    }

    val dialog: MaterialDialog by lazy {
        MaterialDialog(context!!).title(R.string.dialog_cancel_download_title)
                .customView(viewRes = R.layout.dialog_download_info,scrollable = true)
                .positiveButton()
    }
    private val fileName: TextView by lazy {
        dialog.getCustomView().findViewById<TextView>(R.id.fileName)
    }
    private val connection: TextView by lazy {
        dialog.getCustomView().findViewById<TextView>(R.id.connection)
    }


    private val viewModel: DownloadInfoDialogViewModel by lazy {
        ViewModelProviders.of(this, DownloadInfoDialogViewModelFactory(arguments!!.getString(BOOK_ID)!!))
                .get(DownloadInfoDialogViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        context?.let {
            viewModel.paperLiveData.observe(this, Observer {
                d { "XXXX $it"}
                dialog.clearNegativeListeners()

                if (it.downloadState == DownloadState.DOWNLOADING || it.downloadState == DownloadState.DOWNLOADED) {
                    dialog.negativeButton(text = "Download abbrechen")
                }
                else if (it.downloadState == DownloadState.READY) {
                    dismiss()
                }
                else {
                    dialog.negativeButton(text = "")
                }

            })
            viewModel.downloadLiveData.observe(this, Observer {
                fileName.text = it.file.absolutePath

            })
            viewModel.networkLiveData.observe(this, Observer {
                connection.text = it.type.readable
            })

            return dialog
        }
        return super.onCreateDialog(savedInstanceState)
    }

}

class DownloadInfoDialogViewModel(val bookId: String) : ViewModel(), Connection.ConnectionChangeListener {

    val paperLiveData = paperRepository.getLivePaper(bookId);
    val downloadLiveData = MutableLiveData<Download>()
    val networkLiveData = MutableLiveData<ConnectionInfo>()

    init {

        d { "inti XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX ${paperLiveData.value}"}
        poll()
        networkLiveData.value = Connection.getConnectionInfo()
        Connection.addListener(this)
    }

    fun poll() {
        viewModelScope.launch {
            val download = withContext(Dispatchers.Default) {
                paperRepository.getDownloadForPaper(bookId)
            }
            downloadLiveData.value = download
            if (download.state == DownloadState.DOWNLOADING) {

            }
            delay(500)
            poll()
        }
    }

    override fun onNetworkConnectionChanged(info: ConnectionInfo) {
        networkLiveData.value = info
    }
}

class DownloadInfoDialogViewModelFactory(val bookId: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DownloadInfoDialogViewModel(bookId) as T
    }

}