package com.example.devicemonitoring;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private TextView tvLocation;
    private Handler handler = new Handler();
    private Runnable locationRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLocation = findViewById(R.id.tvLocation);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

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
                sendLocation();
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(locationRunnable);
    }

    private void sendLocation() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                tvLocation.setText("Latitude: " + latitude + "\nLongitude: " + longitude);

                sendLocationToServer(latitude, longitude);
            } else {
                Toast.makeText(this, "Không thể lấy vị trí! Kiểm tra cài đặt vị trí trên thiết bị.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            // Xử lý lỗi khi không thể lấy vị trí
            Toast.makeText(this, "Lỗi khi lấy vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("LocationError", "Lỗi: " + e.getMessage());
        });
    }

    private void sendLocationToServer(double latitude, double longitude) {
        OkHttpClient client = new OkHttpClient();
        String json = "{\"latitude\": " + latitude + ", \"longitude\": " + longitude + "}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url("http://10.15.199.228:5000/location")
                .post(body)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    Log.d("SendLocation", "Gửi thành công!");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Gửi tọa độ thành công!", Toast.LENGTH_SHORT).show());
                } else {
                    Log.e("SendLocation", "Gửi thất bại: " + response.message());
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Gửi tọa độ thất bại: " + response.message(), Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("SendLocation", "Lỗi: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi gửi tọa độ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Bắt đầu gửi vị trí mỗi 2 giây khi có quyền
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Quyền vị trí bị từ chối!", Toast.LENGTH_SHORT).show();
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
}
