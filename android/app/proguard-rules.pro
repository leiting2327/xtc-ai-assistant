<?xml version="1.0" encoding="utf-8"?>
<proguard-rules>
    -keepattributes Signature
    -keepattributes *Annotation*
    -keep class com.xtcai.assistant.** { *; }
    -keep class android.webkit.JavascriptInterface { *; }
    -keepclassmembers class * {
        @android.webkit.JavascriptInterface <methods>;
    }
    -keep class com.squareup.okhttp3.** { *; }
    -keep class okhttp3.** { *; }
    -keep class okio.** { *; }
</proguard-rules>
