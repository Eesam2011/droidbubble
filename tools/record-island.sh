#!/usr/bin/env bash
# Records the phone's screen to an .mp4 on this PC with scrcpy, while tapping the island to
# open it. ColorOS blocks `adb shell screenrecord`, so this uses scrcpy (winget: Genymobile.scrcpy).
#
#   tools/record-island.sh <out.mp4> [animator-scale] [seconds]
#   tools/record-island.sh island_1x.mp4            # 1x, 9 s, taps the pill after 2.5 s
#   tools/record-island.sh island_2x.mp4 2.0 12
#   NO_TAP=1 tools/record-island.sh free.mp4 1.0 20  # just record; you drive the phone
#
# The animator scale is set back to what it was when the recording ends.
set -euo pipefail
export MSYS_NO_PATHCONV=1

out=${1:?usage: record-island.sh <out.mp4> [animator-scale] [seconds]}
scale=${2:-}
secs=${3:-9}

ADB=${ADB:-$(command -v adb || echo C:/Android/platform-tools/adb.exe)}
export ADB
SCRCPY=${SCRCPY:-$(command -v scrcpy || ls -d "$LOCALAPPDATA"/Microsoft/WinGet/Packages/Genymobile.scrcpy*/*/scrcpy.exe 2>/dev/null | head -1)}
[ -n "$SCRCPY" ] || { echo "scrcpy not found: winget install Genymobile.scrcpy" >&2; exit 1; }

old_scale=$("$ADB" shell settings get global animator_duration_scale | tr -d '\r')
restore() { "$ADB" shell settings put global animator_duration_scale "$old_scale"; }
if [ -n "$scale" ]; then
  trap restore EXIT
  "$ADB" shell settings put global animator_duration_scale "$scale"
  sleep 1
fi

"$SCRCPY" --no-window --no-audio --no-control --video-bit-rate=16M --max-fps=60 \
  --record="$out" --time-limit="$secs" >/dev/null 2>&1 &
rec=$!
if [ -z "${NO_TAP:-}" ]; then
  sleep 2.5
  "$ADB" shell input tap 360 40   # the pill, top centre (720 px wide screen)
fi
wait $rec
echo "saved $out"
