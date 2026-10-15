# Linda Binti â€” release R8 / ProGuard rules
# Keep line numbers for crash reports without exposing source paths.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# Room entities / DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# EncryptedSharedPreferences / Tink
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Play Services Location
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# Compose / Navigation (safe defaults)
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# App entry points used by reflection / manifest
-keep class com.example.lindabinti.LindaBintiApp { *; }
-keep class com.example.lindabinti.MainActivity { *; }
-keep class com.example.lindabinti.sos.SosForegroundService { *; }
-keep class com.example.lindabinti.sos.SmsStatusReceiver { *; }

# Keep Parcelable / Serializable if any
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Retrofit / OkHttp / Gson (backend Phase 1)
-keepattributes Signature
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.example.lindabinti.data.remote.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**