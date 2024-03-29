# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
# SearchView
-keep class androidx.appcompat.widget.SearchView { *; }
# Static final of Detail activity
-keep public class vn.unlimit.vpngate.activities.DetailActivity {
    public static final *;
}
-keep public class vn.unlimit.vpngate.activities.paid.ServerActivity {
    public static final *;
}
# VPNGate Connection List enum
-keepclassmembers enum vn.unlimit.vpngate.models.* {*;}
# PaidServer Model
-keep class vn.unlimit.vpngate.models.PaidServer {*;}
-keep class vn.unlimit.vpngate.models.PurchaseHistory {*;}
-keep class vn.unlimit.vpngate.models.ConnectedSession {*;}
-keep class vn.unlimit.vpngate.models.ConnectedSession$* {*;}
-keep class vn.unlimit.vpngate.models.DeviceInfo {*;}
-keep class vn.unlimit.vpngate.models.DeviceInfo$NotificationSetting {*;}
# for DexGuard only
# -keepresourcexmlelements manifest/application/meta-data@value=GlideModule
# Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
# Fix R8 build issue
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# Fix gson TypeToken crash
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type
