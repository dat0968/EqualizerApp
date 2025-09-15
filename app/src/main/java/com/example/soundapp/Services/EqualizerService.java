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
        SharedPreferences prefs = getSharedPreferences("EQ_PREFS", MODE_PRIVATE);
        int numberOfPresets = equalizer.getNumberOfPresets();
        int position = prefs.getInt("selectedPreset", numberOfPresets);

        if(position >= 0 && position < numberOfPresets){
            equalizer.usePreset((short) position);
        }

        Log.d("Spinner", "Vị trí Preset được chọn: " + position);
        prefs.edit().putInt("selectedPreset", position).commit();
        for (short b = 0; b < equalizer.getNumberOfBands(); b++) {
            if(position >= 0 && position < numberOfPresets){
                prefs.edit().putInt("band" + b, equalizer.getBandLevel(b)).commit();
            }else{
                int customLevel = prefs.getInt("bandCustom" + b, prefs.getInt("band" + b, 0));
                prefs.edit().putInt("band" + b, customLevel

                ).commit();
            }

        }

        applySavedEqualizer();
        //LoadDataPresent();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences("EQ_PREFS", MODE_PRIVATE);
        if(intent != null && ACTION_USE_PRESET.equals(intent.getAction())){
            int position = intent.getIntExtra(EXTRA_PRESET_POS, 0);
            int numberOfPresets = equalizer.getNumberOfPresets();
            if(position >= 0 && position < numberOfPresets){
                equalizer.usePreset((short) position);
                for (short b = 0; b < equalizer.getNumberOfBands(); b++) {
                    prefs.edit().putInt("band" + b, equalizer.getBandLevel(b)).commit();
                }
            }else {
                for (short b = 0; b < equalizer.getNumberOfBands(); b++) {
                    // đọc bandCustom, nếu chưa có fallback về 0
                    int customLevel = prefs.getInt("bandCustom" + b, prefs.getInt("band" + b, 0));
                    equalizer.setBandLevel(b, (short) customLevel);
                    prefs.edit().putInt("band" + b, customLevel).commit();
                }
            }
            Log.d("Spinner", "Vị trí Preset được chọn: " + position);
            prefs.edit().putInt("selectedPreset", position).commit();
        }
        if (intent != null && ACTION_SET_BAND.equals(intent.getAction())) {
            int band = intent.getIntExtra(EXTRA_BAND, 0);
//            int level = intent.getIntExtra(EXTRA_LEVEL, 0);
            int level = prefs.getInt("band" + band, 0);
            Log.d("setupband", "setupbandservice: " + level);
            try {
                equalizer.setBandLevel((short) band, (short) level);
                int numberOfPresets = equalizer.getNumberOfPresets();
                int position = prefs.getInt("selectedPreset", -1);
                prefs.edit().putInt("selectedPreset", numberOfPresets).commit();
                prefs.edit().putInt("bandCustom" + band, level).commit();
                prefs.edit().putInt("band" + band, level).commit();

            } catch (Exception e) {
                Log.e(TAG, "setBandLevel failed", e);
            }
        }
        if(readyPresent){
            LoadDataPresent();
            readyPresent = false;
        }
        applySavedEqualizer();
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
            short numberOfBands = equalizer.getNumberOfBands();
            for (short band = 0; band < numberOfBands; band++) {
                int level = prefs.getInt("band" + band, 0);
                equalizer.setBandLevel(band, (short)level);
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
