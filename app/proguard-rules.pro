-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**

# kotlinx.serialization: keep generated serializers of our models
-keepclassmembers class dev.imhamid.gymyar.** { *** Companion; }
-keepclasseswithmembers class dev.imhamid.gymyar.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.imhamid.gymyar.**$$serializer { *; }
