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

## Bilingual/context gate — 2026-09-03 UTC

Після додавання мовного меню, двостороннього перекладу, автоматичного слухання, scrollable preview
і режиму «Поле + голос» повний `./scripts/verify.sh` успішний:

- 26 KMP/JVM тестів пройшли, включно з обома напрямками перекладу, prompt isolation,
  whole-field replacement, Undo і захистом від конкурентної зміни поля;
- 19 Android unit tests пройшли, включно з exact full-field replacement через fake
  `InputConnection`;
- Android Lint: `No issues found`;
- debug APK зібрано, `compileReleaseKotlin` і release manifest processing успішні.

На Android 15 emulator перевірено автоматичний старт `speech_started_on_device`, persisted вибір
мови, bounds/стани всіх action-кнопок і наявність `ScrollView` для transcript preview. Screenshot
fallback підтвердив popup із `Українська`, `English`, `Українська → English`,
`English → Українська` і `Поле + голос`. Емулятор без реального аудіовходу закономірно завершує
диктовку помилкою `no speech`, тому end-to-end точність перекладу й Gemini latency лишаються
device-only перевіркою.

Фінальний APK цього проходу:

- Розмір: 16 083 939 bytes.
- SHA-256: `e857d6837ef7dd6c9fd07e48efd49ef6958ca54b02dc527aceead0c0b6e5ebd5`.
- Published: `/tank/files/bubnilka-0.1.0-debug.apk`.

## Unified-field and modular Backspace gate — 2026-09-04 UTC

Default whole-field mode тепер передає LLM весь snapshot поля разом із новою диктовкою та просить
повернути один цілісний текст із природними пробілами й переходами. Результат безумовно замінює
поточне поле; persisted-перемикач «Об’єднувати з полем» дозволяє повернутися до transcript-only
вставки в курсор.

Повний verification gate успішний:

- 29 KMP/JVM тестів, включно з default whole-field, вимкненим transcript-only режимом і
  коректним пробілом у raw fallback;
- 23 Android unit tests, зокрема видалення слова, апострофа/дефіса, пунктуації та пробілів;
- Android Lint: `No issues found`;
- debug APK, `compileReleaseKotlin` і release manifest processing успішні;
- APK Signature Scheme v2 перевірено.

На Android 15 emulator короткі tap послідовно змінили `lo world%21 → lo world% → lo world → lo`,
тобто видалили число як слово, один знак пунктуації та наступне слово. Long press за 1.5 секунди
видалив шість слів і зупинився після release. Direct IME tree підтвердив default-on стан об’єднання
з полем.

Фінальний APK цього проходу:

- Розмір: 16 083 939 bytes.
- SHA-256: `93156d5aaba97c72b86d1341183061b9979a3d615a60fc24eecedb9f9ba6e4ae`.
- Published: `/tank/files/bubnilka-0.1.0-debug.apk`.

## Output-language, Structured prompt, and Day/Night gate — 2026-09-04 UTC

The gear popup now persists only the target language (`Українська` or `English`). Every normal
session marks input language as auto-detected for Gemini, and Android 14+ recognition intents ask
the system service for language detection and balanced switching. Legacy `UA→EN`/`EN→UA`
preference IDs migrate to their former target language.

The Structured prompt now explicitly preserves the author's recognizable vocabulary, tone, and
speaking style, keeps deliberate foreign words in original spelling without transliteration, and
removes filler, meaningless repetition, unfinished rambling, and unrelated comments. Prompt tests
also distinguish ordinary foreign-language prose, which must still be translated to the target,
from deliberate foreign terms that must remain unchanged.

Full verification succeeded:

- 31 KMP/JVM tests passed, including target-language migration and prompt invariants;
- 24 Android unit tests passed, including the Android 14 language-detection capability boundary;
- Android Lint: `No issues found`;
- debug APK, `compileReleaseKotlin`, and release manifest processing succeeded;
- APK Signature Scheme v2 verified and the debug-only UI receiver is absent from release.

Android 15 emulator checks confirmed that the popup contains exactly `Українська`, `English`, and
`Об’єднувати з полем`; selecting English wrote `language_mode=english` and the IME accessibility
description immediately reflected it. PNG inspection covered light IME/popup plus dark onboarding,
IME, and popup. Backgrounds, text, chips, icons, controls, and system bars followed system night
mode without an app setting. Actual mixed-language speech accuracy remains device/provider-only
because the headless emulator has no real audio input and recognizers may ignore optional language
detection extras.

Final APK for this pass:

- Size: 16 083 939 bytes.
- SHA-256: `2a7ce71d879526391521344e0a7049dfca6ee1e75e1ef9ee1664678b9b7b04e3`.
- Published: `/tank/files/bubnilka-0.1.0-debug.apk`.

## Gemini warm-path diagnostics and microphone ripple — 2026-09-04 UTC

Code tracing confirmed that Regenerate is another independent `generateContent()` request with the
same raw transcript and original field snapshot; it is not a chat continuation and does not receive
the previous answer. A later dictation in combined mode is materially different because its field
snapshot can contain the previous cleaned result. Firebase timing now retains the content-free
`gemini_generate` measurement and additionally labels the first request in the application process
as `gemini_generate_first_process`; all later attempts are `gemini_generate_repeat_process`.

Speech recognizer RMS callbacks now produce normalized `0..1` audio-level events. Shared flow tests
prove that levels from stale session IDs are rejected. The Android IME routes active levels directly
to a custom ripple view, avoiding a complete keyboard render for every audio callback.

Full verification succeeded:

- 32 KMP/JVM tests passed, including active/stale audio-level routing;
- 25 Android unit tests passed, including finite/clamped RMS normalization;
- Android Lint: no issues;
- debug APK, `compileReleaseKotlin`, and release manifest processing succeeded;
- APK Signature Scheme v2 verified and the debug-only UI receiver is absent from release.

The final APK was installed on the Android 15 headless emulator. Direct IME inspection confirmed a
`100dp` ripple layer centered behind the `64dp` microphone without overlapping the keyboard switch,
settings, or Backspace controls. A PNG fallback confirmed the quiet listening pulse in the dark
system theme. The emulator automatically entered listening and then reported no speech, as expected
without an audio input; physical-device voice-amplitude response and real Firebase cold/warm timings
remain device-only checks.

Final APK for this pass:

- Size: 16 083 939 bytes.
- SHA-256: `d4b520adf1040175e50442697da6e12077bffc49af19d6f059e1d35507c05141`.
- Published: `/tank/files/bubnilka-0.1.0-debug.apk`.

## Proactive Gemini warm-up — 2026-09-04 UTC

The first supported listening session in an application process now starts one minimal,
content-free Gemini request immediately and in parallel with speech recognition. The first dictated
transformation awaits that attempt, making user content the next model call. Warm-up output is
discarded; warm-up failure is isolated from the keyboard state and never prevents the normal user
request or starts an automatic retry. This intentionally consumes one additional free-tier request
per application process.

Full verification succeeded:

- 34 KMP/JVM tests passed, including immediate/once-only warm-up, strict request ordering, and
  non-blocking warm-up failure;
- 25 Android unit tests passed;
- Android Lint: no issues;
- debug APK, `compileReleaseKotlin`, and release manifest processing succeeded;
- APK Signature Scheme v2 verified.

The final APK was installed on the Android 15 emulator. Content-free Logcat events showed
`gemini_warmup_started` at `20:52:47.924`, followed by `speech_started_on_device` at
`20:52:48.045`, proving warm-up begins at listening startup. The emulator's unregistered cloud test
context returned `ServerException` after 10,942 ms; the error remained diagnostic-only as required.
This is lifecycle evidence, not a valid Firebase latency benchmark. Physical-device logs must compare
`gemini_warmup`, `gemini_generate_first_user_process`, and
`gemini_generate_repeat_user_process`.

Final APK for this pass:

- Size: 16 083 939 bytes.
- SHA-256: `13e7a994da9278a71040ce6937994a54159a1a53b33dcbea6330f01b951ba00d`.
- Published: `/tank/files/bubnilka-0.1.0-debug.apk`.

## Editable style presets — 2026-09-05 UTC

Factory style presets are now seeded into local storage on first use and behave like ordinary
editable presets afterward. The pencil at the end of the keyboard strip opens the ordered preset
list; each row supports edit and confirmed delete, while the final `+` opens the same editor in
create mode. The editor validates a short name and a non-empty prompt, and the keyboard reloads the
persisted list and selection whenever it becomes visible. Deleting the selected preset selects the
first remaining item; deleting the final preset is intentionally blocked.

Full verification succeeded:

- 36 KMP/JVM tests passed, including injection and capture of an editable custom instruction;
- 28 Android unit tests passed, including initial factory seeding, edit/add ordering, selected-item
  fallback, and last-item protection;
- Android Lint completed successfully;
- debug APK, `compileReleaseKotlin`, and release manifest processing succeeded;
- the final APK was installed successfully on the Android 15 emulator.

Emulator interaction covered editing and persisting the Structured preset, adding a custom preset,
confirmed deletion, and returning to the IME to observe the refreshed chip label. Accessibility-tree
bounds also confirmed the list rows, trailing `+`, editor fields, top-right save action, and corrected
Android 15 status-bar inset.

Final APK for this pass:

- Size: 16 084 518 bytes.
- SHA-256: `cf0e3a8092d55d092bb370edcd96bc2f6ecbd562c0091afff13eaec839416490`.
- Published: `/tank/files/bubnilka-0.1.0-debug.apk`.

## Microphone visibility lifecycle — 2026-09-05 UTC

The IME now marks its input view visible before scheduling automatic listening and marks it hidden
before cancelling active recognition from `onFinishInputView`. The visibility guard prevents an
already queued automatic-start callback from reopening the microphone after collapse. The same
callback also stops Backspace repetition and resets the live audio level.

The full verification pipeline, release Kotlin compilation, and release manifest processing
succeeded. On the Android 15 emulator, AppOps reported `RECORD_AUDIO ... (running)` while the IME
was visible. One second after collapsing it with Back, AppOps showed a completed 11.846-second
recording; after another three seconds the duration remained unchanged, proving recognition did not
restart while the input view stayed hidden.

Final APK for this pass:

- Size: 16 084 449 bytes.
- SHA-256: `6d7fd0013eb54b91683c6ba93adcd2d9fe0fb6adee33a4744aaa4179c54941b5`.
- Published: `/tank/files/bubnilka-0.1.0-debug.apk`.
