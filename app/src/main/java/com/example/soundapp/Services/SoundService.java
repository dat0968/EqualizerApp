package com.example.soundapp.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.soundapp.R;

public class SoundService extends Service {
    private Equalizer equalizer;
    @Override
    public void onCreate() {
        super.onCreate();
        equalizer = new Equalizer(0, 0);
        equalizer.setEnabled(true);
        createNotification();
        startForeground(1, createNotification());
    }
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return START_STICKY; // app bị kill vẫn cố khởi động lại service
//    }
    private Notification createNotification() {
        String channelId = "music_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Equalizer Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Equalizer đang chạy")
                .setContentText("Ứng dụng đang chạy nền để điều chỉnh âm thanh")
                .build();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
