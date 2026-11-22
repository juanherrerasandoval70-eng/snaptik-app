package com.snaptik.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppNotificationManager {

    public static final String CHANNEL_DOWNLOADS = "snaptik_media_dl";
    public static final String CHANNEL_ALERTS = "snaptik_alerts_v1";
    private static final String PREFS = "snaptik_notif_prefs";
    private static final String KEY_ENABLED = "notifs_enabled";
    private static final String KEY_LAST_ANNOUNCEMENT_ID = "last_announcement_id";
    private static final String ANNOUNCEMENT_URL = "https://snaptik-backend.onrender.com/api/notifications";

    public static void ensureChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm == null) return;

                NotificationChannel dl = new NotificationChannel(
                        CHANNEL_DOWNLOADS,
                        "Descargas Completadas",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                dl.setDescription("Avisos cuando tus vídeos, fotos o audios MP3 se guardan en la galería.");
                nm.createNotificationChannel(dl);

                NotificationChannel alerts = new NotificationChannel(
                        CHANNEL_ALERTS,
                        "Novedades y Avisos de SnapTik PRO",
                        NotificationManager.IMPORTANCE_HIGH
                );
                alerts.setDescription("Actualizaciones importantes, mejoras de calidad y avisos del equipo.");
                nm.createNotificationChannel(alerts);
            } catch (Exception ignored) {}
        }
    }

    public static void requestPermissionIfNeeded(Activity activity) {
        ensureChannels(activity);
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                if (activity.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 9012);
                }
            } catch (Exception ignored) {}
        }
    }

    public static boolean isNotificationsEnabled(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_ENABLED, true);
    }

    public static void setNotificationsEnabled(Context ctx, boolean enabled) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) {
            ensureChannels(ctx);
        }
    }

    public static void showNotification(Context ctx, String channelId, int notifId, String title, String message, String openUrl) {
        if (!isNotificationsEnabled(ctx)) return;
        ensureChannels(ctx);
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            Intent intent;
            if (openUrl != null && openUrl.startsWith("http")) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(openUrl));
            } else {
                intent = new Intent(ctx, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                    : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getActivity(ctx, notifId, intent, flags);

            Notification.Builder nb = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ? new Notification.Builder(ctx, channelId)
                    : new Notification.Builder(ctx);

            nb.setSmallIcon(R.drawable.ic_launcher)
              .setContentTitle(title)
              .setContentText(message)
              .setStyle(new Notification.BigTextStyle().bigText(message))
              .setContentIntent(pi)
              .setAutoCancel(true);

            nm.notify(notifId, nb.build());
        } catch (Exception ignored) {}
    }

    private static class CheckAnnouncementRunnable implements Runnable {
        private final Context ctx;
        CheckAnnouncementRunnable(Context ctx) {
            this.ctx = ctx;
        }
        @Override
        public void run() {
            checkRemoteAnnouncementsSync(ctx);
        }
    }

    public static void checkRemoteAnnouncements(Context ctx) {
        if (!isNotificationsEnabled(ctx)) return;
        new Thread(new CheckAnnouncementRunnable(ctx.getApplicationContext())).start();
    }

    public static void checkRemoteAnnouncementsSync(Context ctx) {
        if (!isNotificationsEnabled(ctx)) return;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(ANNOUNCEMENT_URL);
            conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) SnapTikPRO/3.9.2");
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject obj = new JSONObject(sb.toString());
                boolean active = obj.optBoolean("active", false);
                String id = obj.optString("id", "");
                String title = obj.optString("title", "");
                String body = obj.optString("body", "");
                String actionUrl = obj.optString("url", "");

                if (active && !id.isEmpty() && !title.isEmpty() && !body.isEmpty()) {
                    SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                    String lastId = sp.getString(KEY_LAST_ANNOUNCEMENT_ID, "");
                    if (!id.equals(lastId)) {
                        sp.edit().putString(KEY_LAST_ANNOUNCEMENT_ID, id).apply();
                        showNotification(ctx, CHANNEL_ALERTS, 7701, title, body, actionUrl);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
