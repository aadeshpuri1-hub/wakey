# Third-party annotation noise
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn afu.org.checkerframework.**
-dontwarn org.jetbrains.annotations.**

# App code is obfuscated. Only the data model is kept as-is (stored as JSON by name).
-keep class app.upwake.data.** { *; }
-keepclassmembers enum app.upwake.** { *; }

# Readable crash reports without exposing original file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Strip debug/verbose/info logging from release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
