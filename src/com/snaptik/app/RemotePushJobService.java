package com.snaptik.app;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;

public class RemotePushJobService extends JobService {

    private static final int JOB_ID = 7791;

    public static void schedulePeriodicCheck(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        try {
            JobScheduler js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null) return;
            ComponentName cn = new ComponentName(ctx, RemotePushJobService.class);
            JobInfo info = new JobInfo.Builder(JOB_ID, cn)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(15 * 60 * 1000L)
                    .setPersisted(false)
                    .build();
            js.schedule(info);
        } catch (Exception ignored) {}
    }

    private static class PushCheckWorker implements Runnable {
        private final RemotePushJobService service;
        private final JobParameters params;

        PushCheckWorker(RemotePushJobService service, JobParameters params) {
            this.service = service;
            this.params = params;
        }

        @Override
        public void run() {
            try {
                AppNotificationManager.checkRemoteAnnouncementsSync(service.getApplicationContext());
            } finally {
                service.jobFinished(params, false);
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(new PushCheckWorker(this, params)).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
