package de.thecode.android.tazreader.dialog;

import android.os.Bundle;
import android.support.annotation.StringDef;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.R;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.utils.StorageManager;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

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

    private static final String[] HELP_RESOURCE_SUBDIRS = {"res/android-help","res/ios-help"};

    private static final String ARG_HELPPAGE = "helpPage";

    @Override
    public View getView(final LayoutInflater inflater, ViewGroup parent) {
        String helpPage = getArguments().getString(ARG_HELPPAGE);


        List<Paper> papers = Paper.getAllPapers(inflater.getContext());

        String baseUrl = "file:///android_asset/help/";

        for (Paper paper: papers) {
            Resource latestResource = Resource.getWithKey(inflater.getContext(), paper.getResource());
            if (latestResource != null && latestResource.isDownloaded()) {

                File latestResourceDir = StorageManager.getInstance(getContext())
                                                       .getResourceDirectory(latestResource.getKey());
                File helpFileDir = null;
                for (String helpFileSubdirPath : HELP_RESOURCE_SUBDIRS) {
                    helpFileDir = new File(latestResourceDir, helpFileSubdirPath);
                    if (helpFileDir.exists()) break;
                }
                if (helpFileDir != null) {
                     baseUrl = "file://" + helpFileDir.getAbsolutePath() + "/";
                     break;
                }
            }
        }

        baseUrl += helpPage;

        View view = inflater.inflate(R.layout.dialog_help, parent, false);
        WebView webView = view.findViewById(R.id.help_web_view);
        webView.getSettings()
               .setJavaScriptEnabled(true);
        webView.loadUrl(baseUrl);
        return view;
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
