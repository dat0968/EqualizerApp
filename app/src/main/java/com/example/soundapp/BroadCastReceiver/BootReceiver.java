package com.example.soundapp.BroadCastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.example.soundapp.Listener.MyNotificationListener;
import com.example.soundapp.Services.MusicEqualizerService;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {

            Log.d(TAG, "Boot completed or package updated, starting MusicEqualizerService");

            // Check if user has enabled the service
            SharedPreferences prefs = context.getSharedPreferences("EQ_PREFS", Context.MODE_PRIVATE);
            boolean serviceEnabled = prefs.getBoolean("service_enabled", true);

            if (serviceEnabled) {
                // Delay startup slightly to ensure system is ready
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    startEqualizerService(context);
                }, 3000); // 3 second delay
            }
        }
    }

    private void startEqualizerService(Context context) {
        try {
            // Kiểm tra Notification Access
//            if (!isNotificationListenerEnabled(context)) {
//                // Dẫn user bật quyền Notification Access
//                Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // quan trọng khi start từ BroadcastReceiver
//                context.startActivity(intent);
//                Log.w(TAG, "Notification Access not enabled, opening settings");
//            }
            Intent serviceIntent = new Intent(context, MusicEqualizerService.class);
            serviceIntent.putExtra("boot_startup", true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            Log.d(TAG, "MusicEqualizerService started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start MusicEqualizerService", e);
        }
    }
//    private boolean isNotificationListenerEnabled(Context context) {
//        String enabledListeners = Settings.Secure.getString(
//                context.getContentResolver(),
//                "enabled_notification_listeners"
//        );
//        if (enabledListeners == null) return false;
//
//        // Tên class đầy đủ của listener
//        String listener = context.getPackageName() + "/" + MyNotificationListener.class.getName();
//        return enabledListeners.contains(listener);
//    }


}