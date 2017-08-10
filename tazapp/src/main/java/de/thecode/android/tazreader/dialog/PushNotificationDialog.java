package de.thecode.android.tazreader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.push.PushNotification;
import de.thecode.android.tazreader.utils.StorageManager;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by mate on 25.07.2017.
 */

public class PushNotificationDialog extends DialogCustomView {

    private static final String ARG_PUSH_NOTIFICATION = "pushNotification";
    private static final String ARG_CURRENT_URL = "pushNotificationCurrentUrl";

    private String           currentUrl;
    private PushNotification pushNotification;

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        WebView webView = new WebView(inflater.getContext());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                currentUrl = url;
                return true;
            }
        });

        pushNotification = getArguments().getParcelable(ARG_PUSH_NOTIFICATION);
        if (pushNotification != null) {
            if (!TextUtils.isEmpty(pushNotification.getUrl())) {
                webView.loadUrl(TextUtils.isEmpty(currentUrl) ? pushNotification.getUrl() : currentUrl);
            } else {
                webView.loadDataWithBaseURL("file:///android_asset/push/",
                                            getHtml(inflater.getContext()),
                                            "text/html",
                                            "utf-8",
                                            null);
                webView.getSettings()
                       .setAllowFileAccess(true);
            }
        }
        return webView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            currentUrl = savedInstanceState.getString(ARG_CURRENT_URL);
        }
    }

    @Override
    public Bundle onSaveInstanceState(Bundle outState) {
        outState.putString(ARG_CURRENT_URL, currentUrl);
        return super.onSaveInstanceState(outState);
    }

    public static class Builder extends AbstractBuilder<Builder, PushNotificationDialog> {

        public Builder() {
            super(PushNotificationDialog.class);
        }

        public Builder setPushNotification(PushNotification pushNotification) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(ARG_PUSH_NOTIFICATION, pushNotification);
            addBundle(bundle);
            return this;
        }

    }

    private String getHtml(Context context) {
        String html;


        String cssfilepath = "file:///android_asset/push/simple.css";
        Resource latestResource = Resource.getLatestDownloaded(context);
        if (latestResource != null) {
            File resourceDir = StorageManager.getInstance(context)
                                             .getResourceDirectory(latestResource.getKey());
            File cssfile = new File(resourceDir,"res/css/notification.css");
            if (cssfile.exists()) {
                cssfilepath = "file://" + resourceDir.getAbsolutePath() + "/res/css/notification.css";
            }
        }

        try {
            html = IOUtils.toString(context.getAssets()
                                           .open("push/template.html"), "UTF-8");

            if (pushNotification != null) {
                String body = pushNotification.getType() == PushNotification.Type.debug ? pushNotification.toString() : pushNotification.getBody();
                html = html.replaceFirst("\\{cssfile\\}", cssfilepath)
                           .replaceFirst("\\{title\\}", pushNotification.getTitle())
                           .replaceFirst("\\{body\\}", body);
            }
        } catch (IOException e) {
            html = e.getMessage();
        }
        return html;
    }


}
