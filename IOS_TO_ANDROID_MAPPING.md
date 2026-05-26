# iOS to Android API Mapping

- `SwiftUI` -> Jetpack Compose
- `UIKit` / `UIViewControllerRepresentable` -> Compose `AndroidView`, Android Activity/View interop
- `SceneKit` / `SpriteKit` -> LibGDX (`ApplicationAdapter`, `ShapeRenderer`, `SpriteBatch`)
- `MapKit` -> Google Maps SDK for Android
- `CoreLocation` -> Google Play Services Fused Location Provider
- `AVFoundation` camera -> CameraX
- `AVKit` video playback -> AndroidX Media3 ExoPlayer
- `Vision` / `CoreML` -> Google ML Kit and optional TensorFlow Lite
- `ARKit` / `RealityKit` -> ARCore or Snap CameraKit Android where supported
- `FirebaseAuth`, `FirebaseFirestore`, `FirebaseStorage`, `FirebaseCore` -> Firebase Android KTX libraries
- `GoogleMobileAds` -> Google Mobile Ads Android SDK
- `StoreKit` -> Google Play Billing
- `UserNotifications` -> Android notification channels + `POST_NOTIFICATIONS`
- `Photos` / iOS image picker -> Android Photo Picker / Activity Result APIs
- `WeatherKit` -> Weather REST API, because there is no native Android WeatherKit SDK
- `SCSDKCameraKit`, `SCSDKLoginKit` -> Snap CameraKit/LoginKit for Android
