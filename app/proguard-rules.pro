# Add project specific ProGuard rules here.

# Keep Kotlinx Serialization classes
-keepattributes *Annotation*, InnerClasses
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

# Keep Room entities
-keep class com.minipai.article.core.database.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
