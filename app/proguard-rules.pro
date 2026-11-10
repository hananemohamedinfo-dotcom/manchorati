# ─────────────────────────────────────────────────────────────
# Proguard & R8 Optimization Rules for Manchorati
# Maximum Shrinking, Obfuscation, and Class Repackaging
# ─────────────────────────────────────────────────────────────

# Enable class repackaging to optimize DEX layout and obfuscation
-repackageclasses 'com.manchorati.www.r'
-allowaccessmodification

# Keep line numbers and file info for clean stack traces
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─────────────────────────────────────────────────────────────
# 1. Annotated Models & Reflection
# ─────────────────────────────────────────────────────────────
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Firebase Firestore Serialization
-keep @com.google.firebase.firestore.IgnoreExtraProperties class * { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
}
-keepclassmembers class * {
    @com.google.firebase.firestore.Exclude *;
}
-keepclassmembers class * {
    @com.google.firebase.firestore.ServerTimestamp *;
}

# Explicitly keep all Firestore Data Models with properties and constructors
-keep class com.manchorati.www.CommunityPost { *; }
-keep class com.manchorati.www.Post { *; }
-keep class com.manchorati.www.FirestorePost { *; }
-keep class com.manchorati.www.Comment { *; }
-keep class com.manchorati.www.AppNotification { *; }
-keep class com.manchorati.www.Category { *; }
-keep class com.manchorati.www.WriterItem { *; }
-keep class com.manchorati.www.UserBadge { *; }
-keep class com.manchorati.www.VideoTheme { *; }
-keep class com.manchorati.www.AiChatMessage { *; }
-keep class com.manchorati.www.AiChatMessage$* { *; }

# ─────────────────────────────────────────────────────────────
# 2. Custom Views instantiated from XML
# ─────────────────────────────────────────────────────────────
-keep public class com.manchorati.www.VideoMotionView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep public class com.manchorati.www.AudioTimelineTrimmerView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep public class com.manchorati.www.TimelineFilmstripView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ─────────────────────────────────────────────────────────────
# 3. AndroidX WorkManager Workers
# ─────────────────────────────────────────────────────────────
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─────────────────────────────────────────────────────────────
# 4. Third-Party Libraries
# ─────────────────────────────────────────────────────────────
# AmbilWarna Color Picker
-keep class yuku.ambilwarna.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
