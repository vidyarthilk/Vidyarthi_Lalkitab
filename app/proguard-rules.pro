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

# Preserve line numbers for Crashlytics / Play Console stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Security/Hardening baseline ---
# Keep runtime-visible annotations used by AndroidX/Room/Billing internals.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod,Signature

# Keep application and entry activities by name (manifest classes).
-keep class com.vidyarthi.lalkitab.VidyarthiLalkitabApp { *; }
-keep class com.vidyarthi.lalkitab.MainActivity { *; }
-keep class com.vidyarthi.lalkitab.KundliFlowActivity { *; }
-keep class com.vidyarthi.lalkitab.SettingsActivity { *; }
-keep class com.vidyarthi.lalkitab.ui.kundli.VarshfalActivity { *; }
-keep class com.vidyarthi.lalkitab.ui.kundli.LalKitabDashaActivity { *; }
-keep class com.vidyarthi.lalkitab.ui.kundli.LalKitabAntardashaActivity { *; }
-keep class com.vidyarthi.lalkitab.ui.lalkitab.LalKitabMukhyaGrahActivity { *; }

# Keep Room generated implementations.
-keep class * extends androidx.room.RoomDatabase
-keep class **_Impl { *; }

# AdMob (play-services-ads) may reference newer platform APIs not present on minSdk.
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener