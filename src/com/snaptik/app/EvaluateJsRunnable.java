package com.snaptik.app;

import android.webkit.WebView;

public class EvaluateJsRunnable implements Runnable {
    private final WebView webView;
    private final String script;

    public EvaluateJsRunnable(WebView webView, String script) {
        this.webView = webView;
        this.script = script;
    }

    @Override
    public void run() {
        if (webView != null) {
            webView.evaluateJavascript(script, null);
        }
    }
}
