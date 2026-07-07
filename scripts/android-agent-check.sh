#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ID="com.apptists.poplur2025"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"
NO_DEVICE=0
SKIP_TESTS=0
LOG_SECONDS=12
START_EMULATOR=1
AVD_NAME="${ANDROID_AVD_NAME:-}"
MONKEY_EVENTS=500
MONKEY_SEED=20250609
INSTALL_TIMEOUT_SECONDS=180

usage() {
  cat <<USAGE
Usage: ./scripts/android-agent-check.sh [options]

Build, test, install and launch the app, exercise it with Android monkey,
then scan logcat for crashes and likely runtime errors.

Options:
  --no-device           Only run local Gradle checks; skip emulator/install/launch/logcat.
  --no-start-emulator   Do not auto-start an emulator if no device is connected.
  --avd NAME            AVD to start when no device is connected. Default: first emulator -list-avds result.
  --skip-tests          Skip unit tests and only assemble/install/launch.
  --log-seconds N       Seconds to wait after launch/monkey before reading logcat. Default: ${LOG_SECONDS}
  --monkey-events N     Number of pseudo-random UI events to run. Default: ${MONKEY_EVENTS}
  --monkey-seed N       Seed for repeatable monkey runs. Default: ${MONKEY_SEED}
  --install-timeout N   Seconds to wait for APK install. Default: ${INSTALL_TIMEOUT_SECONDS}
  -h, --help            Show this help.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-device)
      NO_DEVICE=1
      shift
      ;;
    --skip-tests)
      SKIP_TESTS=1
      shift
      ;;
    --no-start-emulator)
      START_EMULATOR=0
      shift
      ;;
    --avd)
      AVD_NAME="${2:-}"
      if [[ -z "${AVD_NAME}" ]]; then
        echo "error: --avd requires an AVD name" >&2
        exit 2
      fi
      shift 2
      ;;
    --log-seconds)
      LOG_SECONDS="${2:-}"
      if [[ -z "${LOG_SECONDS}" || ! "${LOG_SECONDS}" =~ ^[0-9]+$ ]]; then
        echo "error: --log-seconds requires a number" >&2
        exit 2
      fi
      shift 2
      ;;
    --monkey-events)
      MONKEY_EVENTS="${2:-}"
      if [[ -z "${MONKEY_EVENTS}" || ! "${MONKEY_EVENTS}" =~ ^[0-9]+$ ]]; then
        echo "error: --monkey-events requires a number" >&2
        exit 2
      fi
      shift 2
      ;;
    --monkey-seed)
      MONKEY_SEED="${2:-}"
      if [[ -z "${MONKEY_SEED}" || ! "${MONKEY_SEED}" =~ ^[0-9]+$ ]]; then
        echo "error: --monkey-seed requires a number" >&2
        exit 2
      fi
      shift 2
      ;;
    --install-timeout)
      INSTALL_TIMEOUT_SECONDS="${2:-}"
      if [[ -z "${INSTALL_TIMEOUT_SECONDS}" || ! "${INSTALL_TIMEOUT_SECONDS}" =~ ^[0-9]+$ ]]; then
        echo "error: --install-timeout requires a number" >&2
        exit 2
      fi
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "error: unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

cd "${ROOT_DIR}"

fail() {
  echo
  echo "FAIL: $*" >&2
  exit 1
}

info() {
  echo
  echo "==> $*"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required but was not found on PATH."
}

run_with_timeout() {
  local seconds="$1"
  shift
  perl -e 'alarm shift; exec @ARGV' "${seconds}" "$@"
}

temp_file() {
  mktemp "${TMPDIR:-/tmp}/$1.XXXXXX"
}

if [[ -f local.properties ]]; then
  SDK_DIR="$(sed -n 's/^sdk.dir=//p' local.properties | tail -n 1)"
  if [[ -n "${SDK_DIR}" ]]; then
    export ANDROID_HOME="${ANDROID_HOME:-${SDK_DIR}}"
    export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${SDK_DIR}}"
    export PATH="${SDK_DIR}/platform-tools:${SDK_DIR}/emulator:${PATH}"
  fi
fi

if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then
  ANDROID_STUDIO_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  if [[ -x "${ANDROID_STUDIO_JBR}/bin/java" ]]; then
    export JAVA_HOME="${ANDROID_STUDIO_JBR}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
    echo "Using Android Studio bundled JDK: ${JAVA_HOME}"
  else
    fail "Java is required to run Gradle. Install a JDK, then rerun: ./scripts/android-agent-check.sh --no-device"
  fi
fi

if [[ ! -x ./gradlew ]]; then
  fail "./gradlew is missing or not executable."
fi

if [[ ! -f app/google-services.json ]]; then
  echo "warning: app/google-services.json is missing. Firebase Gradle setup may fail until it is added."
fi

if [[ -z "${MAPBOX_DOWNLOADS_TOKEN:-}" ]] && ! grep -q '^MAPBOX_DOWNLOADS_TOKEN=' gradle.properties 2>/dev/null; then
  echo "warning: MAPBOX_DOWNLOADS_TOKEN is not set in the environment or gradle.properties. Mapbox dependency resolution may fail."
fi

GRADLE_TASKS=(assembleDebug)
if [[ "${SKIP_TESTS}" -eq 0 ]]; then
  GRADLE_TASKS=(testDebugUnitTest assembleDebug)
fi

info "Running Gradle checks: ${GRADLE_TASKS[*]}"
./gradlew "${GRADLE_TASKS[@]}"

if [[ "${NO_DEVICE}" -eq 1 ]]; then
  info "Local checks passed. Device run skipped because --no-device was supplied."
  exit 0
fi

require_cmd adb

if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  if ! adb devices | awk -v serial="${ANDROID_SERIAL}" 'NR > 1 && $1 == serial && $2 == "device" { found = 1 } END { exit found ? 0 : 1 }'; then
    fail "ANDROID_SERIAL is set to ${ANDROID_SERIAL}, but that device is not connected and authorized."
  fi
  DEVICE_COUNT=1
else
  DEVICE_COUNT="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
fi
if [[ "${DEVICE_COUNT}" -eq 0 ]]; then
  if [[ "${START_EMULATOR}" -eq 0 ]]; then
    fail "No authorized Android device/emulator found. Start an emulator or connect a device, then run this script again. Use --no-device for local-only checks."
  fi

  require_cmd emulator
  if [[ -z "${AVD_NAME}" ]]; then
    AVD_NAME="$(emulator -list-avds | sed -n '1p')"
    if [[ -z "${AVD_NAME}" ]]; then
      fail "No Android Virtual Devices were found. Create an emulator in Android Studio, or pass --avd NAME."
    fi
  fi

  info "Starting emulator AVD: ${AVD_NAME}"
  EMULATOR_LOG="$(temp_file poplur-emulator)"
  nohup emulator -avd "${AVD_NAME}" -no-snapshot-save > "${EMULATOR_LOG}" 2>&1 &

  info "Waiting for emulator to connect"
  CONNECT_DEADLINE=$((SECONDS + 90))
  until adb devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit found ? 0 : 1 }'; do
    if (( SECONDS > CONNECT_DEADLINE )); then
      echo "Emulator log: ${EMULATOR_LOG}"
      fail "Timed out waiting for emulator to connect to adb."
    fi
    sleep 3
  done

  info "Waiting for Android boot to complete"
  BOOT_DEADLINE=$((SECONDS + 180))
  until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
    if (( SECONDS > BOOT_DEADLINE )); then
      echo "Emulator log: ${EMULATOR_LOG}"
      fail "Timed out waiting for emulator boot."
    fi
    sleep 3
  done

  adb shell input keyevent 82 >/dev/null 2>&1 || true
  DEVICE_COUNT="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
fi
if [[ -z "${ANDROID_SERIAL:-}" && "${DEVICE_COUNT}" -gt 1 ]]; then
  fail "Multiple devices are connected. Set ANDROID_SERIAL to one device and rerun."
fi

if ! adb shell cmd activity get-current-user >/dev/null 2>&1; then
  fail "The emulator is connected to adb, but Android's activity service is not healthy. Cold boot or wipe the AVD, then rerun."
fi
if ! adb shell cmd package list packages >/dev/null 2>&1; then
  fail "The emulator is connected to adb, but Android's package service is not healthy. Cold boot or wipe the AVD, then rerun."
fi

info "Installing debug build"
APK_PATH=""
for candidate in \
  "app/build/outputs/apk/debug/app-debug.apk" \
  "/private/tmp/poplur-android-build/app/outputs/apk/debug/app-debug.apk"
do
  if [[ -f "${candidate}" ]]; then
    APK_PATH="${candidate}"
    break
  fi
done
if [[ -z "${APK_PATH}" ]]; then
  fail "Debug APK not found in app/build/outputs/apk/debug or /private/tmp/poplur-android-build/app/outputs/apk/debug."
fi
run_with_timeout "${INSTALL_TIMEOUT_SECONDS}" adb push "${APK_PATH}" /data/local/tmp/poplur-debug.apk
run_with_timeout "${INSTALL_TIMEOUT_SECONDS}" adb shell pm install -r /data/local/tmp/poplur-debug.apk

info "Clearing logcat and launching ${MAIN_ACTIVITY}"
adb logcat -c || true
adb shell am force-stop "${APP_ID}" || true
adb shell am start -W -n "${MAIN_ACTIVITY}"

info "Waiting ${LOG_SECONDS}s for startup errors"
sleep "${LOG_SECONDS}"

info "Exercising app with monkey: ${MONKEY_EVENTS} events, seed ${MONKEY_SEED}"
MONKEY_FILE="$(temp_file poplur-monkey)"
adb shell monkey \
  -p "${APP_ID}" \
  --ignore-security-exceptions \
  --monitor-native-crashes \
  --pct-syskeys 0 \
  --pct-nav 0 \
  --pct-majornav 0 \
  --pct-appswitch 0 \
  --pct-anyevent 0 \
  -s "${MONKEY_SEED}" \
  --throttle 120 \
  -v "${MONKEY_EVENTS}" \
  > "${MONKEY_FILE}" || true

sleep "${LOG_SECONDS}"

LOG_FILE="$(temp_file poplur-logcat)"
adb logcat -d > "${LOG_FILE}"

ERROR_FILE="$(temp_file poplur-errors)"
{
  grep -E "FATAL EXCEPTION|${APP_ID}|am_crash|am_anr|ANR in ${APP_ID}|StrictMode|NoClassDefFoundError|ClassNotFoundException|UnsatisfiedLinkError" "${LOG_FILE}" \
    | grep -E "FATAL EXCEPTION|am_crash|am_anr|ANR in|Exception|StrictMode|NoClassDefFoundError|ClassNotFoundException|UnsatisfiedLinkError|java\\.lang\\.Error" \
    | grep -Ev "WindowManager: win=.*destroySurfaces|SemWifiTransportLayerUtils: getApplicationCategory|GmsStrictMode|com.google.android.gms@|com.google.android.apps.nexuslauncher|Taskbar" || true
  grep -E "CRASH: ${APP_ID}|ANR in ${APP_ID}|native crash.*${APP_ID}|No activities found|monkey aborted" "${MONKEY_FILE}" || true
} > "${ERROR_FILE}"

if [[ -s "${ERROR_FILE}" ]]; then
  echo
  echo "Recent startup errors from logcat:"
  tail -n 160 "${ERROR_FILE}"
  echo
  echo "Full logcat: ${LOG_FILE}"
  echo "Monkey output: ${MONKEY_FILE}"
  fail "App launched, but the automated crawl found likely crashes or runtime errors."
fi

info "App installed, launched, exercised, and no likely app crashes were found."
echo "Full logcat: ${LOG_FILE}"
echo "Monkey output: ${MONKEY_FILE}"
