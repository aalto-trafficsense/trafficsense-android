# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Dev/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
# Katso tämäkin:
# https://developer.android.com/studio/build/shrink-code.html

# Add any project specific keep options here:

-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**

-keep class android.net.http.** { *; }
-dontwarn android.net.http.**

-keep class retrofit.** { *; }
-dontwarn retrofit.**

-keep class com.google.common.** { *; }
-dontwarn com.google.common.**

-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**

-keep class android.support.v7.** { *; }
-keep class android.support.v4.** { *; }

-keep class fi.aalto.trafficsense.** { *; }

-dontobfuscate

# These were tried for proguard-android-optimize, but they failed
#-keep class android.support.design.widget.** { *; }
#-keep class com.android.dx.cf.code.** { *; }
#-keep class com.android.build.api.transform.** { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
