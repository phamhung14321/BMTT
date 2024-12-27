package com.example.devicemonitoring;

import static android.companion.CompanionDeviceManager.RESULT_CANCELED;
import static android.companion.CompanionDeviceManager.RESULT_OK;
import static android.graphics.PixelFormat.RGBA_8888;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ScreenshotService extends Service {
    private static final String TAG = "ScreenshotService";
    private Handler handler = new Handler();
    private Runnable screenshotRunnable;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;

    private MediaProjectionManager mediaProjectionManager;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);
        Intent data = intent.getParcelableExtra("data");

        if (resultCode == RESULT_OK && data != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            startScreenshot();

            // Chụp màn hình mỗi 10 giây
            screenshotRunnable = new Runnable() {
                @Override
                public void run() {
                    captureScreenshot();
                    handler.postDelayed(this, 10000);  // 10 giây
                }
            };
            handler.post(screenshotRunnable);
        } else {
            stopSelf();
        }

        return START_STICKY;
    }

    private void startScreenshot() {
        // Khởi tạo ImageReader cho việc chụp màn hình
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = getResources().getDisplayMetrics().densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        Surface surface = imageReader.getSurface();
        mediaProjection.createVirtualDisplay(
                "Screenshot",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
        );

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                image.close();

                // Lưu ảnh vào file
                File screenshotFile = new File(getExternalFilesDir(null), "screenshot.png");
                try (FileOutputStream fileOutputStream = new FileOutputStream(screenshotFile)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    Log.d(TAG, "Screenshot saved to " + screenshotFile.getAbsolutePath());

                    // Gửi ảnh lên server
                    sendImageToServer(screenshotFile);
                } catch (IOException e) {
                    Log.e(TAG, "Error saving screenshot: " + e.getMessage());
                }
            }
        }, new Handler(Looper.getMainLooper()));
    }





    private void captureScreenshot() {
        if (mediaProjection != null) {
            startScreenshot();
        }
    }

    private void sendImageToServer(File file) {
        Log.d(TAG, "Attempting to send image to server...");
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(), RequestBody.create(file, MediaType.parse("image/png")))
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.228.80:5000/upload")  // URL của server
                .post(requestBody)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to upload image. Response code: " + response.code());
                    throw new IOException("Unexpected code " + response);
                }
                Log.d(TAG, "Image uploaded successfully!");
            } catch (IOException e) {
                Log.e(TAG, "Error uploading image: " + e.getMessage());
                e.printStackTrace();  // In ra chi tiết lỗi
            }
        }).start();  // Đảm bảo yêu cầu mạng được thực hiện trên background thread
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        handler.removeCallbacks(screenshotRunnable);
    }
}
