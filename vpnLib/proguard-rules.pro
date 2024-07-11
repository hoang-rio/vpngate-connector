# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/huangyifei/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
-keep class de.blinkt.openvpn.VpnProfile
-keepclassmembers class de.blinkt.openvpn.VpnProfile { public *;}
#-keep class de.blinkt.openvpn.core.**
#-keepclassmembers class de.blinkt.openvpn.core.** { public *;}
-keep class de.blinkt.openvpn.core.ConfigParser$ConfigParseError
-keep class de.blinkt.openvpn.core.ConfigParser
-keepclassmembers class de.blinkt.openvpn.core.ConfigParser {public *;}
-keep class de.blinkt.openvpn.core.ConnectionStatus
-keepclassmembers class de.blinkt.openvpn.core.ConnectionStatus {public *;}
-keep class de.blinkt.openvpn.core.IOpenVPNServiceInternal$Stub
-keepclassmembers class de.blinkt.openvpn.core.IOpenVPNServiceInternal$Stub {public *;}
-keep class de.blinkt.openvpn.core.IOpenVPNServiceInternal
-keepclassmembers class de.blinkt.openvpn.core.IOpenVPNServiceInternal {public *;}
-keep class de.blinkt.openvpn.core.OpenVPNService
-keepclassmembers class de.blinkt.openvpn.core.OpenVPNService {public *;}
-keep class de.blinkt.openvpn.core.ProfileManager
-keepclassmembers class de.blinkt.openvpn.core.ProfileManager {public *;}
-keep class de.blinkt.openvpn.core.VPNLaunchHelper
-keepclassmembers class de.blinkt.openvpn.core.VPNLaunchHelper {public *;}
-keep class de.blinkt.openvpn.core.VpnStatus$ByteCountListener
-keep class de.blinkt.openvpn.core.VpnStatus$StateListener
-keep class de.blinkt.openvpn.core.VpnStatus
-keepclassmembers class de.blinkt.openvpn.core.VpnStatus {public *;}
-keep class de.blinkt.openvpn.utils.**
-keepclassmembers class de.blinkt.openvpn.utils.** { public *;}