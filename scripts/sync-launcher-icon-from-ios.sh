#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_ICON="${1:-${ROOT_DIR}/../Poplur2025-iOS/Poplur2025/Assets.xcassets/AppIcon.appiconset/Icon-App-1024x1024@1x.png}"

if [[ ! -f "${SOURCE_ICON}" ]]; then
  echo "error: source app icon not found: ${SOURCE_ICON}" >&2
  exit 1
fi

resize_icon() {
  local size="$1"
  local output="$2"
  sips -z "${size}" "${size}" "${SOURCE_ICON}" --out "${output}" >/dev/null
}

for spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  density="${spec%%:*}"
  size="${spec##*:}"
  directory="${ROOT_DIR}/app/src/main/res/mipmap-${density}"
  resize_icon "${size}" "${directory}/ic_launcher.png"
  resize_icon "${size}" "${directory}/ic_launcher_round.png"
done

# Adaptive launcher foreground canvases are 108dp at each density. Keeping the
# complete approved icon in the layer lets Android apply the device mask while
# preserving the same artwork as iOS.
for spec in mdpi:108 hdpi:162 xhdpi:216 xxhdpi:324 xxxhdpi:432; do
  density="${spec%%:*}"
  size="${spec##*:}"
  resize_icon "${size}" "${ROOT_DIR}/app/src/main/res/mipmap-${density}/ic_launcher_foreground.png"
done

echo "Synced Android launcher icons from ${SOURCE_ICON}"
