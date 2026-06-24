# Add project specific ProGuard rules here.
# By default, the flags in this file are appended after configuration specified
# in /Users/matrix/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the configuration file and add direct link to your library.

-keep class com.badlogic.gdx.backends.android.** { *; }
-keep class com.badlogic.gdx.physics.box2d.backends.android.** { *; }
-keep class com.apptists.poplur2025.data.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
