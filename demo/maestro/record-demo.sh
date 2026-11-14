#!/usr/bin/env bash
# Records the demo tour on a running device and makes a video and a GIF of it.
#
#   demo/maestro/record-demo.sh android|ios
#
# Android: the device from ANDROID_SERIAL, else the only one `adb devices` shows. The tour is the
# instrumented test `DemoTourTest` (androidApp/src/androidTest); the script installs the release build
# and the test first, so Gradle does not run while the screen is recorded. iOS: the booted simulator with
# the app installed, the tour is the Maestro flow `tour.yaml`. The system language is English. The status bar shows 9:41 and a full battery while recording and is
# given back after, also when the tour fails.
#
# Output in demo/build/demo-video: <platform>-raw.mp4 (as recorded), tour-<platform>.mp4 (for the
# demo README) and tour-<platform>.gif (for the library README).
#
# Settings by environment: VIDEO_SPEED (1.5), VIDEO_WIDTH (720), GIF_SPEED (2), GIF_FPS (12),
# GIF_WIDTH (320), GIF_QUALITY (90), TRIM_START (seconds cut at the start, 0).
set -euo pipefail

PLATFORM="${1:-}"
case "$PLATFORM" in
  android | ios) ;;
  *) echo "Usage: $0 android|ios" >&2; exit 1 ;;
esac

HERE="$(cd "$(dirname "$0")" && pwd)"
DEMO="$(dirname "$HERE")"
OUT="$DEMO/build/demo-video"
RAW="$OUT/$PLATFORM-raw.mp4"
VIDEO="$OUT/tour-$PLATFORM.mp4"
GIF="$OUT/tour-$PLATFORM.gif"

VIDEO_SPEED="${VIDEO_SPEED:-1.5}"
VIDEO_WIDTH="${VIDEO_WIDTH:-720}"
GIF_SPEED="${GIF_SPEED:-2}"
GIF_FPS="${GIF_FPS:-12}"
GIF_WIDTH="${GIF_WIDTH:-320}"
GIF_QUALITY="${GIF_QUALITY:-90}"
TRIM_START="${TRIM_START:-0}"

export PATH="$HOME/.maestro/bin:$PATH"
export MAESTRO_CLI_NO_ANALYTICS=1
export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true

for tool in ffmpeg gifski; do
  command -v "$tool" > /dev/null || { echo "$tool is not installed" >&2; exit 1; }
done
[[ "$PLATFORM" == ios ]] && { command -v maestro > /dev/null || { echo "maestro is not installed" >&2; exit 1; }; }
mkdir -p "$OUT"

# --- Platform: device, app id, status bar, screen recording ---------------------------------------

if [[ "$PLATFORM" == android ]]; then
  command -v adb > /dev/null || { echo "adb is not found" >&2; exit 1; }
  DEVICE="${ANDROID_SERIAL:-$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')}"
  [[ -n "$DEVICE" && "$DEVICE" != *$'\n'* ]] || { echo "Connect one Android device or set ANDROID_SERIAL" >&2; exit 1; }
  APP_ID="io.github.wladyslawpopov.kpage.demo"
  DEVICE_FILE="/sdcard/kpage-tour.mp4"
  TEST_CLASS="io.github.wladyslawpopov.kpage.demo.android.DemoTourTest"

  prepare() {
    (cd "$DEMO" && ANDROID_SERIAL="$DEVICE" ./gradlew --quiet :androidApp:installRelease :androidApp:installReleaseAndroidTest)
  }
  run_tour() {
    local runner log
    runner="$(adb -s "$DEVICE" shell pm list instrumentation | sed -n "s/^instrumentation:\([^ ]*\) .*/\1/p" | grep -m1 "^$APP_ID")"
    [[ -n "$runner" ]] || { echo "The test is not installed on the device" >&2; return 1; }
    log="$(adb -s "$DEVICE" shell am instrument -w -e class "$TEST_CLASS" "$runner" | tr -d '\r')"
    echo "$log"
    # `am instrument` exits 0 also when the test fails: read its verdict.
    grep -q "^OK (" <<< "$log"
  }

  # SystemUI demo mode: a fixed clock and full icons, no notifications.
  demo() { adb -s "$DEVICE" shell am broadcast -a com.android.systemui.demo -e command "$@" > /dev/null; }
  status_bar_on() {
    if [[ "$(adb -s "$DEVICE" shell settings get global sysui_demo_allowed | tr -d '\r')" != 1 ]]; then
      echo "Demo mode of the status bar is off on the device; allow it once with:" >&2
      echo "  adb -s $DEVICE shell settings put global sysui_demo_allowed 1" >&2
      exit 1
    fi
    demo enter
    demo clock -e hhmm 0941
    demo battery -e level 100 -e plugged false
    demo network -e wifi show -e level 4
    demo network -e mobile show -e datatype none -e level 4
    demo notifications -e visible false
  }
  status_bar_off() { demo exit; }

  # screenrecord stops by itself after 3 minutes; the tour is shorter.
  record_start() {
    adb -s "$DEVICE" shell rm -f "$DEVICE_FILE"
    adb -s "$DEVICE" shell screenrecord --bit-rate 16000000 "$DEVICE_FILE" &
    RECORDER=$!
  }
  record_stop() {
    adb -s "$DEVICE" shell pkill -INT screenrecord || true
    wait "$RECORDER" || true
    sleep 1 # the file is finished after the process ends
    adb -s "$DEVICE" pull "$DEVICE_FILE" "$RAW" > /dev/null
    adb -s "$DEVICE" shell rm -f "$DEVICE_FILE"
  }
else
  DEVICE="$(xcrun simctl list devices booted | grep -Eo '[0-9A-F]{8}(-[0-9A-F]{4}){3}-[0-9A-F]{12}' || true)"
  [[ -n "$DEVICE" && "$DEVICE" != *$'\n'* ]] || { echo "Boot one iOS simulator" >&2; exit 1; }
  # The bundle id ends with the Apple team (iosApp/Configuration/Config.xcconfig).
  TEAM_ID="$(sed -n 's/^TEAM_ID *= *//p' "$DEMO/iosApp/Configuration/Local.xcconfig" 2> /dev/null | tail -1 | tr -d ' ')"
  APP_ID="io.github.wladyslawpopov.kpage.demo.KPageDemo$TEAM_ID"

  prepare() { run_flow launch.yaml; }
  run_tour() { run_flow tour.yaml; }

  status_bar_on() {
    xcrun simctl status_bar "$DEVICE" override --time 9:41 --batteryState charged --batteryLevel 100 \
      --dataNetwork wifi --wifiMode active --wifiBars 3 --cellularMode active --cellularBars 4
  }
  status_bar_off() { xcrun simctl status_bar "$DEVICE" clear; }

  # simctl itself, not xcrun: the signal that ends the recording must reach it.
  SIMCTL="$(xcrun -f simctl)"
  record_start() {
    "$SIMCTL" io "$DEVICE" recordVideo --codec=h264 --force "$RAW" 2> /dev/null &
    RECORDER=$!
    sleep 2 # recordVideo needs a moment before the first frame
  }
  record_stop() {
    kill -INT "$RECORDER" 2> /dev/null || true
    wait "$RECORDER" || true
  }
fi

RECORDER=""
# A failed tour still ends the recording (kept for a look) and gives the status bar back.
cleanup() {
  if [[ -n "$RECORDER" ]]; then record_stop || true; fi
  status_bar_off || true
}
trap cleanup EXIT

run_flow() { maestro test --device "$DEVICE" -e APP_ID="$APP_ID" "$HERE/$1"; }

# --- Recording ------------------------------------------------------------------------------------

echo "Before recording: Favorites empty, the first chat not FLAKY, the app and the system in English."
echo "Device $DEVICE, app $APP_ID"

status_bar_on
prepare
record_start
run_tour
sleep 2 # a still moment at the end
record_stop
RECORDER=""
status_bar_off

# --- Editing --------------------------------------------------------------------------------------

ffmpeg -loglevel error -y -ss "$TRIM_START" -i "$RAW" -an \
  -vf "setpts=PTS/$VIDEO_SPEED,scale=$VIDEO_WIDTH:-2:flags=lanczos" \
  -c:v libx264 -preset slow -crf 22 -pix_fmt yuv420p -movflags +faststart "$VIDEO"

# gifski makes one palette per frame without the artifacts of frame differences.
FRAMES="$(mktemp -d)"
ffmpeg -loglevel error -ss "$TRIM_START" -i "$RAW" \
  -vf "setpts=PTS/$GIF_SPEED,fps=$GIF_FPS,scale=$GIF_WIDTH:-2:flags=lanczos" "$FRAMES/%05d.png"
gifski --quiet --fps "$GIF_FPS" --quality "$GIF_QUALITY" -o "$GIF" "$FRAMES"/*.png
rm -rf "$FRAMES"

duration() { ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 "$1" | cut -d. -f1; }
echo "Video $VIDEO: $(duration "$VIDEO") s, $(du -h "$VIDEO" | cut -f1)"
echo "GIF   $GIF: $(du -h "$GIF" | cut -f1)"
