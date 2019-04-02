package de.thecode.android.tazreader.dialognew

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import de.thecode.android.tazreader.*
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.DownloadState
import de.thecode.android.tazreader.data.Paper
import de.thecode.android.tazreader.data.UnmeteredDownloadOnly
import de.thecode.android.tazreader.download.TazDownloadManager
import de.thecode.android.tazreader.utils.Connection
import de.thecode.android.tazreader.utils.ConnectionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadInfoDialog : DialogFragment() {

    interface CancelDownloadDialogListener {
        fun onCancelDownload(bookId: String)
    }

    companion object {

        const val DIALOG_TAG = "downloadInfoDialog"
        private const val BOOK_ID = "bookId"
        private const val TITLE = "title"

        fun newInstance(paper: Paper): DownloadInfoDialog {
            val args = Bundle()
            args.putString(BOOK_ID, paper.bookId)
            args.putString(TITLE, paper.getTitelWithDate(res))
            val fragment = DownloadInfoDialog()
            fragment.arguments = args
            return fragment
        }
    }

    val bookId: String by lazy {
        arguments!!.getString(BOOK_ID)
    }

    var dialog: MaterialDialog? = null

    private val state: TextView by lazy {
        dialog!!.getCustomView()
                .findViewById<TextView>(R.id.state)
    }
    private val connection: TextView by lazy {
        dialog!!.getCustomView()
                .findViewById<TextView>(R.id.connection)
    }

    private val progress: ProgressBar by lazy {
        dialog!!.getCustomView()
                .findViewById<ProgressBar>(R.id.progress)
    }

    private val dmlog: TextView by lazy {
        dialog!!.getCustomView()
                .findViewById<TextView>(R.id.dmLog)
    }

    private val unmeteredOnly: TextView by lazy {
        dialog!!.getCustomView()
                .findViewById<TextView>(R.id.unmeteredOnly)
    }

    private val title: TextView by lazy {
        dialog!!.getCustomView()
                .findViewById<TextView>(R.id.title)
    }

    private val viewModel: DownloadInfoDialogViewModel by lazy {
        ViewModelProviders.of(this, DownloadInfoDialogViewModelFactory(arguments!!.getString(BOOK_ID)!!))
                .get(DownloadInfoDialogViewModel::class.java)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        context?.let {
            dialog = MaterialDialog(context!!)
                    .customView(viewRes = R.layout.dialog_download_info, scrollable = true)
                    .negativeButton(R.string.downloadinfo_cancel_download)
                    .positiveButton()
            title.text = app.getString(R.string.downloadinfo_dialog_title,arguments!!.getString(TITLE))
            viewModel.downloadInfo.observe(this, Observer {
                if (it.state == DownloadState.READY) {
                    dismiss()
                }
                connection.text = it.connectionType.readable()
                @SuppressLint("SetTextI18n")
                state.text = it.state.readable()
                dmlog.text = it.dmLog
                progress.progress = it.progress
                unmeteredOnly.text = it.unmeteredOnly.readable()
            })
            return dialog!!
        }
        return super.onCreateDialog(savedInstanceState)
    }

}

class DownloadInfoDialogViewModel(val bookId: String) : ViewModel(), Connection.ConnectionChangeListener {

    //    val paperLiveData = paperRepository.getLivePaper(bookId);
    val downloadInfo = MutableLiveData<DownloadInfo>(DownloadInfo())
    val downloadLiveData = downloadsRepository.getLiveData(bookId)

    init {

        onNetworkConnectionChanged(Connection.getConnectionInfo())
        Connection.addListener(this)
        downloadLiveData.observeForever {
            it?.let{
                downloadInfo.value!!.unmeteredOnly = it.unmeteredOnly!!
                downloadInfo.value!!.state = it.state
                when (it.state) {
                    DownloadState.DOWNLOADING -> poll()
                    DownloadState.EXTRACTING, DownloadState.CHECKING -> {
                        downloadInfo.value!!.progress = it.progress

                    }
                    else -> {
                    }
                }
                downloadInfo.value = downloadInfo.value
            }
        }

    }

    fun poll() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val download = paperRepository.getDownloadForPaper(bookId)
                when (download.state) {
                    DownloadState.DOWNLOADING, DownloadState.DOWNLOADED -> {
                        val systemDownloadInfo = TazDownloadManager.getInstance()
                                .getSystemDownloadManagerInfo(download.downloadManagerId)
                        val downloadInfoValue = downloadInfo.value
                        downloadInfoValue!!.progress = (systemDownloadInfo.bytesDownloadedSoFar * 100 / systemDownloadInfo.totalSizeBytes).toInt()
                        val reason = if (systemDownloadInfo.reason != 0) " (${systemDownloadInfo.reasonText})" else ""
                        downloadInfoValue.dmLog = "${systemDownloadInfo.statusText}$reason"
                        downloadInfo.postValue(downloadInfoValue)
                        delay(200)
                        if (downloadInfo.value!!.state == DownloadState.DOWNLOADING || downloadInfo.value!!.state == DownloadState.DOWNLOADED) {
                            poll()
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }


    override fun onNetworkConnectionChanged(info: ConnectionInfo) {
        val downloadInfoValue = downloadInfo.value
        downloadInfoValue!!.connectionType = info.type
        downloadInfo.value = downloadInfoValue
    }
}

class DownloadInfoDialogViewModelFactory(val bookId: String) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return DownloadInfoDialogViewModel(bookId) as T
    }

}

data class DownloadInfo(var connectionType: Connection.Type = Connection.Type.NOT_AVAILABLE,
                        var unmeteredOnly: UnmeteredDownloadOnly = UnmeteredDownloadOnly.UNKNOWN,
                        var state: DownloadState = DownloadState.NONE,
                        var progress: Int = 0,
                        var dmLog: String = "")