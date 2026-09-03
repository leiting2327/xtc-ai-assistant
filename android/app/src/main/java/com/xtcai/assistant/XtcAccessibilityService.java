package com.xtcai.assistant;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XtcAccessibilityService extends AccessibilityService {
    private static final String TAG = "XtcAccessibility";
    private static final String XTC_PACKAGE = "com.xtc.watch";
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static volatile String lastWatchReply = null;
    private static volatile boolean messageSendInProgress = false;
    private static volatile Runnable onMessageSentCallback = null;

    public static void setLastWatchReply(String reply) {
        lastWatchReply = reply;
    }

    public static String getLastWatchReply() {
        String reply = lastWatchReply;
        lastWatchReply = null;
        return reply;
    }

    public static void setOnMessageSentCallback(Runnable callback) {
        onMessageSentCallback = callback;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        String packageName = event.getPackageName() != null
                ? event.getPackageName().toString() : "";
        if (!XTC_PACKAGE.equals(packageName)) return;

        int eventType = event.getEventType();
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                handleWindowChanged(event);
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                handleTextChanged(event);
                break;
        }
    }

    private void handleWindowChanged(AccessibilityEvent event) {
        if (!messageSendInProgress) return;
        logNodeInfo("WindowChanged", getRootInActiveWindow());
    }

    private void handleTextChanged(AccessibilityEvent event) {
        if (!messageSendInProgress) return;
        StringBuilder textBuilder = new StringBuilder();
        for (CharSequence text : event.getText()) {
            textBuilder.append(text);
        }
        String text = textBuilder.toString();
        if (text.isEmpty()) return;
        Log.d(TAG, "TextChanged: " + text);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "XtcAccessibilityService connected");

        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }
    }

    private void logNodeInfo(String tag, AccessibilityNodeInfo root) {
        if (root == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            appendNodeTree(sb, root, 0);
            Log.d(TAG, tag + " tree:\n" + sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "logNodeInfo error", e);
        }
    }

    private void appendNodeTree(StringBuilder sb, AccessibilityNodeInfo node, int depth) {
        if (node == null) return;
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append("<")
          .append(node.getClassName() != null ? node.getClassName() : "null")
          .append(" id=").append(node.getViewIdResourceName() != null
                  ? node.getViewIdResourceName() : "none")
          .append(" text=")
          .append(node.getText() != null ? node.getText().toString().substring(0,
                  Math.min(node.getText().length(), 50)) : "null")
          .append(" desc=")
          .append(node.getContentDescription() != null
                  ? node.getContentDescription().toString().substring(0,
                          Math.min(node.getContentDescription().length(), 50))
                  : "null")
          .append(" clickable=").append(node.isClickable())
          .append(" edit=").append(node.isEditable())
          .append(">\n");

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                appendNodeTree(sb, child, depth + 1);
                child.recycle();
            }
        }
    }

    public static class EmojiMessageSender {
        private static final int SCREEN_WIDTH = 1080;
        private static final int SCREEN_HEIGHT = 1200;
        private static final int SCREEN_DPI = 320;
        private int[] screenSize = null;

        public String sendEmojiMessage(AccessibilityService service, String message) {
            return sendEmojiMessage(service, message, null);
        }

        public String sendEmojiMessage(AccessibilityService service, String message,
                                        Runnable callback) {
            if (service == null) {
                return "error: AccessibilityService not available. Please enable it in Settings.";
            }

            if (messageSendInProgress) {
                return "error: Message send already in progress";
            }

            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root == null) {
                return "error: Cannot get window root. Is XTC app in foreground?";
            }

            try {
                messageSendInProgress = true;
                onMessageSentCallback = callback;
                return performEmojiSend(service, root, message);
            } catch (Exception e) {
                Log.e(TAG, "sendEmojiMessage error", e);
                return "error: " + e.getMessage();
            } finally {
                mainHandler.postDelayed(() -> {
                    messageSendInProgress = false;
                }, 5000);
            }
        }

        private String performEmojiSend(AccessibilityService service,
                                         AccessibilityNodeInfo root,
                                         String message) {
            try {
                AccessibilityNodeInfo emojiBtn = findNodeById(root,
                        XTC_PACKAGE + ":id/smile_btn");
                if (emojiBtn != null) {
                    performAction(service, emojiBtn);
                    service.performGlobalAction(
                            AccessibilityService.GLOBAL_ACTION_BACK);
                    emojiBtn.recycle();
                    sleep(500);
                }
            } catch (Exception e) {
                Log.w(TAG, "Emoji button approach failed", e);
            }

            AccessibilityNodeInfo editBox = findEditableNode(root);
            if (editBox == null) {
                editBox = findNodeById(root,
                        XTC_PACKAGE + ":id/input_et");
            }
            if (editBox == null) {
                editBox = findNodeByClass(root,
                        "android.widget.EditText");
            }

            if (editBox == null) {
                try {
                    Log.d(TAG, "Trying focused node approach");
                    AccessibilityNodeInfo focused = service.findFocus(
                            AccessibilityNodeInfo.FOCUS_INPUT);
                    if (focused != null && focused.isEditable()) {
                        editBox = focused;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Focus approach failed", e);
                }
            }

            if (editBox == null) {
                logNodeInfo("NoEditBoxFound", root);
                return "error: Cannot find input box in XTC app. " +
                        "Please make sure you are in the chat screen.";
            }

            try {
                Bundle args = new Bundle();
                args.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        message);
                boolean result = editBox.performAction(
                        AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                editBox.recycle();

                if (!result) {
                    return "error: Failed to set text in input box";
                }

                sleep(300);

                AccessibilityNodeInfo sendBtn = findNodeById(root,
                        XTC_PACKAGE + ":id/send_btn");
                if (sendBtn == null) {
                    sendBtn = findNodeById(root,
                            XTC_PACKAGE + ":id/send_img");
                }
                if (sendBtn == null) {
                    sendBtn = findNodeById(root,
                            XTC_PACKAGE + ":id/btn_send");
                }
                if (sendBtn == null) {
                    sendBtn = findNodeByDesc(root, "发送");
                }
                if (sendBtn == null) {
                    sendBtn = findNodeByText(root, "发送");
                }

                if (sendBtn != null) {
                    performAction(service, sendBtn);
                    sendBtn.recycle();
                } else {
                    Log.w(TAG, "Send button not found, trying Enter key");
                }

                sleep(1000);
                logNodeInfo("AfterSend", service.getRootInActiveWindow());

                return "success";

            } catch (Exception e) {
                Log.e(TAG, "performEmojiSend error", e);
                return "error: " + e.getMessage();
            }
        }

        private AccessibilityNodeInfo findNodeById(AccessibilityNodeInfo root, String id) {
            if (root == null) return null;
            try {
                List<AccessibilityNodeInfo> nodes =
                        root.findAccessibilityNodeInfosByViewId(id);
                if (nodes != null && !nodes.isEmpty()) {
                    return nodes.get(0);
                }
            } catch (Exception e) {
                Log.w(TAG, "findNodeById error: " + id, e);
            }
            return null;
        }

        private AccessibilityNodeInfo findNodeByClass(AccessibilityNodeInfo root,
                                                      String className) {
            if (root == null) return null;
            if (className.equals(root.getClassName().toString())) {
                return AccessibilityNodeInfo.obtain(root);
            }
            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo child = root.getChild(i);
                if (child != null) {
                    AccessibilityNodeInfo found = findNodeByClass(child, className);
                    child.recycle();
                    if (found != null) return found;
                }
            }
            return null;
        }

        private AccessibilityNodeInfo findNodeByDesc(AccessibilityNodeInfo root,
                                                     String desc) {
            if (root == null) return null;
            if (root.getContentDescription() != null &&
                    root.getContentDescription().toString().contains(desc)) {
                return AccessibilityNodeInfo.obtain(root);
            }
            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo child = root.getChild(i);
                if (child != null) {
                    AccessibilityNodeInfo found = findNodeByDesc(child, desc);
                    child.recycle();
                    if (found != null) return found;
                }
            }
            return null;
        }

        private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo root,
                                                     String text) {
            if (root == null) return null;
            if (root.getText() != null &&
                    root.getText().toString().contains(text)) {
                return AccessibilityNodeInfo.obtain(root);
            }
            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo child = root.getChild(i);
                if (child != null) {
                    AccessibilityNodeInfo found = findNodeByText(child, text);
                    child.recycle();
                    if (found != null) return found;
                }
            }
            return null;
        }

        private AccessibilityNodeInfo findEditableNode(AccessibilityNodeInfo root) {
            if (root == null) return null;
            if (root.isEditable()) {
                return AccessibilityNodeInfo.obtain(root);
            }
            for (int i = 0; i < root.getChildCount(); i++) {
                AccessibilityNodeInfo child = root.getChild(i);
                if (child != null) {
                    AccessibilityNodeInfo found = findEditableNode(child);
                    child.recycle();
                    if (found != null) return found;
                }
            }
            return null;
        }

        private void performAction(AccessibilityService service,
                                    AccessibilityNodeInfo node) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } else {
                AccessibilityNodeInfo parent = node.getParent();
                if (parent != null && parent.isClickable()) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    parent.recycle();
                } else {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }

        private void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
