package com.example.devicemonitoring;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraService extends Service {
    private static final String TAG = "CameraService";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String SERVER_URL = "http://192.168.2.16:5000/upload"; // Thay thế bằng URL của server

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Nhận yêu cầu chụp ảnh
        if (intent != null && "CAPTURE_IMAGE".equals(intent.getAction())) {
            openCamera();
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Mở camera để chụp ảnh
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(takePictureIntent);
        } else {
            Log.e(TAG, "Không thể mở camera");
        }
    }

    // Xử lý kết quả chụp ảnh
    public void handleCaptureResult(Intent data) {
        if (data != null && data.getExtras() != null) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            if (imageBitmap != null) {
                // Lưu ảnh vào file tạm
                File imageFile = saveBitmapToFile(imageBitmap);
                if (imageFile != null) {
                    // Gửi ảnh lên server
                    uploadImageToServer(imageFile);
                }
            }
        }
    }

    // Lưu ảnh vào file tạm
    private File saveBitmapToFile(Bitmap bitmap) {
        File file = new File(getCacheDir(), "captured_image.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            return file;
        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi lưu ảnh vào file", e);
            return null;
        }
    }

    // Gửi ảnh lên server
    private void uploadImageToServer(File file) {
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(), RequestBody.create(file, MediaType.parse("image/png")))
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Lỗi khi gửi ảnh lên server", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Ảnh đã được gửi lên server thành công");
                } else {
                    Log.e(TAG, "Lỗi khi gửi ảnh lên server: " + response.message());
                }
            }
        });
    }
}