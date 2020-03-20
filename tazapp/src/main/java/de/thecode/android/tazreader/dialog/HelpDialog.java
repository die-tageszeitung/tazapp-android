package de.thecode.android.tazreader.dialog;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.DownloadState;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.utils.AsyncTaskListener;
import de.thecode.android.tazreader.utils.StorageManager;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import timber.log.Timber;

/**
 * Created by mate on 25.07.2017.
 */

public class HelpDialog extends DialogCustomView {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({HELP_INTRO, HELP_LIBRARY, HELP_ARTICLE, HELP_PAGE, HELP_PRIVACY})
    public @interface HelpPage {
    }


    public static final String HELP_INTRO   = "intro.html";
    public static final String HELP_LIBRARY = "ausgaben.html";
    public static final String HELP_PAGE    = "seiten.html";
    public static final String HELP_ARTICLE = "artikel.html";
    public static final String HELP_PRIVACY = "datenschutz.html";

    private static final String[] HELP_RESOURCE_SUBDIRS = {"res/android-help", "res/ios-help"};

    private static final String ARG_HELPPAGE   = "helpPage";
    private static final String ARG_CURRENTURL = "currentUrl";
    private static final String ARG_BASEURL    = "baseUrl";

    private String  baseUrlPath;
    private WebView webView;
    private String  currentUrl;

    private PaperRepository    paperRepository;
    private ResourceRepository resourceRepository;
    private StorageManager     storageManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentUrl = savedInstanceState.getString(ARG_CURRENTURL);
            baseUrlPath = savedInstanceState.getString(ARG_BASEURL);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View getView(final LayoutInflater inflater, ViewGroup parent) {

        View view = inflater.inflate(R.layout.dialog_help, parent, false);
        webView = view.findViewById(R.id.help_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Timber.i("%s", url);
                if (url != null && url.startsWith(baseUrlPath)) {
                    view.loadUrl(url);
                } else {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext()
                        .startActivity(i);
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        if (TextUtils.isEmpty(currentUrl)) {
            paperRepository = PaperRepository.getInstance(view.getContext());
            resourceRepository = ResourceRepository.getInstance(view.getContext());
            storageManager = StorageManager.getInstance(view.getContext());
            new AsyncTaskListener<Void, String>(aVoid -> {
                List<Paper> papers = paperRepository.getAllPapers();
                String result = "file:///android_asset/help/";
                for (Paper paper : papers) {
                    Resource latestResource = resourceRepository.getWithKey(paper.getResource());
                    if (latestResource != null && resourceRepository.getDownloadState(latestResource.getKey()) == DownloadState.READY) {
                        File latestResourceDir = storageManager.getResourceDirectory(latestResource.getKey());
                        File helpFileDir = null;
                        for (String helpFileSubdirPath : HELP_RESOURCE_SUBDIRS) {
                            helpFileDir = new File(latestResourceDir, helpFileSubdirPath);
                            if (helpFileDir.exists()) break;
                        }
                        if (helpFileDir != null) {
                            result = "file://" + helpFileDir.getAbsolutePath() + "/";
                            break;
                        }
                    }
                }
                return result;
            }, baseUrl -> {
                baseUrlPath = baseUrl;
                String helpPage = getArguments().getString(ARG_HELPPAGE);
                currentUrl = baseUrlPath + helpPage;
                loadUrl();
            }).execute();
        } else {
            loadUrl();
        }

        return view;
    }

    private void loadUrl() {
        Timber.d("loading url %s", currentUrl);
        webView.loadUrl(currentUrl);
    }

    @Override
    public Bundle onSaveInstanceState(Bundle outState) {
        if (webView != null) outState.putString(ARG_CURRENTURL, webView.getUrl());
        outState.putString(ARG_BASEURL, baseUrlPath);
        return super.onSaveInstanceState(outState);
    }

    public static class Builder extends AbstractBuilder<Builder, HelpDialog> {

        Bundle bundle = new Bundle();

        public Builder() {
            super(HelpDialog.class);
        }

        public Builder setHelpPage(@HelpPage String helpPage) {
            bundle.putString(ARG_HELPPAGE, helpPage);
            return this;
        }

        @Override
        public void preBuild() {
            super.preBuild();
            addBundle(bundle);
        }
    }

}
