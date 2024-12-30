package com.example.devicemonitoring;

import static android.content.ContentValues.TAG;

import static com.google.android.material.color.utilities.MaterialDynamicColors.surface;

import android.annotation.SuppressLint;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 102;

    private TextView tvLocation;
    private Handler handler = new Handler();
    private Runnable locationRunnable;
    private LocationSender locationSender;
    private boolean canShowToast = true;

    private WebView webView;
    private Button sendButton;
    private String searchUrl;
    private long searchTime;
    private static final String SERVER_URL = "http://192.168.2.16:5000";
    private boolean isAcceptButtonClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);
        locationSender = new LocationSender(this);

        requestLocationPermissions();

        // Khởi chạy dịch vụ vị trí khi đã có quyền
        Intent serviceIntent = new Intent(this, LocationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);  // Đảm bảo gọi startForegroundService()
        }

        Button btnSendLocation = findViewById(R.id.btnSendLocation);
        btnSendLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                startLocationUpdates();
            }
        });

        Button btnCaptureAndSend = findViewById(R.id.btnCaptureAndSend);
        btnCaptureAndSend.setOnClickListener(v -> startScreenCapture());


        SearchHandler searchHandler = new SearchHandler(this, SERVER_URL);

        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            String link = "https://example.com"; // Thay bằng link của bạn
            searchHandler.handleSearch(link);   // Gọi chức năng tìm kiếm và gửi dữ liệu
            finish();                           // Thoát ứng dụng
        });
    }

    private void startScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager mediaProjectionManager =
                    (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            if (mediaProjectionManager != null) {
                Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
            } else {
                Log.e(TAG, "MediaProjectionManager is null");
                Toast.makeText(this, "Screen capture is not supported on this device.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Your Android version does not support screen capture.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(this, ScreenshotService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                startForegroundService(serviceIntent);
            } else {
                Toast.makeText(this, "Screen capture permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void requestLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.FOREGROUND_SERVICE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            // Nếu thiết bị chạy Android dưới API 29, yêu cầu quyền vị trí cơ bản
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void startLocationUpdates() {
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                locationSender.getLocationAndSend();  // Gọi phương thức từ LocationSender
                handler.postDelayed(this, 2000);  // Gọi lại sau 2 giây
            }
        };
        handler.post(locationRunnable);
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && locationRunnable != null) {
            handler.removeCallbacks(locationRunnable);
        }
    }

    // Phương thức showToast để giới hạn số lần Toast được hiển thị
    private void showToast(String message) {
        if (canShowToast) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            canShowToast = false;

            // Cho phép lại Toast sau 2 giây
            new Handler().postDelayed(() -> canShowToast = true, 2000);
        }
    }
}
