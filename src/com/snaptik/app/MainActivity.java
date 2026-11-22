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
    private WebAppInterface webAppInterface;
    private String lastAutoPastedUrl = "";

    @Override
    protected void onResume() {
        super.onResume();
        AppNotificationManager.checkRemoteAnnouncements(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            checkClipboardForTikTokLink();
        }
    }

    private void checkClipboardForTikTokLink() {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                android.content.ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null) {
                        String str = text.toString().trim();
                        if ((str.contains("tiktok.com/") || str.contains("douyin.com/")) && !str.equals(lastAutoPastedUrl)) {
                            lastAutoPastedUrl = str;
                            String safeText = str.replace("'", "\\'");
                            if (webView != null) {
                                webView.post(new EvaluateJsRunnable(webView, "if(window.onAutoPasteDetected) { window.onAutoPasteDetected('" + safeText + "'); }"));
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Blindaje en el arranque: bloquear APK repackaged/modificado o inyectado con Frida/Debugger
        if (!AppSecurityGuard.verifySignature(this)
                || AppSecurityGuard.isFridaOrHookingDetected()
                || AppSecurityGuard.isDebuggerActive()) {
            android.widget.Toast.makeText(this, "Integridad de aplicación no verificada.", android.widget.Toast.LENGTH_LONG).show();
            finishAffinity();
            return;
        }

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
        webAppInterface = new WebAppInterface(this, webView);
        webView.addJavascriptInterface(webAppInterface, "AndroidApp");
        webView.loadUrl("file:///android_asset/index.html");

        // Solicitar permiso de notificaciones (Android 13+) y activar recepción en segundo plano (cada 15 min)
        AppNotificationManager.requestPermissionIfNeeded(this);
        AppNotificationManager.checkRemoteAnnouncements(this);
        RemotePushJobService.schedulePeriodicCheck(this);

        // Inicializar Start.io (Banner + Native + Interstitial + Video Reward + Return Ads)
        android.view.ViewGroup bannerContainer = findViewById(R.id.startIoBannerContainer);
        StartIoRewardManager.init(this, bannerContainer, webView);

        // Comprobación silenciosa de actualizaciones OTA en segundo plano
        webView.postDelayed(new AutoCheckUpdateRunnable(this), 3000);
    }

    @Override
    protected void onDestroy() {
        if (webAppInterface != null) {
            webAppInterface.cleanup();
        }
        super.onDestroy();
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
                String safeText = sharedText.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ");
                setIntent(new Intent());
                if (webView != null) {
                    webView.post(new EvaluateJsRunnable(webView, "if(window.onSharedLinkReceived){window.onSharedLinkReceived('" + safeText + "');}"));
                }
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
