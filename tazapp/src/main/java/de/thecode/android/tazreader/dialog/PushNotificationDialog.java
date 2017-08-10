package de.thecode.android.tazreader.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import de.mateware.dialog.DialogCustomView;
import de.thecode.android.tazreader.push.PushNotification;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * Created by mate on 25.07.2017.
 */

public class PushNotificationDialog extends DialogCustomView {

    private static final String ARG_PUSH_NOTIFICATION = "pushNotification";

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        WebView webView = new WebView(inflater.getContext());
        webView.loadDataWithBaseURL("file:///android_asset/push/", getHtml(inflater.getContext()), "text/html", "utf-8", null);
        webView.getSettings()
               .setAllowFileAccess(true);
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
        PushNotification pushNotification = getArguments().getParcelable(ARG_PUSH_NOTIFICATION);
        String html;
        try {
            html = IOUtils.toString(context.getAssets()
                                           .open("push/template.html"), "UTF-8");

            if (pushNotification != null) {
                html = html.replaceFirst("\\{title\\}", pushNotification.getTitle())
                           .replaceFirst("\\{body\\}", pushNotification.getBody());
            }
        } catch (IOException e) {
            html = e.getMessage();
        }
        return html;
    }


}
