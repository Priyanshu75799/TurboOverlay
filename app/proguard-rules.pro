# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Shizuku UserService & AIDL Interfaces - Keep these names stable in release APKs.
-keep class com.PriyanshuDeveloper.turbooverlay.PrivilegedShellService { <init>(); *; }
-keep class com.PriyanshuDeveloper.turbooverlay.IPrivilegedShellService { *; }
-keep interface com.PriyanshuDeveloper.turbooverlay.IPrivilegedShellService { *; }
-keep class com.PriyanshuDeveloper.turbooverlay.IPrivilegedShellService$* { *; }
-keep interface com.PriyanshuDeveloper.turbooverlay.IPrivilegedShellService$* { *; }

# Keep Shizuku library classes & interfaces completely
-keep class moe.** { *; }
-keep interface moe.** { *; }
-keep class rikka.** { *; }
-keep interface rikka.** { *; }

# Keep View Binding classes
-keep class com.PriyanshuDeveloper.turbooverlay.databinding.** { *; }
