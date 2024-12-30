
package com.example.devicemonitoring;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebHistorySender {
    private static final String TAG = "WebHistorySender";
    private Context context;
    private static final String SERVER_URL = "http://192.168.2.16:5000/home/browser-history";
    private static final String[] COMMON_WEBSITES = {
            "https://google.com",
            "https://facebook.com",
            "https://youtube.com",
            "https://gmail.com",
            "https://maps.google.com",
            "https://drive.google.com",
            "https://twitter.com",
            "https://instagram.com",
            "https://linkedin.com",
            "https://github.com"
    };

    private static final String[] COMMON_TITLES = {
            "Google",
            "Facebook - Đăng nhập hoặc đăng ký",
            "YouTube",
            "Gmail",
            "Google Maps",
            "Google Drive",
            "Twitter",
            "Instagram",
            "LinkedIn",
            "GitHub"
    };

    public WebHistorySender(Context context) {
        this.context = context;
    }

    public void collectAndSendBrowserHistory() {
        new Thread(() -> {
            try {
                JSONArray historyArray = generateBrowserHistory();
                if (historyArray.length() > 0) {
                    sendToServer(historyArray);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error collecting browser history: " + e.getMessage());
                showToast("Không thể thu thập lịch sử duyệt web: " + e.getMessage());
            }
        }).start();
    }

    private JSONArray generateBrowserHistory() {
        JSONArray historyArray = new JSONArray();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Random random = new Random();

        // Tạo dữ liệu lịch sử cho 24 giờ gần nhất
        long currentTime = System.currentTimeMillis();
        long oneDayAgo = currentTime - (24 * 60 * 60 * 1000);

        try {
            // Tạo 20-30 mục lịch sử
            int numEntries = 20 + random.nextInt(11);

            for (int i = 0; i < numEntries; i++) {
                JSONObject historyItem = new JSONObject();

                // Chọn ngẫu nhiên một website từ danh sách
                int websiteIndex = random.nextInt(COMMON_WEBSITES.length);

                // Tạo timestamp ngẫu nhiên trong 24 giờ qua
                long randomTime = oneDayAgo + random.nextInt((int) (currentTime - oneDayAgo));

                historyItem.put("url", COMMON_WEBSITES[websiteIndex]);
                historyItem.put("title", COMMON_TITLES[websiteIndex]);
                historyItem.put("timestamp", dateFormat.format(new Date(randomTime)));
                historyItem.put("visit_count", 1 + random.nextInt(10));
                historyItem.put("last_visited", dateFormat.format(new Date(randomTime)));

                historyArray.put(historyItem);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating browser history: " + e.getMessage());
        }

        return historyArray;
    }

//    private void sendToServer(JSONArray historyData) {
//        OkHttpClient client = new OkHttpClient();
//        RequestBody body = RequestBody.create(
//                historyData.toString(),
//                MediaType.parse("application/json")
//        );
//
//        Request request = new Request.Builder()
//                .url(SERVER_URL)
//                .post(body)
//                .build();
//
//        try {
//            Response response = client.newCall(request).execute();
//            if (response.isSuccessful()) {
//                Log.d(TAG, "Browser history sent successfully");
//                showToast("Đã gửi lịch sử duyệt web thành công");
//            } else {
//                Log.e(TAG, "Failed to send browser history: " + response.message());
//                showToast("Gửi lịch sử duyệt web thất bại: " + response.message());
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Error sending browser history: " + e.getMessage());
//            showToast("Lỗi khi gửi lịch sử duyệt web: " + e.getMessage());
//        }
 //   }
private void sendToServer(JSONArray historyData) {
    new Thread(() -> {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                historyData.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                Log.d(TAG, "Browser history sent successfully");
                showToast("Đã gửi lịch sử duyệt web thành công");
            } else {
                String errorMessage = "Gửi lịch sử duyệt web thất bại: " + response.message();
                Log.e(TAG, errorMessage);
                showToast(errorMessage);
            }
        } catch (Exception e) {
            String errorMessage = "Lỗi khi gửi lịch sử duyệt web: " + e.getMessage();
            Log.e(TAG, errorMessage);
            showToast(errorMessage);
        }
    }).start();
}

    private void showToast(String message) {
        android.os.Handler handler = new android.os.Handler(context.getMainLooper());
        handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }
}