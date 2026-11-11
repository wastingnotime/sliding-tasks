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

# This developer command always uses a standalone visible emulator window.
# Automation may still launch headless emulators independently.
running_serial=""
avd_name="${MOBILE_AVD:-}"
restart_port=""
while read -r candidate; do
  candidate_avd="$($adb_bin -s "$candidate" emu avd name | tr -d '\r' | head -n 1)"
  [[ -z "$avd_name" || "$avd_name" == "$candidate_avd" ]] || continue
  avd_name="$candidate_avd"
  if python3 - "$candidate_avd" <<'PY_HEADLESS'
import pathlib
import sys
for entry in pathlib.Path('/proc').glob('[0-9]*/cmdline'):
    try:
        args = entry.read_bytes().decode().split('\0')
    except (OSError, UnicodeError):
        continue
    if not args or not ('qemu-system' in args[0] or pathlib.Path(args[0]).name == 'emulator'):
        continue
    avd = args[args.index('-avd') + 1] if '-avd' in args else next((a[1:] for a in args if a.startswith('@')), '')
    if avd == sys.argv[1] and any(flag in args for flag in ('-no-window', '-qt-hide-window')):
        sys.exit(0)
sys.exit(1)
PY_HEADLESS
  then
    echo "Restarting headless emulator with a visible window: $avd_name"
    restart_port="${candidate#emulator-}"
    "$adb_bin" -s "$candidate" emu kill >/dev/null
    # Wait for the old process to release the AVD lock and console port.
    for _ in {1..30}; do
      if ! "$adb_bin" devices | awk '{print $1}' | grep -Fxq "$candidate"; then break; fi
      sleep 1
    done
    sleep 2
  else
    running_serial="$candidate"
  fi
  break
done < <("$adb_bin" devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ { print $1 }')

if [[ -z "$running_serial" ]]; then
  if [[ -z "$avd_name" ]]; then
    avd_name="$("$emulator_bin" -list-avds | head -n 1)"
  fi
  if [[ -z "$avd_name" ]]; then
    echo "No Android Virtual Device exists. Create one in Android Studio Device Manager."
    exit 1
  fi

  emulator_args=(-avd "$avd_name")
  [[ -z "$restart_port" ]] || emulator_args+=(-port "$restart_port")
  echo "Starting visible Android emulator: $avd_name"
  nohup "$emulator_bin" "${emulator_args[@]}" >"${TMPDIR:-/tmp}/sliding-tasks-emulator.log" 2>&1 &

  for _ in {1..180}; do
    while read -r candidate; do
      candidate_avd="$("$adb_bin" -s "$candidate" emu avd name | tr -d '\r' | head -n 1)"
      if [[ "$candidate_avd" == "$avd_name" ]]; then running_serial="$candidate"; break; fi
    done < <("$adb_bin" devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ { print $1 }')
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
