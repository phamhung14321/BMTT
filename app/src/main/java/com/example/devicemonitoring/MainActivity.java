package com.example.devicemonitoring;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private TextView tvLocation;
    private Handler handler = new Handler();
    private Runnable locationRunnable;
    private LocationSender locationSender;
    private boolean canShowToast = true;  // Biến để kiểm tra xem có thể hiển thị Toast hay không

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);
        locationSender = new LocationSender(this);

        Button btnSendLocation = findViewById(R.id.btnSendLocation);
        btnSendLocation.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                startLocationUpdates();
            }
        });
    }

    private void startLocationUpdates() {
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                locationSender.getLocationAndSend();  // Gọi phương thức từ LocationSender
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(locationRunnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Bắt đầu gửi vị trí mỗi 2 giây khi có quyền
                startLocationUpdates();
            } else {
                showToast("Quyền vị trí bị từ chối!");  // Gọi phương thức showToast thay vì Toast trực tiếp
            }
        }
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
