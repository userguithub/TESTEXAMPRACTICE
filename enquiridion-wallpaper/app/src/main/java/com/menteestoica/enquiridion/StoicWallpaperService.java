package com.menteestoica.enquiridion;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoicWallpaperService extends Service implements Runnable {
    private static final String CHANNEL = "stoic_wallpaper";
    private Handler handler;
    private BroadcastReceiver screenReceiver;
    private ExecutorService executor;
    private boolean unlockHandledForCycle = true;
    private int unlockChecksRemaining = 0;

    @Override public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();
        registerScreenEvents();
        startAsForeground();
    }

    private void registerScreenEvents() {
        screenReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    unlockHandledForCycle = false;
                    unlockChecksRemaining = 0;
                    onLockEvent();
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    unlockChecksRemaining = 40; // hasta ~10 s para biometría/PIN
                    checkForUnlockedState();
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    onUnlockEventOnce();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        // Solo escuchamos broadcasts del sistema; Android 14+ permite registrarlo sin flags.
        registerReceiver(screenReceiver, filter);
    }

    private void onLockEvent() {
        final int mode = WallpaperRenderer.mode(this);
        if (mode == WallpaperRenderer.MODE_LOCK_NEXT || mode == WallpaperRenderer.MODE_LOCK_UNLOCK_NEXT) {
            executor.execute(() -> WallpaperRenderer.applyNext(this));
        } else if (mode == WallpaperRenderer.MODE_LOCK_RANDOM || mode == WallpaperRenderer.MODE_LOCK_UNLOCK_RANDOM) {
            executor.execute(() -> WallpaperRenderer.applyRandomDifferent(this));
        }
    }

    private void onUnlockEventOnce() {
        if (unlockHandledForCycle) return;
        unlockHandledForCycle = true;
        unlockChecksRemaining = 0;

        final int mode = WallpaperRenderer.mode(this);
        if (mode == WallpaperRenderer.MODE_UNLOCK_NEXT || mode == WallpaperRenderer.MODE_LOCK_UNLOCK_NEXT) {
            executor.execute(() -> WallpaperRenderer.applyNext(this));
        } else if (mode == WallpaperRenderer.MODE_UNLOCK_RANDOM || mode == WallpaperRenderer.MODE_LOCK_UNLOCK_RANDOM) {
            executor.execute(() -> WallpaperRenderer.applyRandomDifferent(this));
        }
    }

    private void checkForUnlockedState() {
        handler.removeCallbacks(unlockCheckRunnable);
        handler.postDelayed(unlockCheckRunnable, 250);
    }

    private final Runnable unlockCheckRunnable = new Runnable() {
        @Override public void run() {
            if (unlockHandledForCycle || unlockChecksRemaining <= 0) return;

            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && !km.isKeyguardLocked()) {
                onUnlockEventOnce();
                return;
            }

            unlockChecksRemaining--;
            handler.postDelayed(this, 250);
        }
    };

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL, "Enquiridión dinámico", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Enquiridión estoico")
                .setContentText("Rotación automática de máximas activa")
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
        executor.execute(() -> WallpaperRenderer.applyNext(this));
        schedule();
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(this);
        handler.removeCallbacks(unlockCheckRunnable);
        if (screenReceiver != null) {
            try { unregisterReceiver(screenReceiver); } catch (Exception ignored) {}
        }
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
