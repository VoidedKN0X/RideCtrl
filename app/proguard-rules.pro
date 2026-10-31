# kotlinx.serialization models (library ships its own rules; these keep saved logs and routes intact).
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep,includedescriptorclasses class com.vknxworks.ridectrl.**$$serializer { *; }
-keepclassmembers class com.vknxworks.ridectrl.** {
    *** Companion;
}
-keepclasseswithmembers class com.vknxworks.ridectrl.** {
    kotlinx.serialization.KSerializer serializer(...);
}
