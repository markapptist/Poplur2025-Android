# Android Cube iOS Match Notes

The Android cube has been updated to mirror the uploaded iOS `CubeTabView` / `HomeView` cube behavior more closely.

## Matched iOS details

- Floating bottom cube is now shown at `200.dp`, closer to the iOS cube overlay size of roughly 230 pt.
- Camera now uses a 40° perspective field of view and z = 5, matching the SceneKit camera spec.
- Cube geometry uses the same 1.3 scene-unit size.
- Face order and yaw now match the iOS navigation spec:
  - Record / Camera: 0°
  - Feed: 90°
  - Profile: 180°
  - Map: 270°
- The default selected tab remains Feed, matching `HomeView`.
- Horizontal drag uses the iOS sensitivity: about 0.5° per screen point.
- Release snap now uses an iOS-style overshoot and settle animation: 0.18s overshoot, 0.22s settle.
- Face textures now show an indigo-tinted rounded panel with large white icon artwork and bold tab titles.
- The Android cube keeps transparent GL output so it floats over the app like the iOS SceneKit cube.

## Android equivalent limitations

- SceneKit's real chamfered `SCNBox(chamferRadius: 0.25)` is approximated visually in LibGDX using rounded face artwork. A true beveled mesh can be added later if you want exact rounded cube edges.
- SF Symbols are recreated as simple vector-like LibGDX shapes, because Android does not have SF Symbols.
- iOS notification callbacks for cube tap/long-hold are logged in the LibGDX adapter right now; wiring those to real Compose camera/map/feed/profile callbacks is the next step if you want full behavior parity.
