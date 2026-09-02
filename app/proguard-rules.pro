# CleanMind - Android R8 / ProGuard Configuration

# Line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# WorkManager
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Domain & Data Models (preserve entity fields)
-keepclassmembers class com.aistudio.cleanmind.app.data.local.entity.** { *; }
-keepclassmembers class com.aistudio.cleanmind.app.domain.model.** { *; }

# Google ML Kit Text Recognition
-keep class com.google.mlkit.vision.text.** { *; }
-dontwarn com.google.mlkit.**
