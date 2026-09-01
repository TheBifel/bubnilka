#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
hooks_root="$(git -C "$repo_root" rev-parse --git-path hooks)"

mkdir -p "$hooks_root"
install -m 0755 "$repo_root/scripts/hooks/pre-commit" "$hooks_root/pre-commit"
install -m 0755 "$repo_root/scripts/hooks/pre-push" "$hooks_root/pre-push"

printf 'Installed pre-commit and pre-push hooks in %s\n' "$hooks_root"
