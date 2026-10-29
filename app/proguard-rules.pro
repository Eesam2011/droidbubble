# Shizuku: ShizukuHelper calls the private Shizuku.newProcess(...) via reflection
-keep class rikka.shizuku.Shizuku { *; }
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Keep line numbers so release crash stack traces stay readable
-keepattributes SourceFile,LineNumberTable
