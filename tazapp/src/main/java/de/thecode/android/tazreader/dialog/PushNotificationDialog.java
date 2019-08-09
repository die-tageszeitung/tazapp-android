package de.thecode.android.tazreader.dialog;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.data.DownloadState;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.PaperRepository;
import de.thecode.android.tazreader.data.ResourceRepository;
import de.thecode.android.tazreader.data.ResourceWithDownloadState;
import de.thecode.android.tazreader.push.PushNotification;
import de.thecode.android.tazreader.utils.AsyncTaskListener;
import de.thecode.android.tazreader.utils.Charsets;
import de.thecode.android.tazreader.utils.StorageManager;
import de.thecode.android.tazreader.utils.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mate on 25.07.2017.
 */

public class PushNotificationDialog extends DialogCustomView {

    private static final String ARG_PUSH_NOTIFICATION = "pushNotification";
    private WebView            mWebView;
    private PaperRepository    paperRepository;
    private ResourceRepository resourceRepository;
    private StorageManager     storageManager;
    private AssetManager       assetManager;

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        paperRepository = PaperRepository.getInstance(inflater.getContext());
        resourceRepository = ResourceRepository.getInstance(inflater.getContext());
        storageManager = StorageManager.getInstance(inflater.getContext());
        assetManager = inflater.getContext()
                               .getAssets();
        WebView webView = new WebView(inflater.getContext());
        mWebView = webView;
        webView.getSettings()
               .setJavaScriptEnabled(true);

        PushNotification pushNotification = getArguments().getParcelable(ARG_PUSH_NOTIFICATION);
        if (pushNotification != null) {
            if (!TextUtils.isEmpty(pushNotification.getUrl())) {
                webView.loadUrl(pushNotification.getUrl());
            } else {
                webView.getSettings()
                       .setAllowFileAccess(true);
                new AsyncTaskListener<PushNotification, String>(
                        pushNotifications -> getHtml(pushNotifications[0]),
                        html -> mWebView.loadDataWithBaseURL(
                                "file:///android_asset/push/", html, "text/html", "utf-8", null
                        )
                ).execute(pushNotification);
            }
        }
        return webView;
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

    private String getHtml(PushNotification pushNotification) {


        String html;

        try {
            InputStream inputStream = assetManager.open("push/template.html");
            html = StreamUtils.toString(inputStream, Charsets.UTF_8);
            inputStream.close();

            String cssfilepath = "file:///android_asset/push/simple.css";
            Paper latestPaper = paperRepository.getLatestPaper();
            if (latestPaper != null) {
                ResourceWithDownloadState latestResource = resourceRepository.getWithKey(latestPaper.getResource());
                if (latestResource != null && latestResource.getDownloadState() == DownloadState.READY) {
                    File resourceDir = storageManager.getResourceDirectory(latestResource.getKey());
                    File cssfile = new File(resourceDir, "res/css/notification.css");
                    if (cssfile.exists()) {
                        cssfilepath = "file://" + resourceDir.getAbsolutePath() + "/res/css/notification.css";
                    }
                }

            }
            html = html.replaceFirst("\\{cssfile\\}", cssfilepath);

            if (pushNotification != null) {

                String body = pushNotification.getType() == PushNotification.Type.debug ? "<pre>" + pushNotification.toString()
                                                                                                                    .replaceAll(
                                                                                                                            "\\n",
                                                                                                                            "<BR>") + "</pre>" : pushNotification.getBody();
                if (TextUtils.isEmpty(body)) {
                    body = "Kein Text";
                }
                html = html.replaceFirst("\\{body\\}", body);
            }
        } catch (IOException e) {
            html = e.getMessage();
        }
        return html;
    }


}
