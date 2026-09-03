# XTC AI Assistant proguard rules
-keepattributes Signature
-keepattributes *Annotation*

# Keep the app's own classes (bridge + services)
-keep class com.xtcai.assistant.** { *; }

# Keep WebView JS interface annotation
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**
