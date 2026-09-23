#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
apk_path="$project_dir/app/build/outputs/apk/debug/app-debug.apk"
package_name="org.wastingnotime.slidingtasks"
activity_name="$package_name/.MainActivity"

sdk_candidates=(
  "${ANDROID_SDK_ROOT:-}"
  "${ANDROID_HOME:-}"
  "$HOME/Android/Sdk"
  "$HOME/tools/android-sdk"
  "/opt/android-sdk"
  "/usr/lib/android-sdk"
)

emulator_bin=""
adb_bin=""
for sdk_dir in "${sdk_candidates[@]}"; do
  [[ -n "$sdk_dir" ]] || continue
  if [[ -z "$emulator_bin" && -x "$sdk_dir/emulator/emulator" ]]; then
    emulator_bin="$sdk_dir/emulator/emulator"
  fi
  if [[ -z "$adb_bin" && -x "$sdk_dir/platform-tools/adb" ]]; then
    adb_bin="$sdk_dir/platform-tools/adb"
  fi
done

if [[ -z "$emulator_bin" ]] && command -v emulator >/dev/null 2>&1; then
  emulator_bin="$(command -v emulator)"
fi
if [[ -z "$adb_bin" ]] && command -v adb >/dev/null 2>&1; then
  adb_bin="$(command -v adb)"
fi

if [[ -z "$emulator_bin" || -z "$adb_bin" ]]; then
  echo "Android emulator or adb was not found. Install Android SDK Emulator and Platform Tools." >&2
  exit 1
fi

running_serial="$($adb_bin devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ { print $1; exit }')"

if [[ -z "$running_serial" ]]; then
  avd_name="${MOBILE_AVD:-}"
  if [[ -z "$avd_name" ]]; then
    avd_name="$($emulator_bin -list-avds | head -n 1)"
  fi
  if [[ -z "$avd_name" ]]; then
    echo "No Android Virtual Device exists. Create one in Android Studio Device Manager." >&2
    exit 1
  fi

  echo "Starting Android emulator: $avd_name"
  nohup "$emulator_bin" -avd "$avd_name" >"${TMPDIR:-/tmp}/sliding-tasks-emulator.log" 2>&1 &

  for _ in {1..180}; do
    running_serial="$($adb_bin devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ { print $1; exit }')"
    [[ -n "$running_serial" ]] && break
    sleep 1
  done
fi

if [[ -z "$running_serial" ]]; then
  echo "The emulator did not become available. See ${TMPDIR:-/tmp}/sliding-tasks-emulator.log." >&2
  exit 1
fi

echo "Waiting for Android to finish booting on $running_serial"
for _ in {1..180}; do
  boot_complete="$($adb_bin -s "$running_serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  [[ "$boot_complete" == "1" ]] && break
  sleep 1
done

if [[ "${boot_complete:-}" != "1" ]]; then
  echo "Android did not finish booting within 180 seconds." >&2
  exit 1
fi

echo "Building and installing Sliding Tasks"
(cd "$project_dir" && ./gradlew assembleDebug)
$adb_bin -s "$running_serial" install -r "$apk_path" >/dev/null
$adb_bin -s "$running_serial" shell am start -n "$activity_name" >/dev/null
echo "Sliding Tasks is running on $running_serial"
