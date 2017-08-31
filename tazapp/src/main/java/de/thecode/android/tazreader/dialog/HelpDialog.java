package de.thecode.android.tazreader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.download.ResourceDownloadEvent;
import de.thecode.android.tazreader.utils.StorageManager;

import org.apache.commons.io.IOUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import timber.log.Timber;

/**
 * Created by mate on 25.07.2017.
 */

public class HelpDialog extends DialogCustomView {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({HELP_INTRO, HELP_LIBRARY, HELP_ARTICLE, HELP_PAGE})
    public @interface HelpPage {
    }


    public static final String HELP_INTRO   = "intro.html";
    public static final String HELP_LIBRARY = "ausgaben.html";
    public static final String HELP_PAGE    = "seiten.html";
    public static final String HELP_ARTICLE = "artikel.html";

    private static final String[] HELP_RESOURCE_SUBDIRS = {"res/android-help","res/ios-help"};

    private static final String ARG_HELPPAGE = "helpPage";
    private String  helpPage;
    private WebView webView;
    private boolean withoutRessourceMode = true;

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        helpPage = getArguments().getString(ARG_HELPPAGE);
        webView = new WebView(inflater.getContext());
        webView.getSettings()
               .setJavaScriptEnabled(true);
        setHtmlInWebView(inflater.getContext());
        return webView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void setHtmlInWebView(Context context) {

        List<Paper> papers = Paper.getAllPapers(context);

        String baseUrl = "file:///android_asset/help/";
        InputStream helpFileStream = null;

        for (Paper paper: papers) {
            Resource latestResource = Resource.getWithKey(context, paper.getResource());
            if (latestResource != null && latestResource.isDownloaded()) {

                File latestResourceDir = StorageManager.getInstance(context)
                                                       .getResourceDirectory(latestResource.getKey());
                File helpFileDir = null;
                for (String helpFileSubdirPath : HELP_RESOURCE_SUBDIRS) {
                    helpFileDir = new File(latestResourceDir, helpFileSubdirPath);
                    if (helpFileDir.exists()) break;
                }
                if (helpFileDir != null) {
                    try {
                        helpFileStream = new FileInputStream(new File(helpFileDir, helpPage));
                        baseUrl = "file://" + helpFileDir.getAbsolutePath() + "/";
                        withoutRessourceMode = false;
                        break;
                    } catch (FileNotFoundException e) {
                        Timber.e(e);
                    }
                }
            }
        }

        if (helpFileStream == null) {
            try {
                helpFileStream = context.getAssets()
                                        .open("help/help.html");
            } catch (IOException e) {
                Timber.e(e);
            }
        }
        String html = "Hilfe konnte nicht geladen werden";
        if (helpFileStream != null) {
            try {
                html = IOUtils.toString(helpFileStream, "UTF-8");
            } catch (IOException e) {
                Timber.e(e);
            }
        }
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResourceDownload(ResourceDownloadEvent event) {
        if (withoutRessourceMode) {
            if (getContext() != null) {
                setHtmlInWebView(getContext());
            }
        }
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
