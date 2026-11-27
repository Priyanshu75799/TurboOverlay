# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Shizuku UserService & AIDL Interfaces - Keep these names stable in release APKs.
-keep class com.PriyanshuDeveloper.turbooverlay.phoenixengine.PrivilegedShellService { <init>(); *; }
-keep class com.PriyanshuDeveloper.turbooverlay.IPrivilegedShellService { *; }
-keep interface com.PriyanshuDeveloper.turbooverlay.IPrivilegedShellService { *; }
-keep class com.PriyanshuDeveloper.turbooverlay.IPrivilegedShellService$* { *; }
-keep interface com.PriyanshuDeveloper.turbooverlay.IPrivilegedShellService$* { *; }

# Keep all models and frontend/engine components
-keep class com.PriyanshuDeveloper.turbooverlay.frontend.** { *; }
-keep class com.PriyanshuDeveloper.turbooverlay.phoenixengine.** { *; }

# Keep Shizuku library classes & interfaces completely
-keep class moe.** { *; }
-keep interface moe.** { *; }
-keep class dev.rikka.** { *; }
-keep interface dev.rikka.** { *; }
-keep class rikka.** { *; }
-keep interface rikka.** { *; }

# Keep View Binding classes
-keep class com.PriyanshuDeveloper.turbooverlay.databinding.** { *; }
