# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Firebase Messaging
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class tokyo.isseikuzumaki.atvremote.helper.**$$serializer { *; }
-keepclassmembers class tokyo.isseikuzumaki.atvremote.helper.** {
    *** Companion;
}
-keepclasseswithmembers class tokyo.isseikuzumaki.atvremote.helper.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data models
-keep class tokyo.isseikuzumaki.atvremote.helper.model.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }
