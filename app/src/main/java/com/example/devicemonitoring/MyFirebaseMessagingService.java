//package com.example.devicemonitoring;
//
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Build;
//import androidx.core.app.NotificationCompat;
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.RemoteMessage;
//
//public class MyFirebaseMessagingService extends FirebaseMessagingService {
//
//    @Override
//    public void onMessageReceived(RemoteMessage remoteMessage) {
//        if (remoteMessage.getData().containsKey("action") && "CAPTURE_IMAGE".equals(remoteMessage.getData().get("action"))) {
//            showNotification("Yêu cầu chụp ảnh", "Nhấn để chụp ảnh và gửi lên server");
//        }
//    }
//
//    private void showNotification(String title, String message) {
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Tạo kênh thông báo (cho Android 8.0 trở lên)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel("default", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // Tạo Intent để mở MainActivity khi nhấn vào thông báo
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        // Tạo thông báo
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
//                .setSmallIcon(R.drawable.ic_location)
//                .setContentTitle(title)
//                .setContentText(message)
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true);
//
//        // Hiển thị thông báo
//        notificationManager.notify(1, builder.build());
//    }
//}