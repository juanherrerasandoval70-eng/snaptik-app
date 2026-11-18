package com.snaptik.app;

import android.app.DownloadManager;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

public class WebAppInterface {
    private final Context context;
    private final WebView webView;

    public WebAppInterface(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        registerDownloadReceiver();
    }

    private void registerDownloadReceiver() {
        try {
            DownloadCompleteReceiver receiver = new DownloadCompleteReceiver(context);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        } catch (Exception ignored) {}
    }

    @JavascriptInterface
    public void extract(String tiktokUrl) {
        vibrate(30);
        if (!AppSecurityGuard.verifySignature(context)) {
            showToast("Alerta de seguridad: Firma del APK alterada.");
            return;
        }
        if (AppSecurityGuard.isDebuggerActive()) {
            showToast("Depuración detectada: Extracción denegada.");
            return;
        }
        new Thread(new ExtractTaskRunnable(webView, tiktokUrl)).start();
    }

    @JavascriptInterface
    public void download(String fileUrl, String filename) {
        vibrate(50);
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle("SnapTik: " + filename);
            request.setDescription("Guardando en Galería...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            request.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            request.addRequestHeader("Referer", "https://ssstik.io/");

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(context, "Descarga iniciada. Visible en tu galería.", Toast.LENGTH_SHORT).show();
                if (AdNetworkManager.recordDownloadAndCheckAdTrigger()) {
                    if (context instanceof android.app.Activity) {
                        AdNetworkManager.triggerInterstitial((android.app.Activity) context, webView);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error al descargar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @JavascriptInterface
    public void openDownloadsFolder() {
        vibrate(25);
        try {
            Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
                intent.setDataAndType(uri, "*/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {
                Toast.makeText(context, "Archivos guardados en carpeta Descargas", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @JavascriptInterface
    public void shareText(String text, String title) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            Intent chooser = Intent.createChooser(intent, title != null ? title : "Compartir con...");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, "Error al compartir", Toast.LENGTH_SHORT).show();
        }
    }

    @JavascriptInterface
    public String getClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                if (text != null) {
                    return text.toString();
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    @JavascriptInterface
    public void vibrate(int milliseconds) {
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(milliseconds);
                }
            }
        } catch (Exception ignored) {}
    }

    @JavascriptInterface
    public String getSecurityReport() {
        return AppSecurityGuard.getSecurityReport(context);
    }

    @JavascriptInterface
    public void openSponsorOffer(String url) {
        AdNetworkManager.openDirectSponsorUrl(context, url);
    }

    @JavascriptInterface
    public void requestRewardedVideo() {
        if (context instanceof android.app.Activity) {
            AdNetworkManager.triggerAdsterraReward((android.app.Activity) context, webView);
        }
    }

    @JavascriptInterface
    public void testRewardedVideo() {
        if (context != null) {
            AdNetworkManager.openDirectSponsorUrl(context, null);
        }
    }

    @JavascriptInterface
    public String getUnityAdsStatus() {
        org.json.JSONObject obj = new org.json.JSONObject();
        try {
            obj.put("initialized", true);
            obj.put("ready", true);
            obj.put("network", "Adsterra Smartlinks");
            obj.put("activeLinks", AdNetworkManager.SMARTLINKS.length);
            obj.put("currentLink", AdNetworkManager.getCurrentSmartlink());
        } catch (Exception ignored) {}
        return obj.toString();
    }

    @JavascriptInterface
    public String getAdNetworkConfig() {
        return AdNetworkManager.getAdConfig();
    }

    @JavascriptInterface
    public void checkForUpdates(boolean isManual) {
        vibrate(20);
        AppUpdateManager.checkForUpdates(context, webView, isManual);
    }

    @JavascriptInterface
    public void installUpdate(String apkUrl, String versionName) {
        vibrate(30);
        AppUpdateManager.downloadAndInstall(context, webView, apkUrl, versionName);
    }

    @JavascriptInterface
    public void setCustomUpdateUrl(String url) {
        AppUpdateManager.setUpdateCheckUrl(url);
    }

    @JavascriptInterface
    public void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
