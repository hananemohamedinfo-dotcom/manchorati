# Proguard & R8 Optimization Rules for Manchorati
-keep class com.manchorati.www.** { *; }
-keepclassmembers class com.manchorati.www.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Firebase Firestore & Auth models
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes *Annotation*

# Google Play Services & Google Sign In
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Coil & Glide Image Loading
-keep class io.coilkt.** { *; }
-keep class com.bumptech.glide.** { *; }

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# AmbilWarna Color Picker
-keep class yuku.ambilwarna.** { *; }
