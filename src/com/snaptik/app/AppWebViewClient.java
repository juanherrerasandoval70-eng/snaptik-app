package com.snaptik.app;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AppWebViewClient extends WebViewClient {
    private final MainActivity activity;

    public AppWebViewClient(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (activity != null) {
            activity.handleIncomingIntent(activity.getIntent());
        }
    }
}
