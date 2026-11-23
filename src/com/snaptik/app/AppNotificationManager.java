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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppNotificationManager {

    public static final String CHANNEL_DOWNLOADS = "snaptik_media_dl";
    public static final String CHANNEL_ALERTS = "snaptik_alerts_chaching_v3";
    private static final String PREFS = "snaptik_notif_prefs";
    private static final String KEY_ENABLED = "notifs_enabled";
    private static final String KEY_LAST_ANNOUNCEMENT_ID = "last_announcement_id";
    private static final String ANNOUNCEMENT_URL = "https://snaptik-backend.onrender.com/api/notifications";
    private static final String STREAM_URL = "https://snaptik-backend.onrender.com/api/notifications/stream";
    private static volatile boolean liveStreamRunning = false;
    private static volatile HttpURLConnection liveStreamConn = null;
    private static final long[] CHACHING_VIBRATION = new long[]{0, 110, 65, 160};

    private static Uri getChaChingSoundUri(Context ctx) {
        try {
            int resId = ctx.getResources().getIdentifier("chaching", "raw", ctx.getPackageName());
            if (resId != 0) {
                return Uri.parse("android.resource://" + ctx.getPackageName() + "/" + resId);
            }
        } catch (Exception ignored) {}
        return android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
    }

    public static void ensureChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm == null) return;

                try {
                    nm.deleteNotificationChannel("snaptik_alerts_v1");
                    nm.deleteNotificationChannel("snaptik_alerts_chaching_v2");
                } catch (Exception ignored) {}

                NotificationChannel dl = new NotificationChannel(
                        CHANNEL_DOWNLOADS,
                        "Descargas Completadas",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                dl.setDescription("Avisos cuando tus vídeos, fotos o audios MP3 se guardan en la galería.");
                nm.createNotificationChannel(dl);

                NotificationChannel alerts = new NotificationChannel(
                        CHANNEL_ALERTS,
                        "Avisos SnapTik PRO (Cha-Ching)",
                        NotificationManager.IMPORTANCE_HIGH
                );
                alerts.setDescription("Notificaciones flotantes con banner y sonido Cha-Ching.");
                alerts.enableVibration(true);
                alerts.setVibrationPattern(CHACHING_VIBRATION);
                alerts.enableLights(true);
                alerts.setLightColor(0xFF38EFEE);
                alerts.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                AudioAttributes aa = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                alerts.setSound(getChaChingSoundUri(ctx), aa);
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

    private static Bitmap roundCorners(Bitmap src, float radiusPx) {
        if (src == null) return null;
        try {
            int w = src.getWidth();
            int h = src.getHeight();
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Rect rect = new Rect(0, 0, w, h);
            RectF rectF = new RectF(rect);
            canvas.drawRoundRect(rectF, radiusPx, radiusPx, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(src, rect, rect, paint);
            return out;
        } catch (Throwable ignored) {
            return src;
        }
    }

    private static Bitmap downloadBannerBitmap(Context ctx, String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("http")) return null;
        java.io.File cacheFile = null;
        try {
            String key = Integer.toHexString(imageUrl.hashCode());
            cacheFile = new java.io.File(ctx.getCacheDir(), "notif_banner_" + key + ".png");
            if (cacheFile.exists() && cacheFile.length() > 1024) {
                Bitmap cached = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
                if (cached != null) return cached;
            }
        } catch (Throwable ignored) {}

        HttpURLConnection conn = null;
        try {
            URL u = new URL(imageUrl);
            conn = (HttpURLConnection) u.openConnection(java.net.Proxy.NO_PROXY);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(6000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) SnapTikPRO/3.9.8");
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap raw = BitmapFactory.decodeStream(is, null, opts);
                is.close();
                if (raw == null) return null;
                int maxW = 520;
                Bitmap scaled = raw;
                if (raw.getWidth() > maxW) {
                    int newH = Math.max(1, (raw.getHeight() * maxW) / raw.getWidth());
                    scaled = Bitmap.createScaledBitmap(raw, maxW, newH, true);
                    if (scaled != raw) raw.recycle();
                }
                Bitmap rounded = roundCorners(scaled, 22f);
                if (rounded != scaled) scaled.recycle();
                if (cacheFile != null && rounded != null) {
                    try {
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile);
                        rounded.compress(Bitmap.CompressFormat.PNG, 95, fos);
                        fos.flush();
                        fos.close();
                    } catch (Throwable ignored) {}
                }
                return rounded;
            }
        } catch (Throwable t) {
            android.util.Log.e("SnapTikNotif", "Banner download error", t);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    public static void showNotification(Context ctx, String channelId, int notifId, String title, String message, String openUrl) {
        showNotificationWithBanner(ctx, channelId, notifId, title, message, "", openUrl);
    }

    public static boolean showNotificationWithBanner(Context ctx, String channelId, int notifId, String title, String message, String imageUrl, String openUrl) {
        if (!isNotificationsEnabled(ctx)) return false;
        ensureChannels(ctx);
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return false;

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

            Bitmap appIconBmp = null;
            try {
                Bitmap rawIcon = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_launcher);
                if (rawIcon != null) {
                    appIconBmp = Bitmap.createScaledBitmap(rawIcon, 128, 128, true);
                    if (appIconBmp != rawIcon) rawIcon.recycle();
                }
            } catch (Throwable ignored) {}

            Bitmap bannerBmp = downloadBannerBitmap(ctx, imageUrl);

            nb.setSmallIcon(R.drawable.ic_notif_snaptik)
              .setContentTitle(title)
              .setContentText(message)
              .setSubText("SnapTik PRO Oficial")
              .setShowWhen(true)
              .setWhen(System.currentTimeMillis())
              .setContentIntent(pi)
              .setAutoCancel(true)
              .setColor(0xFF38EFEE)
              .setPriority(Notification.PRIORITY_MAX)
              .setCategory(Notification.CATEGORY_PROMO)
              .setVisibility(Notification.VISIBILITY_PUBLIC);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String timeLabel = "• " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                Bitmap roundedIcon = (appIconBmp != null) ? roundCorners(appIconBmp, 24f) : null;

                Intent appIntent = new Intent(ctx, MainActivity.class);
                appIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent appPi = PendingIntent.getActivity(ctx, notifId + 1, appIntent, flags);

                RemoteViews compact = new RemoteViews(ctx.getPackageName(), R.layout.notif_snaptik_compact);
                compact.setTextViewText(R.id.notif_title, title);
                compact.setTextViewText(R.id.notif_body, message);
                compact.setTextViewText(R.id.notif_time_compact, timeLabel);
                compact.setOnClickPendingIntent(R.id.notif_cta, pi);
                if (roundedIcon != null) {
                    compact.setImageViewBitmap(R.id.notif_icon, roundedIcon);
                }
                if (bannerBmp != null) {
                    try {
                        Bitmap miniRaw = Bitmap.createScaledBitmap(bannerBmp, 112, 80, true);
                        Bitmap miniRounded = roundCorners(miniRaw, 14f);
                        if (miniRounded != miniRaw) miniRaw.recycle();
                        compact.setImageViewBitmap(R.id.notif_thumb_compact, miniRounded);
                        compact.setViewVisibility(R.id.notif_thumb_compact, View.VISIBLE);
                    } catch (Throwable ignored) {
                        compact.setViewVisibility(R.id.notif_thumb_compact, View.GONE);
                    }
                } else {
                    compact.setViewVisibility(R.id.notif_thumb_compact, View.GONE);
                }

                RemoteViews expanded = new RemoteViews(ctx.getPackageName(), R.layout.notif_snaptik_expanded);
                expanded.setTextViewText(R.id.notif_title_big, title);
                expanded.setTextViewText(R.id.notif_body_big, message);
                expanded.setTextViewText(R.id.notif_time_big, timeLabel);
                expanded.setOnClickPendingIntent(R.id.notif_btn_paste, appPi);
                expanded.setOnClickPendingIntent(R.id.notif_cta_big, pi);
                if (roundedIcon != null) {
                    expanded.setImageViewBitmap(R.id.notif_icon_big, roundedIcon);
                }
                if (bannerBmp != null) {
                    expanded.setImageViewBitmap(R.id.notif_banner_big, bannerBmp);
                    expanded.setViewVisibility(R.id.notif_banner_wrap, View.VISIBLE);
                } else {
                    expanded.setViewVisibility(R.id.notif_banner_wrap, View.GONE);
                }

                nb.setCustomContentView(compact);
                nb.setCustomBigContentView(expanded);
                nb.setCustomHeadsUpContentView(compact);
            } else {
                if (appIconBmp != null) {
                    nb.setLargeIcon(appIconBmp);
                }
                if (bannerBmp != null) {
                    Notification.BigPictureStyle style = new Notification.BigPictureStyle()
                            .bigPicture(bannerBmp)
                            .setBigContentTitle(title)
                            .setSummaryText(message);
                    nb.setStyle(style);
                } else {
                    nb.setStyle(new Notification.BigTextStyle().bigText(message));
                }
            }

            try {
                nm.notify(notifId, nb.build());
                return true;
            } catch (Throwable binderErr) {
                android.util.Log.e("SnapTikNotif", "Custom notify failed, retrying standard style", binderErr);
                Notification.Builder fallback = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        ? new Notification.Builder(ctx, channelId)
                        : new Notification.Builder(ctx);
                fallback.setSmallIcon(R.drawable.ic_notif_snaptik)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .setColor(0xFF38EFEE)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setStyle(new Notification.BigTextStyle().bigText(message));
                nm.notify(notifId, fallback.build());
                return true;
            }
        } catch (Throwable t) {
            android.util.Log.e("SnapTikNotif", "showNotificationWithBanner failed", t);
            return false;
        }
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

    private static synchronized void processNotificationJson(Context ctx, String jsonStr) {
        try {
            if (jsonStr == null || jsonStr.trim().isEmpty()) return;
            JSONObject obj = new JSONObject(jsonStr);
            boolean active = obj.optBoolean("active", false);
            String id = obj.optString("id", "");
            String title = obj.optString("title", "");
            String body = obj.optString("body", "");
            String imageUrl = obj.optString("image_url", "");
            String actionUrl = obj.optString("url", "");

            if (active && !id.isEmpty() && !title.isEmpty() && !body.isEmpty()) {
                SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                String lastId = sp.getString(KEY_LAST_ANNOUNCEMENT_ID, "");
                if (!id.equals(lastId)) {
                    int uniqueNotifId = (int) (System.currentTimeMillis() & 0x7FFFFFFF);
                    boolean shown = showNotificationWithBanner(ctx, CHANNEL_ALERTS, uniqueNotifId, title, body, imageUrl, actionUrl);
                    if (shown) {
                        sp.edit().putString(KEY_LAST_ANNOUNCEMENT_ID, id).apply();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private static class LiveStreamWorker implements Runnable {
        private final Context appCtx;
        LiveStreamWorker(Context appCtx) {
            this.appCtx = appCtx;
        }
        @Override
        public void run() {
            while (liveStreamRunning) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(STREAM_URL);
                    conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
                    liveStreamConn = conn;
                    conn.setRequestMethod("GET");
                    conn.setUseCaches(false);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(35000);
                    conn.setRequestProperty("Accept", "text/event-stream");
                    conn.setRequestProperty("Cache-Control", "no-cache");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) SnapTikPRO/3.9.8");
                    if (conn.getResponseCode() == 200) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        String line;
                        while (liveStreamRunning && (line = br.readLine()) != null) {
                            if (line.startsWith("data:")) {
                                String payload = line.substring(5).trim();
                                processNotificationJson(appCtx, payload);
                            }
                        }
                        br.close();
                    }
                } catch (Throwable ignored) {
                } finally {
                    liveStreamConn = null;
                    if (conn != null) {
                        try { conn.disconnect(); } catch (Throwable ignored) {}
                    }
                }
                if (liveStreamRunning) {
                    try { Thread.sleep(3000); } catch (InterruptedException e) { break; }
                }
            }
        }
    }

    public static void startLiveStream(Context ctx) {
        if (!isNotificationsEnabled(ctx)) return;
        if (liveStreamRunning) return;
        liveStreamRunning = true;
        Thread t = new Thread(new LiveStreamWorker(ctx.getApplicationContext()));
        t.setDaemon(true);
        t.start();
    }

    public static void stopLiveStream() {
        liveStreamRunning = false;
        HttpURLConnection c = liveStreamConn;
        liveStreamConn = null;
        if (c != null) {
            try { c.disconnect(); } catch (Throwable ignored) {}
        }
    }

    public static void checkRemoteAnnouncementsSync(Context ctx) {
        if (!isNotificationsEnabled(ctx)) return;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(ANNOUNCEMENT_URL + "?t=" + System.currentTimeMillis());
            conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) SnapTikPRO/3.9.8");
            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                processNotificationJson(ctx, sb.toString());
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
