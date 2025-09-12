package com.example.soundapp.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Map;

public class MusicEqualizerService extends Service {

    private static final String TAG = "MusicEQService";
    private static final String CHANNEL_ID = "eq_service_channel";
    private static final int NOTIFICATION_ID = 1;

    private Equalizer globalEqualizer;
    private BassBoost globalBassBoost;

    private Map<String, Equalizer> activeEqualizers = new HashMap<>();
    private Map<String, BassBoost> activeBassBoosts = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForegroundService();
        applyGlobalEqualizer();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Equalizer Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Audio Enhancement");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Audio Enhancement Active")
                .setContentText("Applying custom sound settings")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void applyGlobalEqualizer() {
        try {
            if (globalEqualizer != null) globalEqualizer.release();
            if (globalBassBoost != null) globalBassBoost.release();

            globalEqualizer = new Equalizer(0, 0);
            globalBassBoost = new BassBoost(0, 0);

            globalEqualizer.setEnabled(true);
            globalBassBoost.setEnabled(true);

            Log.d(TAG, "Global equalizer applied");
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply global equalizer", e);
        }
    }

    private void applyEqualizerToSession(int sessionId, String packageName) {
        try {
            if (activeEqualizers.containsKey(packageName)) return;

            Equalizer eq = new Equalizer(0, sessionId);
            BassBoost bb = new BassBoost(0, sessionId);

            eq.setEnabled(true);
            bb.setEnabled(true);

            activeEqualizers.put(packageName, eq);
            activeBassBoosts.put(packageName, bb);

            Log.d(TAG, "Applied EQ to " + packageName + " sessionId=" + sessionId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply EQ to " + packageName, e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "apply_eq".equals(intent.getStringExtra("action"))) {
            String packageName = intent.getStringExtra("packageName");
            int sessionId = intent.getIntExtra("audioSessionId", 0);
            applyEqualizerToSession(sessionId, packageName);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (globalEqualizer != null) globalEqualizer.release();
        if (globalBassBoost != null) globalBassBoost.release();

        for (Equalizer eq : activeEqualizers.values()) if (eq != null) eq.release();
        for (BassBoost bb : activeBassBoosts.values()) if (bb != null) bb.release();

        activeEqualizers.clear();
        activeBassBoosts.clear();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
