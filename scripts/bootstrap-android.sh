#!/usr/bin/env bash
set -euo pipefail

toolchain_root="${BUBNILKA_TOOLCHAIN_ROOT:-$HOME/.local/share/bubnilka-toolchain}"
jdk_root="$toolchain_root/jdk-17"
sdk_root="$toolchain_root/android-sdk"
command_line_tools_revision="15859902"

mkdir -p "$toolchain_root" "$sdk_root/cmdline-tools"

if [[ ! -x "$jdk_root/bin/java" ]]; then
    archive="$toolchain_root/temurin17.tar.gz"
    extract_root="$(mktemp -d "$toolchain_root/jdk.XXXXXX")"
    curl -fL --retry 3 \
        -o "$archive" \
        "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
    tar -xzf "$archive" -C "$extract_root"
    extracted_jdk="$(find "$extract_root" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
    mv "$extracted_jdk" "$jdk_root"
fi

if [[ ! -x "$sdk_root/cmdline-tools/latest/bin/sdkmanager" ]]; then
    archive="$toolchain_root/commandlinetools.zip"
    extract_root="$(mktemp -d "$toolchain_root/android-cli.XXXXXX")"
    curl -fL --retry 3 \
        -o "$archive" \
        "https://dl.google.com/android/repository/commandlinetools-linux-${command_line_tools_revision}_latest.zip"
    (
        cd "$extract_root"
        "$jdk_root/bin/jar" xf "$archive"
    )
    mv "$extract_root/cmdline-tools" "$sdk_root/cmdline-tools/latest"
    chmod u+x "$sdk_root/cmdline-tools/latest/bin/"*
fi

export JAVA_HOME="$jdk_root"
export ANDROID_HOME="$sdk_root"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

yes | "$sdk_root/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null || true
"$sdk_root/cmdline-tools/latest/bin/sdkmanager" \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0"

printf 'JAVA_HOME=%s\nANDROID_HOME=%s\n' "$JAVA_HOME" "$ANDROID_HOME"
