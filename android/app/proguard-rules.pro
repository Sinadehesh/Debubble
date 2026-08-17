# kotlinx.serialization keeps its generated serializers on the companion of each
# @Serializable class; R8 needs to be told not to strip them.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.debubble.app.**$$serializer { *; }
-keepclassmembers class com.debubble.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.debubble.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
