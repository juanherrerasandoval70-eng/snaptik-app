package com.snaptik.app;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

public class WebAppInterface {
    private final Context context;
    private final WebView webView;
    private DownloadCompleteReceiver downloadReceiver;

    public WebAppInterface(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        AdNetworkManager.initFromPrefs(context);
        registerDownloadReceiver();
    }

    private void registerDownloadReceiver() {
        try {
            downloadReceiver = new DownloadCompleteReceiver(context.getApplicationContext());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        } catch (Exception ignored) {}
    }

    public void cleanup() {
        if (downloadReceiver != null) {
            try {
                context.unregisterReceiver(downloadReceiver);
            } catch (Exception ignored) {}
            downloadReceiver = null;
        }
    }

    @JavascriptInterface
    public void extract(String tiktokUrl) {
        vibrate(30);
        if (!AppSecurityGuard.verifySignature(context)) {
            showToast("Alerta de seguridad: APK modificado o firma inválida.");
            return;
        }
        if (AppSecurityGuard.isFridaOrHookingDetected()) {
            showToast("Entorno no autorizado detectado.");
            return;
        }
        if (AppSecurityGuard.isDebuggerActive()) {
            showToast("Depuración detectada: Extracción denegada.");
            return;
        }
        new Thread(new ExtractTaskRunnable(webView, tiktokUrl)).start();
    }

    @JavascriptInterface
    public void download(final String fileUrl, final String filename) {
        vibrate(50);
        try {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                Toast.makeText(context, "Enlace de descarga vacío.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (fileUrl.startsWith("data:")) {
                int commaIdx = fileUrl.indexOf(',');
                if (commaIdx > 0) {
                    byte[] rawBytes = android.util.Base64.decode(fileUrl.substring(commaIdx + 1), android.util.Base64.DEFAULT);
                    java.io.File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists()) dir.mkdirs();
                    java.io.File outFile = new java.io.File(dir, filename);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile);
                    fos.write(rawBytes);
                    fos.flush();
                    fos.close();
                    android.media.MediaScannerConnection.scanFile(context, new String[]{outFile.getAbsolutePath()}, null, null);
                    dispatchDownloadComplete(filename, outFile.getAbsolutePath(), false, "14.5 MB/s");
                    showSystemDownloadCompleteNotification(context, filename);
                    Toast.makeText(context, "✓ Guardado en Galería: " + filename, Toast.LENGTH_SHORT).show();
                    if (AdNetworkManager.recordDownloadAndCheckAdTrigger(context) && context instanceof android.app.Activity) {
                        AdNetworkManager.triggerInterstitial((android.app.Activity) context, webView);
                    }
                    return;
                }
            }

            // Descarga directa acelerada en hilo secundario (Videos MP4, Audios MP3 e Imágenes)
            // Utiliza búfer optimizado de 128 KB, conexión directa HTTP/HTTPS y fallback a DownloadManager
            downloadFastMediaDirectly(fileUrl, filename);
        } catch (Exception e) {
            Toast.makeText(context, "Error al descargar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @JavascriptInterface
    public void downloadQuiet(final String fileUrl, final String filename) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) return;
            if (fileUrl.startsWith("data:")) {
                int commaIdx = fileUrl.indexOf(',');
                if (commaIdx > 0) {
                    byte[] rawBytes = android.util.Base64.decode(fileUrl.substring(commaIdx + 1), android.util.Base64.DEFAULT);
                    java.io.File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists()) dir.mkdirs();
                    java.io.File outFile = new java.io.File(dir, filename);
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile);
                    fos.write(rawBytes);
                    fos.flush();
                    fos.close();
                    android.media.MediaScannerConnection.scanFile(context, new String[]{outFile.getAbsolutePath()}, null, null);
                    dispatchDownloadComplete(filename, outFile.getAbsolutePath(), false, "12.0 MB/s");
                    return;
                }
            }
            new Thread(new FastMediaDownloader(context, fileUrl, filename, this, true)).start();
        } catch (Exception ignored) {}
    }

    public void dispatchDownloadProgress(final String filename, final int percent, final long downloaded, final long total, final String speedLabel) {
        if (webView != null) {
            final String safeName = filename != null ? filename.replace("'", "\\'") : "";
            final String safeSpeed = speedLabel != null ? speedLabel.replace("'", "\\'") : "";
            final String js = "if(typeof window.onFastDownloadProgress==='function'){window.onFastDownloadProgress('" + safeName + "', " + percent + ", " + downloaded + ", " + total + ", '" + safeSpeed + "');}";
            webView.post(new EvaluateJsRunnable(webView, js));
        }
    }

    public void dispatchDownloadComplete(final String filename, final String filePath, final boolean isVideo, final String speedLabel) {
        if (webView != null) {
            final String safeName = filename != null ? filename.replace("'", "\\'") : "";
            final String safePath = filePath != null ? filePath.replace("\\", "/").replace("'", "\\'") : "";
            final String safeSpeed = speedLabel != null ? speedLabel.replace("'", "\\'") : "";
            final String js = "if(typeof window.onFastDownloadComplete==='function'){window.onFastDownloadComplete('" + safeName + "', '" + safePath + "', " + isVideo + ", '" + safeSpeed + "');}";
            webView.post(new EvaluateJsRunnable(webView, js));
        }
    }

    public void dispatchDownloadFallback(final String filename) {
        if (webView != null) {
            final String safeName = filename != null ? filename.replace("'", "\\'") : "";
            final String js = "if(typeof window.onFastDownloadFallback==='function'){window.onFastDownloadFallback('" + safeName + "');}";
            webView.post(new EvaluateJsRunnable(webView, js));
        }
    }

    public static void showSystemDownloadCompleteNotification(Context ctx, String filename) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            String channelId = "snaptik_media_dl";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    channelId, "Descargas SnapTik PRO", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Notificaciones de descargas multimedia finalizadas");
                nm.createNotificationChannel(channel);
            }

            Intent openIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE) :
                PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getActivity(ctx, (int) (System.currentTimeMillis() % 100000), openIntent, flags);

            Notification.Builder nb;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nb = new Notification.Builder(ctx, channelId);
            } else {
                nb = new Notification.Builder(ctx);
            }
            nb.setSmallIcon(android.R.drawable.stat_sys_download_done)
              .setContentTitle("✓ Descarga completada")
              .setContentText(filename + " guardado en Galería")
              .setContentIntent(pi)
              .setAutoCancel(true);

            nm.notify((int) (System.currentTimeMillis() % 100000), nb.build());
        } catch (Exception ignored) {}
    }

    @JavascriptInterface
    public void openDownloadedFile(String filename) {
        vibrate(20);
        try {
            java.io.File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            java.io.File file = new java.io.File(dir, filename);
            if (!file.exists()) {
                openDownloadsFolder();
                return;
            }
            try {
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
            } catch (Exception ignored) {}

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(file);
            String mime = "video/mp4";
            String lower = filename.toLowerCase();
            if (lower.endsWith(".mp3")) mime = "audio/mpeg";
            else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mime = "image/jpeg";
            else if (lower.endsWith(".png")) mime = "image/png";
            else if (lower.endsWith(".webp")) mime = "image/webp";

            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            openDownloadsFolder();
        }
    }

    private static class TriggerInterstitialRunnable implements Runnable {
        private final android.app.Activity activity;
        private final WebView webView;

        TriggerInterstitialRunnable(android.app.Activity activity, WebView webView) {
            this.activity = activity;
            this.webView = webView;
        }

        @Override
        public void run() {
            AdNetworkManager.triggerInterstitial(activity, webView);
        }
    }

    public void triggerInterstitialOnUi() {
        if (context instanceof android.app.Activity) {
            android.app.Activity act = (android.app.Activity) context;
            act.runOnUiThread(new TriggerInterstitialRunnable(act, webView));
        }
    }

    private void downloadFastMediaDirectly(final String fileUrl, final String filename) {
        new Thread(new FastMediaDownloader(context, fileUrl, filename, this, false)).start();
    }

    private static class FastMediaDownloader implements Runnable {
        private final Context ctx;
        private final String fileUrl;
        private final String filename;
        private final WebAppInterface webApp;
        private final boolean quietMode;

        FastMediaDownloader(Context ctx, String fileUrl, String filename, WebAppInterface webApp, boolean quietMode) {
            this.ctx = ctx;
            this.fileUrl = fileUrl;
            this.filename = filename;
            this.webApp = webApp;
            this.quietMode = quietMode;
        }

        private String formatSpeed(long bytes, long elapsedMs) {
            if (elapsedMs <= 0) elapsedMs = 1;
            double mbPerSec = (bytes / (1024.0 * 1024.0)) / (elapsedMs / 1000.0);
            if (mbPerSec < 0.1) mbPerSec = 0.1;
            if (mbPerSec > 99.9) mbPerSec = 99.9;
            return String.format(java.util.Locale.US, "%.1f MB/s", mbPerSec);
        }

        @Override
        public void run() {
            java.io.InputStream is = null;
            java.io.BufferedInputStream bis = null;
            java.io.FileOutputStream fos = null;
            java.io.BufferedOutputStream bos = null;
            try {
                java.net.URL url = new java.net.URL(fileUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
                String referer = (fileUrl.contains("tikcdn.io") || fileUrl.contains("ssstik")) ? "https://ssstik.io/" : "https://www.tiktok.com/";
                conn.setRequestProperty("Referer", referer);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);

                int code = conn.getResponseCode();
                if (code >= 200 && code < 400) {
                    java.io.File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!dir.exists()) dir.mkdirs();
                    java.io.File outFile = new java.io.File(dir, filename);
                    if (outFile.exists()) {
                        outFile.delete();
                    }

                    is = conn.getInputStream();
                    bis = new java.io.BufferedInputStream(is, 131072);
                    fos = new java.io.FileOutputStream(outFile);
                    bos = new java.io.BufferedOutputStream(fos, 131072);

                    long startMs = System.currentTimeMillis();
                    long totalBytes = conn.getContentLengthLong();
                    long downloadedBytes = 0;
                    byte[] buffer = new byte[131072]; // Búfer de 128 KB para saturación óptima de ancho de banda
                    int bytesRead;
                    int lastReported = 0;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;
                        if (totalBytes > 0) {
                            int pct = (int) ((downloadedBytes * 100) / totalBytes);
                            if (pct >= lastReported + 8 || pct == 100) {
                                lastReported = pct;
                                String spd = formatSpeed(downloadedBytes, System.currentTimeMillis() - startMs);
                                webApp.dispatchDownloadProgress(filename, pct, downloadedBytes, totalBytes, spd);
                            }
                        }
                    }
                    bos.flush();

                    String finalSpeed = formatSpeed(downloadedBytes, System.currentTimeMillis() - startMs);
                    android.media.MediaScannerConnection.scanFile(ctx, new String[]{outFile.getAbsolutePath()}, null, null);
                    webApp.dispatchDownloadComplete(filename, outFile.getAbsolutePath(), filename.toLowerCase().endsWith(".mp4"), finalSpeed);
                    if (!quietMode) {
                        showSystemDownloadCompleteNotification(ctx, filename);
                        webApp.vibrate(25);
                        if (AdNetworkManager.recordDownloadAndCheckAdTrigger(ctx)) {
                            webApp.triggerInterstitialOnUi();
                        }
                    }
                } else {
                    webApp.dispatchDownloadFallback(filename);
                    webApp.downloadViaDownloadManager(fileUrl, filename);
                }
            } catch (Exception e) {
                webApp.dispatchDownloadFallback(filename);
                webApp.downloadViaDownloadManager(fileUrl, filename);
            } finally {
                try { if (bos != null) bos.close(); } catch (Exception ignored) {}
                try { if (fos != null) fos.close(); } catch (Exception ignored) {}
                try { if (bis != null) bis.close(); } catch (Exception ignored) {}
                try { if (is != null) is.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void downloadViaDownloadManager(String fileUrl, String filename) {
        try {
            try {
                java.io.File destFile = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
                if (destFile.exists()) {
                    destFile.delete();
                }
            } catch (Exception ignored) {}

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle("SnapTik: " + filename);
            request.setDescription("Guardando en Galería...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            request.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
            String referer = (fileUrl.contains("tikcdn.io") || fileUrl.contains("ssstik")) ? "https://ssstik.io/" : "https://www.tiktok.com/";
            request.addRequestHeader("Referer", referer);
            request.allowScanningByMediaScanner();

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(context, "Descarga iniciada. Visible en tu galería.", Toast.LENGTH_SHORT).show();
                if (AdNetworkManager.recordDownloadAndCheckAdTrigger(context)) {
                    if (context instanceof android.app.Activity) {
                        AdNetworkManager.triggerInterstitial((android.app.Activity) context, webView);
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error al encolar descarga: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    public void clickNativeAd() {
        if (context instanceof android.app.Activity) {
            StartIoRewardManager.clickNativeAd((android.app.Activity) context);
        }
    }

    @JavascriptInterface
    public void showInterstitialNow() {
        if (context instanceof android.app.Activity) {
            StartIoRewardManager.showInterstitial((android.app.Activity) context);
        }
    }

    @JavascriptInterface
    public String getStartIoStatus() {
        return StartIoRewardManager.getDiagnosticsJson();
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
    public boolean isAgeVerified() {
        return AdNetworkManager.isAgeVerified(context);
    }

    @JavascriptInterface
    public boolean isAdultModeEnabled() {
        return AdNetworkManager.isAdult18PlusEnabled(context);
    }

    @JavascriptInterface
    public void setAgePreference(boolean isAdult18Plus) {
        vibrate(20);
        AdNetworkManager.setAgePreference(context, isAdult18Plus);
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

    private static class ToastRunnable implements Runnable {
        private final Context ctx;
        private final String msg;
        ToastRunnable(Context ctx, String msg) {
            this.ctx = ctx;
            this.msg = msg;
        }
        @Override
        public void run() {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }
    }

    @JavascriptInterface
    public void showToast(final String message) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new ToastRunnable(context, message));
    }
}
