package de.thecode.android.tazreader.start;

import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import de.thecode.android.tazreader.dialog.TcDialogCustomView;

/**
 * Created by Mate on 16.02.2015.
 */
public class LicencesDialog extends TcDialogCustomView {

    @Override
    public View getView(LayoutInflater inflater) {
        WebView webView = new WebView(getActivity());
        webView.loadUrl("file:///android_asset/licences.html");
        return webView;
    }
}
