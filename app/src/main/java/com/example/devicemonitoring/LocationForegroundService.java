package com.example.devicemonitoring;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class LocationForegroundService extends Service {

    private LocationSender locationSender;

    private String SERVER_URL = Constants.SERVER_URL;
    @Override
    public void onCreate() {
        super.onCreate();
        locationSender = new LocationSender(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, getNotification());  // Dùng ID và notification hợp lệ
        }
        // Tiếp tục với các logic khác
        return START_STICKY;
    }

    // Đảm bảo rằng bạn có quyền và thiết lập notification đúng
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getNotification() {
        String channelId = "location_channel";
        NotificationChannel channel = new NotificationChannel(channelId, "Location Service", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Ứng dụng giám sát")
                .setContentText("Đang gửi vị trí...")
                .setSmallIcon(R.drawable.ic_location) // Thêm biểu tượng hợp lệ
                .build();
    }

    private void scheduleLocationUpdates() {
        new Thread(() -> {
            while (true) {
                try {
                    locationSender.getLocationAndSend();
                    Thread.sleep(Constants.TIMEOUT_DURATION); // Gửi vị trí mỗi 5 giây
                } catch (InterruptedException e) {
                    Log.e("ServiceError", "Lỗi khi chờ: " + e.getMessage());
                }
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
