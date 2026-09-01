# Host verification

Дата: 2026-09-01 UTC

## Результат

Команда `./scripts/verify.sh` завершилася успішно на build-host `ai-agent-hub`:

- secret scan: 101 файл, збігів із credential-патернами немає;
- KMP/JVM: 17 тестів пройшли;
- Android host unit tests: 18 тестів пройшли, включно з local-first/system-fallback selection і
  Backspace через fake `InputConnection`;
- Android Lint (`lintDebug`): 0 issues;
- debug APK (`assembleDebug`): успішно;
- додаткова перевірка `compileReleaseKotlin`: успішно.

APK: `android-app/build/outputs/apk/debug/android-app-debug.apk`

- Розмір: 16 083 107 bytes.
- SHA-256 на момент перевірки:
  `e22de9653df4a9516e8ef9d697a50412be4f6d1a5d3f18dc869779376b09fee4`.
- APK Signature Scheme v2: перевірено `apksigner`.
- Опублікований артефакт `/tank/files/bubnilka-0.1.0-debug.apk` має ту саму SHA-256.

## Межа доказів

Host-перевірка доводить компіляцію, deterministic workflow, prompt-контракти, privacy policy,
consent/versioning, вибір local/system speech mode, error mapping, Backspace і safe insertion через
fake connection. Вона не доводить успішність системного cloud STT, фактичне завантаження
української on-device speech-моделі, реальну App Check атестацію, Gemini latency чи сумісність
`InputConnection` у конкретному застосунку — це лишається device-only перевіркою. Нова збірка
додає content-free `gemini_generate` timing для наступного device-проходу.

Під час Android SDK tasks build-host друкує інформаційне попередження про SDK XML v4, яке походить
від поєднання поточного command-line tools із AGP 9.1.0; збірка, lint і тести від нього не залежать.
