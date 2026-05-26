# Poplur2025 Android Conversion

This is an Android/Kotlin conversion scaffold generated from the uploaded `Poplur2025-iOS` project.

## What was converted

| iOS source/framework | Android equivalent applied here |
|---|---|
| SwiftUI views | Jetpack Compose screens in `ui/screens` |
| SceneKit / SpriteKit views (`SceneKitLogoView`, `CubeTabView`, `IsometricRadiusBar`, `MapZoomSlider3D`, `ConfettiSystem`, etc.) | LibGDX renderer in `gdx/PoplurGdxScene.kt` and Compose bridge in `gdx/LibGdxSceneView.kt` |
| MapKit / CoreLocation | Google Maps SDK + Fused Location Provider |
| AVFoundation camera preview/capture | CameraX Preview |
| Vision/CoreML object detection | Google ML Kit object/face detection placeholder manager |
| FirebaseAuth / Firestore / Storage | Firebase Android BoM with Auth, Firestore, Storage dependencies |
| GoogleMobileAds | Google Mobile Ads Android SDK |
| StoreKit | Google Play Billing dependency |
| AVKit video feed | AndroidX Media3 dependency |
| UserNotifications | Android notification permission declared; implement notification channels as needed |
| Photos/ImagePicker | Android photo/video permissions declared; plug in Photo Picker as needed |
| WeatherKit | Use a weather REST API on Android; WeatherKit has no native Android equivalent |
| SCSDKCameraKit / SCSDKLoginKit / Bitmoji | Add Snap Android SDK manually from Snap docs/credentials; placeholders preserved in Settings/filters |

## Setup before running

1. Open this folder in Android Studio.
2. Replace `app/google-services.json` with your real Firebase Android config for package `com.apptists.poplur2025`.
3. Put your Android Google Maps API key in `AndroidManifest.xml` where it says `YOUR_ANDROID_GOOGLE_MAPS_API_KEY`.
4. Verify the AdMob app id in `AndroidManifest.xml`.
5. Add the Snap Android CameraKit/LoginKit dependency and credentials if you want the Snap lenses/Bitmoji path active.
6. Sync Gradle and run on an Android device.

## Important limitation

The uploaded iOS project contains over 34k lines of SwiftUI/UIKit/SceneKit code. This Android project applies the direct platform equivalents and recreates the main app architecture, demo data, tabs, camera, map, feed/profile/messages/settings screens, Firebase wiring, and LibGDX animation layer. Highly custom SwiftUI layouts and every individual animation are represented by Android equivalents/stubs, not a byte-for-byte UI clone.
