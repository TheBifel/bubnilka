#!/usr/bin/env bash
set -euo pipefail

toolchain_root="${BUBNILKA_TOOLCHAIN_ROOT:-/home/aiagent/.local/share/bubnilka-toolchain}"
android_home="${ANDROID_HOME:-$toolchain_root/android-sdk}"
adb_bin="$android_home/platform-tools/adb"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
serial="${ANDROID_SERIAL:-emulator-5554}"
package="com.thebifel.bubnilka"
activity="$package/.MainActivity"

elapsed_ms() {
    local started="$1"
    echo $(( $(date +%s%3N) - started ))
}

"$adb_bin" -s "$serial" get-state >/dev/null

echo "# Bubnilka emulator benchmark"
echo
echo "- Device: $serial, Android $("$adb_bin" -s "$serial" shell getprop ro.build.version.release | tr -d '\r')"
echo "- ABI: $("$adb_bin" -s "$serial" shell getprop ro.product.cpu.abi | tr -d '\r')"
echo
echo "## Activity launch (Android am start -W)"
for run in 1 2 3; do
    "$adb_bin" -s "$serial" shell am force-stop "$package"
    result="$($adb_bin -s "$serial" shell am start -W -n "$activity")"
    total="$(printf '%s\n' "$result" | awk -F': ' '/TotalTime/ {gsub("\\r", "", $2); print $2}')"
    wait="$(printf '%s\n' "$result" | awk -F': ' '/WaitTime/ {gsub("\\r", "", $2); print $2}')"
    echo "- Cold $run: TotalTime=${total:-n/a} ms, WaitTime=${wait:-n/a} ms"
done

echo
echo "## Inspection loop"
started="$(date +%s%3N)"
"$repo_root/scripts/android-screen.sh" window --actionable >/dev/null
echo "- Active-window UIAutomator tree: $(elapsed_ms "$started") ms"

started="$(date +%s%3N)"
"$repo_root/scripts/android-screen.sh" ime --actionable >/dev/null
echo "- Direct Bubnilka IME tree: $(elapsed_ms "$started") ms"

started="$(date +%s%3N)"
"$repo_root/scripts/android-screen.sh" screenshot /tmp/bubnilka-benchmark.png >/dev/null
echo "- PNG capture: $(elapsed_ms "$started") ms"

echo
echo "## Process memory"
"$adb_bin" -s "$serial" shell dumpsys meminfo "$package" |
    awk '/TOTAL PSS:/ || /TOTAL RSS:/ {gsub("\\r", ""); print "- " $0}'
