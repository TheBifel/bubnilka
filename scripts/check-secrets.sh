#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

for forbidden in firebase.local.properties google-services.json; do
    if git ls-files --error-unmatch "$forbidden" >/dev/null 2>&1 ||
        git ls-files | grep -Eq "(^|/)$forbidden$"; then
        printf 'Secret check failed: tracked local configuration: %s\n' "$forbidden" >&2
        exit 1
    fi
done

mapfile -t candidates < <(
    git ls-files --cached --others --exclude-standard |
        grep -Ev '(^|/)(build|\.gradle)/|firebase\.local\.properties\.example$' |
        grep -E '\.(kt|kts|java|xml|json|properties|md|yml|yaml|sh|toml)$' || true
)

if ((${#candidates[@]} == 0)); then
    exit 0
fi

patterns='-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|AIza[0-9A-Za-z_-]{30,}|sk-[A-Za-z0-9_-]{20,}|app.?check.{0,20}(debug.?token|secret)[[:space:]]*[:=][[:space:]]*[^[:space:]]{16,}'
if grep -EIl -- "$patterns" "${candidates[@]}" >/dev/null; then
    printf 'Secret check failed: a credential-like value was detected. Inspect changed files locally.\n' >&2
    exit 1
fi

printf 'Secret check passed (%d files scanned).\n' "${#candidates[@]}"
