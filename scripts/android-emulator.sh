#!/usr/bin/env bash
set -euo pipefail

toolchain_root="${BUBNILKA_TOOLCHAIN_ROOT:-/home/aiagent/.local/share/bubnilka-toolchain}"
android_home="${ANDROID_HOME:-$toolchain_root/android-sdk}"
adb_bin="$android_home/platform-tools/adb"
emulator_bin="$android_home/emulator/emulator"
avd_name="${BUBNILKA_AVD:-bubnilka_api35}"
serial="${ANDROID_SERIAL:-emulator-5554}"

usage() {
    echo "Usage: $0 start | wait | status | stop | install [apk]"
}

case "${1:-status}" in
    start)
        "$emulator_bin" -accel-check
        exec "$emulator_bin" "@$avd_name" \
            -no-window -no-audio -gpu swiftshader -no-boot-anim \
            -camera-back none -camera-front none -cores 2 \
            -partition-size 2048 -no-snapshot-load -no-snapshot-save
        ;;
    wait)
        "$adb_bin" -s "$serial" wait-for-device
        until [[ "$($adb_bin -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
            sleep 2
        done
        echo "$serial ready"
        ;;
    status)
        "$adb_bin" -s "$serial" get-state
        "$adb_bin" -s "$serial" shell getprop ro.build.version.release
        ;;
    stop)
        "$adb_bin" -s "$serial" emu kill
        ;;
    install)
        apk="${2:-android-app/build/outputs/apk/debug/android-app-debug.apk}"
        "$adb_bin" -s "$serial" install -r "$apk"
        ;;
    *)
        usage
        exit 2
        ;;
esac
