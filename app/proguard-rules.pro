# Proguard rules for MP3 Player Pro
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
