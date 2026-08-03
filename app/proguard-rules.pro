-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.doapp.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.doapp.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
