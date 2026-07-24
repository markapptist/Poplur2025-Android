# Add project specific ProGuard rules here.
# By default, the flags in this file are appended after configuration specified
# in /Users/matrix/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the configuration file and add direct link to your library.

-keep class com.badlogic.gdx.backends.android.** { *; }
-keep class com.badlogic.gdx.physics.box2d.backends.android.** { *; }
-keep class com.apptists.poplur2025.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ── First-release keep rules (R8 broke reflection-based init; measured on A03s) ──
# WorkManager/Room: generated WorkDatabase_Impl is looked up reflectively
-keep class androidx.work.impl.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
# libGDX: reflection-heavy (scene loaders, fragment backends)
-keep class com.badlogic.** { *; }
-dontwarn com.badlogic.**
# Agora RTC native bindings
-keep class io.agora.** { *; }
-dontwarn io.agora.**
# Firebase/Firestore deserialize app models via reflection — keep the app's own
# classes unobfuscated (assets dominate APK size; code minify of the app package
# buys little and silently corrupts toObject() mapping)
-keep class com.apptists.poplur2025.** { *; }
# First release: shrink but DO NOT rename — the app+SDK mix relies on reflection
# in many places (Firebase AppCheck/Play Integrity, Coil, WorkManager, libGDX);
# obfuscation buys no runtime performance and every rename is a startup crash.
-dontobfuscate
# Firebase/GMS: components are discovered reflectively via ComponentRegistrar —
# shrinking strips them (AppCheck registered null). Keep the SDKs whole.
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
