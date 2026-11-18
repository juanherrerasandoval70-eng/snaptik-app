package com.snaptik.app;

import android.util.Base64;
import android.webkit.WebView;

public class ExtractTaskRunnable implements Runnable {
    private final WebView webView;
    private final String tiktokUrl;

    public ExtractTaskRunnable(WebView webView, String tiktokUrl) {
        this.webView = webView;
        this.tiktokUrl = tiktokUrl;
    }

    @Override
    public void run() {
        String jsonResult = TikTokResolver.resolve(tiktokUrl);
        try {
            byte[] utf8Bytes = jsonResult.getBytes("UTF-8");
            String base64Json = Base64.encodeToString(utf8Bytes, Base64.NO_WRAP);
            if (webView != null) {
                webView.post(new EvaluateJsRunnable(webView, "window.onExtractionCompleteBase64('" + base64Json + "');"));
            }
        } catch (Exception e) {
            String escaped = jsonResult.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ");
            if (webView != null) {
                webView.post(new EvaluateJsRunnable(webView, "window.onExtractionComplete('" + escaped + "');"));
            }
        }
    }
}
