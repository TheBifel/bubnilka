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

## Emulator/UI gate — 2026-09-02 UTC

На KVM-прискореному `bubnilka_api35` (Android 15, x86_64, headless SwiftShader) перевірено:

- debug APK встановлено, Бубнілку ввімкнено та вибрано як поточну IME;
- direct IME tree повертає bounds, centers, IDs, видимість та selected/enabled/clickable стани;
- tap за координатами з дерева перемкнув `Структуровано → Коротко`, selected state підтверджено;
- введення `abc` і tap по Backspace змінили host field на `ab`;
- screenshot fallback підтвердив відсутність перекриття action-кнопок системною navigation bar;
- direct IME dump: 65 ms; combined UIAutomator tree: 2.4–3.5 s; PNG capture: 0.28–0.29 s;
- cold onboarding Activity: 1906/3106/3204 ms; process після launch: 47 MB PSS, 143 MB RSS;
- IME process під час keyboard flow: 65 MB PSS, 168 MB RSS.

`dumpsys gfxinfo` після серії швидких ADB taps показав 48 ms p50 і 93.75% janky frames. Це
очікуване обмеження SwiftShader/N100 і не використовується як оцінка фізичного Galaxy S21. Так само
emulator gate не вимірює Gemini: без окремо зареєстрованого emulator App Check token cloud timing
не буде валідним. Для нього в APK залишається content-free `gemini_generate` telemetry.

Після UI змін повний `./scripts/verify.sh` успішний: 17 KMP/JVM тестів, 18 Android unit tests,
Android Lint — 0 issues, debug APK зібрано; `compileReleaseKotlin` також успішний. Debug receiver
захищений системним `android.permission.DUMP` і відсутній у release manifest.

Фінальний APK:

- Розмір: 16 083 865 bytes.
- SHA-256: `42e75b6229ba649087ea6d2aa4b3377c037b1e22221c4c1f0d58f58036ab2599`.
- Published: `/tank/files/bubnilka-0.1.0-debug.apk`.
