#!/usr/bin/env bash
set -euo pipefail

toolchain_root="${BUBNILKA_TOOLCHAIN_ROOT:-/home/aiagent/.local/share/bubnilka-toolchain}"
android_home="${ANDROID_HOME:-$toolchain_root/android-sdk}"
adb_bin="$android_home/platform-tools/adb"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
serial="${ANDROID_SERIAL:-emulator-5554}"

usage() {
    echo "Usage: $0 tree|window|ime [--actionable] | screenshot [file.png] | tap X Y | text VALUE"
}

require_device() {
    "$adb_bin" -s "$serial" get-state >/dev/null
}

dump_active_window() {
    local filter="${1:-}"
    "$adb_bin" -s "$serial" shell uiautomator dump --compressed /sdcard/bubnilka-window.xml >/dev/null
    "$adb_bin" -s "$serial" exec-out cat /sdcard/bubnilka-window.xml |
        "$repo_root/scripts/android-ui-tree.py" $filter
}

dump_bubnilka_ime() {
    local filter="${1:-}"
    "$adb_bin" -s "$serial" shell am broadcast \
        -a com.thebifel.bubnilka.debug.DUMP_IME_UI \
        -n com.thebifel.bubnilka/.debug.UiDumpReceiver >/dev/null 2>&1 || return 0
    local tree
    tree="$($adb_bin -s "$serial" shell run-as com.thebifel.bubnilka \
        cat files/ime-ui-tree.txt 2>/dev/null || true)"
    [[ -n "$tree" ]] || return 0
    echo
    if [[ "$filter" == "--actionable" ]]; then
        printf '%s\n' "$tree" | awk '/^#/ || (/flags=visible/ && /clickable/)'
    else
        printf '%s\n' "$tree"
    fi
}

command="${1:-tree}"
shift || true
require_device

case "$command" in
    tree|window|ime)
        filter="${1:-}"
        if [[ -n "$filter" && "$filter" != "--actionable" ]]; then
            usage
            exit 2
        fi
        if [[ "$command" != "ime" ]]; then
            dump_active_window "$filter"
        fi
        if [[ "$command" != "window" ]]; then
            dump_bubnilka_ime "$filter"
        fi
        ;;
    screenshot)
        output="${1:-/tmp/bubnilka-screen.png}"
        "$adb_bin" -s "$serial" exec-out screencap -p >"$output"
        echo "$output"
        ;;
    tap)
        [[ $# -eq 2 ]] || { usage; exit 2; }
        "$adb_bin" -s "$serial" shell input tap "$1" "$2"
        ;;
    text)
        [[ $# -ge 1 ]] || { usage; exit 2; }
        "$adb_bin" -s "$serial" shell input text "$*"
        ;;
    *)
        usage
        exit 2
        ;;
esac
