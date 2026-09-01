# Host verification

Дата: 2026-09-01 UTC

## Результат

Команда `./scripts/verify.sh` завершилася успішно на build-host `ai-agent-hub`:

- secret scan: 98 файлів, збігів із credential-патернами немає;
- KMP/JVM: 17 тестів пройшли;
- Android host unit tests: 14 тестів пройшли;
- Android Lint (`lintDebug`): 0 issues;
- debug APK (`assembleDebug`): успішно;
- додаткова перевірка `compileReleaseKotlin`: успішно.

APK: `android-app/build/outputs/apk/debug/android-app-debug.apk`

- Розмір: приблизно 14.5 MB.
- SHA-256 на момент перевірки:
  `27100f80ee664db240fecbf3c2997bc5cb3478e435f230fb925c23590c272d88`.

## Межа доказів

Host-перевірка доводить компіляцію, deterministic workflow, prompt-контракти, privacy policy,
consent/versioning, error mapping і safe insertion через fake connection. Вона не доводить наявність
української on-device speech-моделі, реальну App Check атестацію, cloud latency чи сумісність
`InputConnection` у конкретному застосунку — це лишається device-only перевіркою.

Під час Android SDK tasks build-host друкує інформаційне попередження про SDK XML v4, яке походить
від поєднання поточного command-line tools із AGP 9.1.0; збірка, lint і тести від нього не залежать.
