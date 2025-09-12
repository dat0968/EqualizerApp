package com.example.soundapp.Listener;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;

import java.util.List;

public class MyNotificationListener extends NotificationListenerService {

    private static final String TAG = "MyNotificationListener";

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "Notification listener connected");

        if (isNotificationListenerEnabled(this)) {
            checkActiveMediaSessions();
        } else {
            Log.w(TAG, "Notification Access not enabled by user");
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn.getNotification() != null) {
            CharSequence ticker = sbn.getNotification().tickerText;
            Log.d(TAG, "Notification posted: " + sbn.getPackageName() +
                    " - " + (ticker != null ? ticker : "null"));
        } else {
            Log.d(TAG, "Notification posted: " + sbn.getPackageName() + " - null notification");
        }
        Notification notification = sbn.getNotification();
        CharSequence ticker = notification.tickerText;
        // Có thể gửi broadcast hoặc callback cho MusicEqualizerService
        Intent broadcastIntent = new Intent("com.example.soundapp.ACTION_MEDIA_NOTIFICATION");
        broadcastIntent.putExtra("packageName", sbn.getPackageName());
        broadcastIntent.putExtra("tickerText", ticker != null ? ticker.toString() : "null");
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed: " + sbn.getPackageName());
        // Gửi broadcast để service cleanup EQ nếu cần
        Intent broadcastIntent = new Intent("com.example.soundapp.ACTION_MEDIA_REMOVED");
        broadcastIntent.putExtra("packageName", sbn.getPackageName());
        sendBroadcast(broadcastIntent);
    }

    /**
     * Kiểm tra active MediaSession (chỉ khi quyền được bật)
     */
    private void checkActiveMediaSessions() {
        try {
            if (!isNotificationListenerEnabled(this)) {
                Log.w(TAG, "Notification listener not enabled by user");
                return;
            }
            MediaSessionManager mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            ComponentName listenerComponent = new ComponentName(this, MyNotificationListener.class);


            if (mediaSessionManager == null) {
                Log.w(TAG, "MediaSessionManager is null");
                return;
            }
            List<MediaController> controllers = mediaSessionManager.getActiveSessions(listenerComponent);
            Log.d(TAG, "Active media sessions: " + controllers.size());

            for (MediaController controller : controllers) {
                String packageName = controller.getPackageName();
                Log.d(TAG, "Media app: " + packageName + " - Playback state: " + controller.getPlaybackState().getState());
                // Gửi broadcast cho service
                Intent broadcastIntent = new Intent("com.example.soundapp.ACTION_MEDIA_NOTIFICATION");
                broadcastIntent.putExtra("packageName", packageName);
                sendBroadcast(broadcastIntent);
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Missing permission to control media. User must enable Notification Access.", e);
        }
    }

    /**
     * Kiểm tra user đã bật Notification Access chưa
     */
    private boolean isNotificationListenerEnabled(Context context) {
        String enabledListeners = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
        );
        return enabledListeners != null && enabledListeners.contains(context.getPackageName());
    }
}
