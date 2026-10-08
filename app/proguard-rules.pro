# Naughty ProGuard / R8 Rules

# Room database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Markwon markdown renderer
-keep class io.noties.markwon.** { *; }

# Google ML Kit offline text recognition
-keep class com.google.mlkit.** { *; }

# AndroidX Biometric & Device Credentials
-keep class androidx.biometric.** { *; }

# Audx RNNoise native audio denoising
-keep class com.audx.android.** { *; }
-keepclasseswithmembernames class com.audx.android.** {
    native <methods>;
}
