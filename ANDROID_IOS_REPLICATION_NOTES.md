# Android ↔ iOS Replication Notes

This Android project has been updated to mirror the uploaded iOS project more closely.

## Added / aligned

- iOS-style launch/auth gate: unauthenticated users now see a Poplur welcome/login/signup flow before Home.
- Main navigation preserves the iOS 4-tab cube order: Record/Camera, Feed, Profile, Map.
- Profile now includes:
  - profile avatar/name/username/bio/stats,
  - Media and Messages tabs,
  - Edit Profile screen,
  - Settings screen,
  - logout support.
- Map now behaves more like the iOS map flow:
  - hybrid map mode,
  - fixed high zoom range,
  - disabled default map toolbar/labels controls where possible,
  - visible post markers,
  - radius circles around posts,
  - tap-to-select coordinate,
  - choose-media action for the selected coordinate.
- Camera now behaves closer to the iOS record screen:
  - live CameraX preview,
  - front/back camera toggle,
  - torch toggle,
  - gallery picker,
  - capture/record button state,
  - scanning overlay,
  - radius overlay.
- Settings copy now maps the iOS frameworks to Android equivalents: Firebase, Google Maps, CameraX, ML Kit, Media3, AdMob, Play Billing, and LibGDX.

## Still requires real platform credentials/services

- Replace `app/google-services.json` with the real Android Firebase config for `com.apptists.poplur2025`.
- Verify the Google Maps API key and restrict it in Google Cloud Console.
- Add real upload/write paths for Firebase Storage + Firestore when you are ready to persist selected media.
- Add Snap CameraKit/LoginKit only if you have valid Snap Android credentials.

## Build note

I could not run a full Gradle compile in this sandbox because the project wrapper attempted to download Gradle from `services.gradle.org`, and network access is unavailable here. Open the patched project in Android Studio, let Gradle sync, then fix any version-specific API warnings that your local Android Studio reports.
