package com.example.devicemonitoring;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class SearchHandler {

    private final Context context;
    private final String serverUrl;

    // Constructor
    public SearchHandler(Context context, String serverUrl) {
        this.context = context;
        this.serverUrl = serverUrl;
    }


    private void sendDataToServer(String link, long timestamp) {
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + "/submit-search");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("link", link);
                json.put("timestamp", timestamp);

                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(json.toString());
                writer.flush();
                writer.close();

                int responseCode = connection.getResponseCode();
                System.out.println("Response code: " + responseCode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private void openGoogleSearch(String link) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + link));
        context.startActivity(intent);
    }

    public void handleSearch(String link) {
        long timestamp = new Date().getTime(); // Lấy thời gian hiện tại
        sendDataToServer(link, timestamp);    // Gửi dữ liệu tới server
        openGoogleSearch(link);               // Mở Google tìm kiếm
    }
}
