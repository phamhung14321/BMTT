package com.example.devicemonitoring;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocationSender {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Context context;
    public LocationSender(Context context) {
        this.context = context;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void sendLocationToServer(double latitude, double longitude) {
        OkHttpClient client = new OkHttpClient();
        String json = "{\"latitude\": " + latitude + ", \"longitude\": " + longitude + "}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(Constants.SERVER_URL+"/home/location")
                .post(body)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    Log.d("SendLocation", "Gửi thành công!");
                    showToast("Gửi tọa độ thành công!");
                } else {
                    Log.e("SendLocation", "Gửi thất bại: " + response.message());
                    showToast("Gửi tọa độ thất bại: " + response.message());
                }
            } catch (Exception e) {
                Log.e("SendLocation", "Lỗi: " + e.getMessage());
                showToast("Lỗi gửi tọa độ: " + e.getMessage());
            }
        }).start();
    }

    @SuppressLint("MissingPermission")
    public void getLocationAndSend() {
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                sendLocationToServer(latitude, longitude);
            } else {
                showToast("Không thể lấy vị trí! Kiểm tra cài đặt vị trí trên thiết bị.");
            }
        }).addOnFailureListener(e -> {
            showToast("Lỗi khi lấy vị trí: " + e.getMessage());
            Log.e("LocationError", "Lỗi: " + e.getMessage());
        });
    }

    private void showToast(String message) {
        // Kiểm tra xem context có phải là Activity không
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        } else {
            // Nếu không phải Activity (ví dụ là Service), dùng Application context để hiển thị Toast
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

}
