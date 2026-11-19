package com.snaptik.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {

    public WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Barra de estado y navegación en negro puro OLED (#000000)
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(0xFF000000);
            window.setNavigationBarColor(0xFF000000);
        }

        setContentView(R.layout.activity_main);

        // Cargar backend remoto guardado en caché si existe
        try {
            String cachedBackend = getSharedPreferences("snaptik_prefs", MODE_PRIVATE)
                                   .getString("custom_backend_url", null);
            if (cachedBackend != null && !cachedBackend.isEmpty()) {
                TikTokResolver.setCustomBackendUrl(cachedBackend);
            }
        } catch (Exception ignored) {}

        webView = findViewById(R.id.webView);
        webView.setBackgroundColor(0xFF000000);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Bloqueo estricto de depuración remota WebView (Anti-Chrome DevTools / Anti-Reverse)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(false);
        }

        // Aceleración por hardware a 60/120Hz
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebViewClient(new AppWebViewClient(this));
        webView.addJavascriptInterface(new WebAppInterface(this, webView), "AndroidApp");
        webView.loadUrl("file:///android_asset/index.html");

        // Red publicitaria: Adsterra Smartlinks (Activación directa)

        // Comprobación silenciosa de actualizaciones OTA en segundo plano
        webView.postDelayed(new AutoCheckUpdateRunnable(this), 3000);

        // Procesar intent de compartir recibido
        handleIncomingIntent(getIntent());
    }

    private static class AutoCheckUpdateRunnable implements Runnable {
        private final MainActivity activity;
        public AutoCheckUpdateRunnable(MainActivity activity) {
            this.activity = activity;
        }
        @Override
        public void run() {
            if (activity != null && activity.webView != null) {
                AppUpdateManager.checkForUpdates(activity, activity.webView, false);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    public void handleIncomingIntent(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.isEmpty()) {
                String safeText = sharedText.replace("'", "\\'");
                webView.post(new EvaluateJsRunnable(webView, "window.onSharedLinkReceived('" + safeText + "');"));
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
