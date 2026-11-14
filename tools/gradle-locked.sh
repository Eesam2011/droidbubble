#!/usr/bin/env bash
# Runs ./gradlew with a machine-wide lock, so parallel checkouts (git worktrees) build one at a
# time: one Gradle build needs up to 3 GB and two at once don't fit in this PC's memory.
# Each checkout other than the main one gets its own outputs under ~/.droidbubble-build/wt/<name>.
#
#   tools/gradle-locked.sh :app:testDebugUnitTest --tests "*Foo*" -q
set -uo pipefail
root=$(git rev-parse --show-toplevel)
lock="$HOME/.droidbubble-build/gradle.lock"
mkdir -p "$HOME/.droidbubble-build"
extra=()
if [ "$(git rev-parse --git-dir)" != "$(git rev-parse --git-common-dir)" ]; then
  extra=(-PdroidbubbleBuildRoot="$HOME/.droidbubble-build/wt/$(basename "$root")")
fi
waited=0
until mkdir "$lock" 2>/dev/null; do
  # A lock older than 40 minutes was left behind by a killed build
  if [ -n "$(find "$lock" -maxdepth 0 -mmin +40 2>/dev/null)" ]; then rmdir "$lock" 2>/dev/null; continue; fi
  [ $waited -eq 0 ] && echo "waiting for another checkout's build..." >&2
  waited=1; sleep 5
done
trap 'rmdir "$lock" 2>/dev/null' EXIT
cd "$root" && ./gradlew "${extra[@]}" "$@"
