# Add project specific ProGuard rules here.

# Keep Kotlinx Serialization classes
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.minipai.article.**$$serializer { *; }
-keepclassmembers class com.minipai.article.** {
    *** Companion;
}
-keepclasseswithmembers class com.minipai.article.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities + DAOs
-keep class com.minipai.article.core.database.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Coil
-dontwarn coil.**

# Compose
-keepclassmembers class androidx.compose.runtime.** { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**

