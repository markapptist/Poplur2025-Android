# AGENTS.md

## Project Expectations

- This is an Android/Kotlin project using Gradle, Jetpack Compose, Firebase, Mapbox, CameraX, ML Kit, Billing, Media3, and LibGDX.
- Keep changes focused and avoid unrelated refactors.
- After modifying Kotlin, XML, Gradle, manifest, or resource files, run `./scripts/android-agent-check.sh --no-device` at minimum.
- Run `./scripts/android-agent-check.sh` to build, start the default emulator if needed, install, launch, crawl the app with Android monkey, and scan logcat for crashes.
- Use `./scripts/android-agent-check.sh --monkey-events 1500` for a deeper automated crawl.
- If Gradle cannot run because Java is missing, install a JDK first and rerun the same check.
- If Mapbox dependencies cannot resolve, verify `MAPBOX_DOWNLOADS_TOKEN` is set in the environment or Gradle properties.
- Do not commit secrets such as Firebase configs, keystores, service account files, API keys, or tokens.
