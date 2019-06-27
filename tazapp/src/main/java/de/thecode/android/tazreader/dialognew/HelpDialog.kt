package de.thecode.android.tazreader.dialognew

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.github.ajalt.timberkt.d
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.DownloadState
import de.thecode.android.tazreader.paperRepository
import de.thecode.android.tazreader.resourceRepository
import de.thecode.android.tazreader.storageManager
import org.jetbrains.anko.doAsync
import java.io.File

class HelpDialog : DialogFragment() {

    companion object {
        const val TAG = "helpDialog"
        const val ARG_PAGE = "helpPage"
        fun newInstance(page: HelpPage): HelpDialog {
            val bundle = Bundle()
            bundle.putSerializable(ARG_PAGE, page)
            val fragment = HelpDialog()
            fragment.arguments = bundle
            return fragment
        }
    }

    var webView: WebView? = null

    private val viewModel: HelpDialogViewModel by lazy {
        ViewModelProviders.of(this, HelpDialogViewModelFactory(arguments!!.getSerializable(ARG_PAGE) as HelpPage))
                .get(HelpDialogViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        d { "XXXX onCreateDialog" }
        context?.let { dialogContext ->
            val dialog = MaterialDialog(dialogContext).customView(viewRes = R.layout.dialog_help,noVerticalPadding = true,dialogWrapContent = true)
            dialog.maxWidth(R.dimen.help_max_width)
            dialog.positiveButton()
            if (viewModel.page == HelpPage.INTRO){
                dialog.neutralButton(R.string.drawer_account) {
                    activity?.let {
                        if (it is HelpIntroActivity){
                            it.onAccountDataClick()
                        }
                    }
                }
            }
            webView = dialog.getCustomView()
                    .findViewById(R.id.help_web_view)
            webView?.settings!!.javaScriptEnabled = true
            webView?.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let {
                        if (url.startsWith(viewModel.baseUrl)) {
                            view?.loadUrl(url)
                        } else {
                            context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    d { "XXX onPageFinished $url" }
                    viewModel.currentUrlLiveData.value = url
                    super.onPageFinished(view, url)
                }
            }
            viewModel.currentUrlLiveData.observe(this, Observer { urlString ->
                d { "XXX ${webView!!.url}" }
                webView?.let {
                    if (it.url != urlString) {
                        webView?.loadUrl(urlString)
                    }
                }
            })
            return dialog
        }

        return super.onCreateDialog(savedInstanceState)

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        d { "XXXX onViewCreated" }
        super.onViewCreated(view, savedInstanceState)
    }
}

enum class HelpPage(val pageName: String) {
    INTRO("intro.html"),
    LIBRARY("ausgaben.html"),
    PAGE("help.html#seiten"),
    ARTICLE("artikel.html"),
    PRIVACY("datenschutz.html");
}

class HelpDialogViewModel(val page: HelpPage) : ViewModel() {

    var baseUrl = "file:///android_asset/help/"
    val currentUrlLiveData = MutableLiveData<String>()

    init {
        //currentUrlLiveData.value = "file:///android_asset/help/${page.pageName}"
        doAsync {
            run finder@{
                paperRepository.allPapers.forEach { paper ->
                    val latestResource = resourceRepository.getResourceForPaper(paper)
                    latestResource?.let { resource ->
                        d { "XXX $latestResource" }
                        if (resource.downloadState == DownloadState.READY) {
                            val latestResourceDir = storageManager.getResourceDirectory(resource)
                            d { "XXX $latestResourceDir" }
                            arrayOf("res/android-help", "res/ios-help").map {
                                File(latestResourceDir, it)
                            }
                                    .find {
                                        it.exists()
                                    }
                                    ?.let {
                                        baseUrl = "file://${it.absolutePath}/"
                                        return@finder
                                    }

                        }
                    }
                }
            }
            currentUrlLiveData.postValue(baseUrl + page.pageName)
        }
    }
}

@Suppress("UNCHECKED_CAST")
class HelpDialogViewModelFactory(val page: HelpPage) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return HelpDialogViewModel(page) as T
    }
}

interface HelpIntroActivity {
    fun onAccountDataClick()
}
