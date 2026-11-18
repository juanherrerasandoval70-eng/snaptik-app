package com.snaptik.app;

import android.content.Context;
import android.widget.Toast;

public class ShowToastRunnable implements Runnable {
    private final Context context;
    private final String message;

    public ShowToastRunnable(Context context, String message) {
        this.context = context;
        this.message = message;
    }

    @Override
    public void run() {
        if (context != null && message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
