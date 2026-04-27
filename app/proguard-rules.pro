-keep class com.hapticks.app.HapticksApp { *; }
-keep class com.hapticks.app.service.HapticsAccessibilityService { *; }
-keep class com.hapticks.app.xposed.EdgeEffectHapticsModule { *; }
-keep class com.hapticks.app.** { *; }
-keep class io.github.libxposed.** { *; }
-keep interface io.github.libxposed.** { *; }
-keep class de.robv.android.xposed.** { *; }
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage { *; }
-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit { *; }
-keep class * extends de.robv.android.xposed.XposedMod { *; }
-keepclassmembers class * {
    void handleLoadPackage(de.robv.android.xposed.XC_LoadPackage$LoadPackageParam);
    void initZygote(de.robv.android.xposed.IXposedHookZygoteInit$StartupParam);
}
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