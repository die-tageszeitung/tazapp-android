package de.thecode.android.tazreader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.data.Paper;
import de.thecode.android.tazreader.data.Resource;
import de.thecode.android.tazreader.push.PushNotification;
import de.thecode.android.tazreader.utils.StorageHelper;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by mate on 25.07.2017.
 */

public class PushNotificationDialog extends DialogCustomView {

    private static final String ARG_PUSH_NOTIFICATION = "pushNotification";
    private PushNotification pushNotification;

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        WebView webView = new WebView(inflater.getContext());
        webView.getSettings()
               .setJavaScriptEnabled(true);

        pushNotification = getArguments().getParcelable(ARG_PUSH_NOTIFICATION);
        if (pushNotification != null) {
            if (!TextUtils.isEmpty(pushNotification.getUrl())) {
                webView.loadUrl(pushNotification.getUrl());
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

        try {
            html = IOUtils.toString(context.getAssets()
                                           .open("push/template.html"), "UTF-8");

            String cssfilepath = "file:///android_asset/push/simple.css";
            Paper latestPaper = Paper.getLatestPaper(context);
            if (latestPaper != null) {
                Resource latestResource = Resource.getWithKey(context, latestPaper.getResource());
                if (latestResource != null && latestResource.isDownloaded()) {
                    File resourceDir = StorageHelper.getResourceDirectory(getContext(), latestResource.getKey());
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
