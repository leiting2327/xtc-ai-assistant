package com.xtcai.assistant;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class XtcNotificationService extends NotificationListenerService {
    private static final String TAG = "XtcNotification";
    private static final String XTC_PACKAGE = "com.xtc.watch";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getPackageName() == null) return;
        if (!XTC_PACKAGE.equals(sbn.getPackageName())) return;

        try {
            Bundle extras = sbn.getNotification().extras;
            if (extras == null) return;

            CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence bigTextCs = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

            String title = titleCs != null ? titleCs.toString() : "";
            String text = textCs != null ? textCs.toString() : "";
            String bigText = bigTextCs != null ? bigTextCs.toString() : "";

            String message = bigText.isEmpty() ? text : bigText;

            if (!message.isEmpty()) {
                Log.d(TAG, "Watch reply: [" + title + "] " + message);
                XtcAccessibilityService.setLastWatchReply(title + ": " + message);
                sendBroadcastToWeb(title, message);
            }
        } catch (Exception e) {
            Log.e(TAG, "onNotificationPosted error", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    private void sendBroadcastToWeb(String title, String message) {
        Intent intent = new Intent("com.xtcai.assistant.WATCH_REPLY");
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }
}
