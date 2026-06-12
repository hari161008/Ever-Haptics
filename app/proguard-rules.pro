-keep class com.hapticks.app.HapticksApp { *; }
-keep class com.hapticks.app.service.HapticsAccessibilityService { *; }
-keep class com.hapticks.app.** { *; }
-keep class androidx.compose.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class androidx.datastore.** { *; }
-keep class com.google.protobuf.** { *; }
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn androidx.**
-keep class androidx.** { *; }