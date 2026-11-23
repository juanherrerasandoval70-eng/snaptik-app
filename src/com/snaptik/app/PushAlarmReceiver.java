package com.snaptik.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

public class PushAlarmReceiver extends BroadcastReceiver {

    private static final int ALARM_REQ_CODE = 7792;
    private static final long INTERVAL_MS = 5 * 60 * 1000L; // Cada 5 minutos en segundo plano

    public static void scheduleNextCheck(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            Intent intent = new Intent(ctx, PushAlarmReceiver.class);
            intent.setAction("com.snaptik.app.ACTION_CHECK_PUSH");
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                    : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, ALARM_REQ_CODE, intent, flags);
            long triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            }
        } catch (Throwable ignored) {}
    }

    private static class AlarmCheckWorker implements Runnable {
        private final Context appCtx;
        private final PendingResult pendingResult;

        AlarmCheckWorker(Context appCtx, PendingResult pendingResult) {
            this.appCtx = appCtx;
            this.pendingResult = pendingResult;
        }

        @Override
        public void run() {
            try {
                AppNotificationManager.checkRemoteAnnouncementsSync(appCtx);
            } catch (Throwable ignored) {
            } finally {
                scheduleNextCheck(appCtx);
                try {
                    pendingResult.finish();
                } catch (Throwable ignored) {}
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Context appCtx = context.getApplicationContext();
        if (!AppNotificationManager.isNotificationsEnabled(appCtx)) return;
        PendingResult pr = goAsync();
        new Thread(new AlarmCheckWorker(appCtx, pr)).start();
    }
}
