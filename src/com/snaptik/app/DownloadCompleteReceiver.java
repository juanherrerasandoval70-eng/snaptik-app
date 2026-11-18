package com.snaptik.app;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;

public class DownloadCompleteReceiver extends BroadcastReceiver {
    private final Context context;

    public DownloadCompleteReceiver(Context context) {
        this.context = context;
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent != null && DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId != -1) {
                scanDownloadedFile(downloadId);
            }
        }
    }

    private void scanDownloadedFile(long downloadId) {
        try {
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm == null) return;
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor c = dm.query(query);
            if (c != null && c.moveToFirst()) {
                int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (statusIdx != -1 && c.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL) {
                    int uriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    if (uriIdx != -1) {
                        String localUri = c.getString(uriIdx);
                        if (localUri != null) {
                            Uri uri = Uri.parse(localUri);
                            String path = uri.getPath();
                            if (path != null) {
                                MediaScannerConnection.scanFile(context, new String[]{path}, null, null);
                            }
                        }
                    }
                }
                c.close();
            }
        } catch (Exception ignored) {}
    }
}
