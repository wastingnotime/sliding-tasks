#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
apk_path="$project_dir/app/build/outputs/apk/debug/app-debug.apk"
package_name="org.wastingnotime.slidingtasks"
activity_name="$package_name/.MainActivity"

adb_bin="${ADB_BIN:-}"
if [[ -z "$adb_bin" ]] && command -v adb >/dev/null 2>&1; then
  adb_bin="$(command -v adb)"
fi
if [[ -z "$adb_bin" && -n "${ANDROID_SDK_ROOT:-}" && -x "$ANDROID_SDK_ROOT/platform-tools/adb" ]]; then
  adb_bin="$ANDROID_SDK_ROOT/platform-tools/adb"
fi
if [[ -z "$adb_bin" && -n "${ANDROID_HOME:-}" && -x "$ANDROID_HOME/platform-tools/adb" ]]; then
  adb_bin="$ANDROID_HOME/platform-tools/adb"
fi
if [[ -z "$adb_bin" ]]; then
  echo "adb was not found. Install Android SDK Platform Tools or set ADB_BIN." >&2
  exit 1
fi

device_serial="${DEVICE_SERIAL:-}"
if [[ -z "$device_serial" ]]; then
  mapfile -t physical_devices < <("$adb_bin" devices | awk '$2 == "device" && $1 !~ /^emulator-/ { print $1 }')
  if [[ "${#physical_devices[@]}" -eq 0 ]]; then
    echo "No authorized physical Android device found. Connect one with USB debugging enabled." >&2
    exit 1
  fi
  if [[ "${#physical_devices[@]}" -gt 1 ]]; then
    echo "More than one physical device is connected. Choose one with: make mobile-install DEVICE_SERIAL=<serial>" >&2
    printf '  %s\n' "${physical_devices[@]}" >&2
    exit 1
  fi
  device_serial="${physical_devices[0]}"
fi

device_state="$($adb_bin -s "$device_serial" get-state 2>/dev/null || true)"
if [[ "$device_state" != "device" ]]; then
  echo "Android device $device_serial is not connected and authorized." >&2
  exit 1
fi

echo "Building Sliding Tasks"
(cd "$project_dir" && ./gradlew assembleDebug)
echo "Installing on $device_serial"
"$adb_bin" -s "$device_serial" install -r "$apk_path" >/dev/null
"$adb_bin" -s "$device_serial" shell am start -n "$activity_name" >/dev/null
echo "Sliding Tasks is running on $device_serial"
