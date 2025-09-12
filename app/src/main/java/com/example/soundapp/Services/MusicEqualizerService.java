package com.example.soundapp.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
        startForeground(1, buildNotification());
        applySavedEqualizer(this);
    }
    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Equalizer Service")
                .setContentText("Equalizer is applied after boot")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
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

    private void applySavedEqualizer(Context context) {
        try {
            int audioSessionId = 0; // 0 = tất cả media playback
            Equalizer equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);

            SharedPreferences prefs = context.getSharedPreferences("EQ_PREFS", Context.MODE_PRIVATE);
            short numberOfBands = equalizer.getNumberOfBands();

            for (short i = 0; i < numberOfBands; i++) {
                int level = prefs.getInt("band" + i, 0);
                equalizer.setBandLevel(i, (short) level);
                Log.d(TAG, "Set band " + i + " = " + level);
            }

            Log.d(TAG, "Equalizer applied successfully in service");

        } catch (Exception e) {
            Log.e(TAG, "Error applying equalizer: " + e.getMessage());
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

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent != null && "apply_eq".equals(intent.getStringExtra("action"))) {
//            String packageName = intent.getStringExtra("packageName");
//            int sessionId = intent.getIntExtra("audioSessionId", 0);
//            applyEqualizerToSession(sessionId, packageName);
//        }
//        return START_STICKY;
//    }

    @Override
    public void onDestroy() {
//        super.onDestroy();
//        if (globalEqualizer != null) globalEqualizer.release();
//        if (globalBassBoost != null) globalBassBoost.release();
//
//        for (Equalizer eq : activeEqualizers.values()) if (eq != null) eq.release();
//        for (BassBoost bb : activeBassBoosts.values()) if (bb != null) bb.release();
//
//        activeEqualizers.clear();
//        activeBassBoosts.clear();
        super.onDestroy();
        if (globalEqualizer != null) {
            globalEqualizer.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
