package com.menteestoica.enquiridion;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class StoicWallpaperService extends Service implements Runnable {
    private static final String CHANNEL = "stoic_wallpaper";
    private Handler handler;
    private BroadcastReceiver unlockReceiver;

    @Override public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        unlockReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!Intent.ACTION_USER_PRESENT.equals(intent.getAction())) return;
                int mode = WallpaperRenderer.mode(StoicWallpaperService.this);
                if (mode == WallpaperRenderer.MODE_UNLOCK_NEXT) {
                    WallpaperRenderer.applyNext(StoicWallpaperService.this);
                } else if (mode == WallpaperRenderer.MODE_UNLOCK_RANDOM) {
                    WallpaperRenderer.applyRandomDifferent(StoicWallpaperService.this);
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(unlockReceiver, filter);
        }
        startAsForeground();
    }

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Enquiridión dinámico", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(channel);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Enquiridión estoico")
                .setContentText("Cambio automático de máximas activo")
                .setOngoing(true)
                .build();
        startForeground(71, notification);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        schedule();
        return START_STICKY;
    }

    private long intervalForMode(int mode) {
        if (mode == WallpaperRenderer.MODE_15_MIN) return 15L * 60L * 1000L;
        if (mode == WallpaperRenderer.MODE_1_HOUR) return 60L * 60L * 1000L;
        if (mode == WallpaperRenderer.MODE_6_HOURS) return 6L * 60L * 60L * 1000L;
        if (mode == WallpaperRenderer.MODE_24_HOURS) return 24L * 60L * 60L * 1000L;
        return 0L;
    }

    private void schedule() {
        handler.removeCallbacks(this);
        long delay = intervalForMode(WallpaperRenderer.mode(this));
        if (delay > 0) handler.postDelayed(this, delay);
    }

    @Override public void run() {
        WallpaperRenderer.applyNext(this);
        schedule();
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(this);
        if (unlockReceiver != null) unregisterReceiver(unlockReceiver);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
