package com.example.soundapp.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EqualizerService extends Service {
    public static final String ACTION_READY = "com.example.soundapp.READY";
    public static final String ACTION_SET_BAND = "com.example.soundapp.SET_BAND";
    public static final String EXTRA_BAND = "band";
    public static final String EXTRA_LEVEL = "level";

    public static final String ACTION_USE_PRESET = "com.example.soundapp.USE_PRESET";
    public static final String EXTRA_PRESET_POS = "preset_position";
    private static final String TAG = "EqualizerService";
    private static final String CHANNEL_ID = "EqualizerServiceChannel";
    private static boolean readyPresent = true;
    private Equalizer equalizer;
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, buildNotification());
        }
        equalizer = new Equalizer(0, 0);
        equalizer.setEnabled(true);
        applySavedEqualizer();
        //LoadDataPresent();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences("EQ_PREFS", MODE_PRIVATE);
        if(intent != null && ACTION_USE_PRESET.equals(intent.getAction())){
            int position = intent.getIntExtra(EXTRA_PRESET_POS, 0);
            equalizer.usePreset((short) position);
            Log.d("Spinner", "Vị trí Preset được chọn: " + position);
            for (short b = 0; b < equalizer.getNumberOfBands(); b++) {
                prefs.edit().putInt("band" + b, equalizer.getBandLevel(b)).commit();
            }
            applySavedEqualizer();
        }
        if (intent != null && ACTION_SET_BAND.equals(intent.getAction())) {
            int band = intent.getIntExtra(EXTRA_BAND, 0);
            int level = intent.getIntExtra(EXTRA_LEVEL, 0);
            try {
                equalizer.setBandLevel((short) band, (short) level);

                prefs.edit().putInt("band" + band, level).apply();
                applySavedEqualizer();

            } catch (Exception e) {
                Log.e(TAG, "setBandLevel failed", e);
            }
        }
        if(readyPresent){
            LoadDataPresent();
            readyPresent = false;
        }
        Intent readyIntent = new Intent(ACTION_READY);
        sendBroadcast(readyIntent);
        return START_STICKY;
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
    public void LoadDataPresent(){
        if(equalizer == null) return;
        SharedPreferences prefs = getSharedPreferences("EQ_PREFS", Context.MODE_PRIVATE);
        int numberOfPresets = equalizer.getNumberOfPresets();
        prefs.edit().putInt("numberOfPresets", numberOfPresets).commit();
        for (short i = 0; i < numberOfPresets; i++) {
            prefs.edit().putString("preset" + i, equalizer.getPresetName(i)).commit();
        }
    }
    private void applySavedEqualizer() {
        try {
            if (equalizer == null) return;
            SharedPreferences prefs = getSharedPreferences("EQ_PREFS", Context.MODE_PRIVATE);
            short minLevel = equalizer.getBandLevelRange()[0];
            short maxLevel = equalizer.getBandLevelRange()[1];
            int numberOfSteps = 20;
            short numberOfBands = equalizer.getNumberOfBands();
            for (short band = 0; band < numberOfBands; band++) {
                int savedProgress = prefs.getInt("band" + band, 50);
                float normalized = savedProgress / 100f;
                short level = (short) (minLevel + (maxLevel - minLevel) * Math.pow(normalized, 2));
                equalizer.setBandLevel(band, level);
            }

            Log.d(TAG, "Equalizer applied successfully in service");

        } catch (Exception e) {
            Log.e(TAG, "Error applying equalizer: " + e.getMessage());
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (equalizer  != null) {
            equalizer.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
