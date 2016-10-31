package de.thecode.android.tazreader.start;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import de.mateware.dialog.DialogCustomView;

/**
 * Created by Mate on 16.02.2015.
 */
public class LicencesDialog extends DialogCustomView {

    @Override
    public View getView(LayoutInflater inflater,ViewGroup parent) {
        WebView webView = new WebView(getContext());
        webView.loadUrl("file:///android_asset/licences.html");
        return webView;
    }

    public static class Builder extends AbstractBuilder<Builder, LicencesDialog> {
        public Builder() {
            super(LicencesDialog.class);
        }
    }
}
