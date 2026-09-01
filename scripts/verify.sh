#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
toolchain_root="${BUBNILKA_TOOLCHAIN_ROOT:-$HOME/.local/share/bubnilka-toolchain}"

if [[ -z "${JAVA_HOME:-}" && -x "$toolchain_root/jdk-17/bin/java" ]]; then
    export JAVA_HOME="$toolchain_root/jdk-17"
fi
if [[ -z "${ANDROID_HOME:-}" && -d "$toolchain_root/android-sdk" ]]; then
    export ANDROID_HOME="$toolchain_root/android-sdk"
fi

cd "$repo_root"
./scripts/check-secrets.sh
./gradlew --no-daemon \
    :shared-core:jvmTest \
    :android-app:testDebugUnitTest \
    :android-app:lintDebug \
    :android-app:assembleDebug
