# VisionTrack ProGuard Rules

# Keep TensorFlow Lite classes
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.**

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep data models for Gson/Firestore
-keep class com.visiontrack.data.** { *; }
-keep class com.visiontrack.domain.model.** { *; }

# Keep Hilt-generated classes
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Keep Room entities
-keep @androidx.room.Entity class * { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*, Signature, Exception
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Remove logging in release
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
}
