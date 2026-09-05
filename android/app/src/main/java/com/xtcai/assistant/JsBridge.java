package com.xtcai.assistant;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JsBridge {
    private static final String TAG = "JsBridge";
    private final Activity activity;
    private final WebView webView;
    private final PreferenceManager prefManager;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    public JsBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.prefManager = new PreferenceManager(activity);
    }

    @JavascriptInterface
    public String getSettings() {
        return prefManager.getSettingsJson();
    }

    @JavascriptInterface
    public void saveSettings(String json) {
        prefManager.saveSettingsJson(json);
        runOnUiThread(() ->
                Toast.makeText(activity, "设置已保存", Toast.LENGTH_SHORT).show());
    }

    @JavascriptInterface
    public String getApiConfig(int index) {
        String settings = prefManager.getSettingsJson();
        try {
            JSONObject root = new JSONObject(settings);
            JSONObject api = root.optJSONObject(index == 0 ? "primaryApi" : "backupApi");
            if (api != null) {
                JSONObject config = new JSONObject();
                config.put("baseUrl", api.optString("baseUrl"));
                config.put("apiKey", api.optString("apiKey"));
                config.put("model", api.optString("model"));
                return config.toString();
            }
        } catch (JSONException e) {
            Log.e(TAG, "getApiConfig error", e);
        }
        return "{}";
    }

    @JavascriptInterface
    public String sendMessageToXtc(String message) {
        try {
            return XtcAccessibilityService.sendToForeground(message);
        } catch (Exception e) {
            Log.e(TAG, "sendMessageToXtc error", e);
            return "error: " + e.getMessage();
        }
    }

    @JavascriptInterface
    public boolean isAccessibilityEnabled() {
        if (activity instanceof MainActivity) {
            return ((MainActivity) activity).isAccessibilityServiceEnabled();
        }
        return false;
    }

    @JavascriptInterface
    public void openAccessibilitySettings() {
        runOnUiThread(() -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).openAccessibilitySettings();
            }
        });
    }

    @JavascriptInterface
    public void openNotificationSettings() {
        runOnUiThread(() -> {
            if (activity instanceof MainActivity) {
                ((MainActivity) activity).openNotificationListenerSettings();
            }
        });
    }

    @JavascriptInterface
    public void openXtcApp() {
        runOnUiThread(() -> {
            try {
                // Primary check: launcher intent
                Intent launchIntent = activity.getPackageManager()
                        .getLaunchIntentForPackage("com.xtc.watch");
                if (launchIntent != null) {
                    activity.startActivity(launchIntent);
                    return;
                }
                
                // Fallback check: installed check using improved method
                if (isAppInstalled("com.xtc.watch")) {
                    activity.startActivity(new Intent(activity, com.xtcai.assistant.MainActivity.class));
                    return;
                }
                
                Toast.makeText(activity,
                        "未安装小天利用家长端App", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(activity,
                        "打开小天利用App失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @JavascriptInterface
    public void toast(String message) {
        runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
    }

    @JavascriptInterface
    public String getDeviceInfo() {
        JSONObject info = new JSONObject();
        try {
            info.put("brand", Build.BRAND);
            info.put("model", Build.MODEL);
            info.put("sdk", Build.VERSION.SDK_INT);
            info.put("appVersion", getAppVersionName());
            info.put("xtcInstalled", isAppInstalled("com.xtc.watch"));
            info.put("accessibilityEnabled", isAccessibilityEnabled());
        } catch (JSONException e) {
            Log.e(TAG, "getDeviceInfo error", e);
        }
        return info.toString();
    }

    @JavascriptInterface
    public void setLastReply(String message) {
        XtcAccessibilityService.setLastWatchReply(message);
    }

    @JavascriptInterface
    public String getLastReply() {
        String reply = XtcAccessibilityService.getLastWatchReply();
        return reply != null ? reply : "";
    }

    private boolean isAppInstalled(String packageName) {
        try {
            // Primary check: package exists
            activity.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e1) {
            // Fallback check: check if launcher intent exists
            try {
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(packageName);
                return intent != null;
            } catch (Exception e2) {
                // Last fallback: check running services
                try {
                    android.app.ActivityManager activityManager = (android.app.ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
                    List<android.app.RunningAppProcessInfo> running Processes = activityManager.getRunningAppProcesses();
                    if (running Processes != null) {
                        for (android.app.RunningAppProcessInfo process : running Processes) {
                            if (process.processName.equals(packageName)) {
                                return true;
                            }
                        }
                    }
                } catch (Exception e3) {
                    // Ignore
                }
            }
        }
        return false;
    }

    private String getAppVersionName() {
        try {
            PackageInfo info = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    private void runOnUiThread(Runnable action) {
        activity.runOnUiThread(action);
    }
}
