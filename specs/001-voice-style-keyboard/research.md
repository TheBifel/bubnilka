# Research: Voice-to-Styled-Text Keyboard

## Kotlin and Android build stack

- **Decision**: Kotlin 2.4.10, AGP 9.1.0, Gradle 9.3.1, JDK 17, compile SDK 36, minimum SDK 31.
- **Rationale**: Kotlin 2.4.10 is the current stable release and officially supports AGP through
  9.1.0. AGP 9.1 requires Gradle 9.3.1 and JDK 17. API 31 is the first Android release with the
  explicit on-device speech recognizer factory required by the privacy boundary.
- **Alternatives considered**: AGP 9.3 was rejected because Kotlin 2.4.10's compatibility table
  supports AGP only through 9.1.x. AGP 8.x was rejected for a new project because it relies on the
  legacy KMP Android integration being phased out.
- **Sources**: [KMP compatibility](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html),
  [AGP 9.1.1](https://developer.android.com/build/releases/agp-9-1-0-release-notes),
  [Android-KMP plugin](https://developer.android.com/kotlin/multiplatform/plugin).

## KMP boundary

- **Decision**: `shared-core` uses the Kotlin Multiplatform and Android-KMP library plugins with
  common and Android/JVM host-test targets. `android-app` is a separate application module. No Apple
  targets or placeholder Swift/Xcode code exist in the MVP.
- **Rationale**: This creates genuinely reusable common code today without making Linux builds
  resolve or compile unused Apple targets. Adding iOS later is additive: introduce Apple targets
  and platform adapters while retaining the common contracts.
- **Alternatives considered**: A fake iOS target was rejected because it adds build risk without
  testing any iOS behavior. A single Android-only module was rejected because product logic would
  become coupled to Android types.

## Keyboard UI

- **Decision**: Use `InputMethodService` with native Android Views/XML and a small onboarding
  activity. The keyboard view is a vertical panel: styles, transcript/status, action row, and a
  globe/keyboard-switch control.
- **Rationale**: Native Views minimize startup, lifecycle, and dependency complexity in an IME.
- **Alternatives considered**: Compose inside the IME was rejected for the first slice because it
  adds view-tree/lifecycle setup and memory overhead without improving the core validation.

## Local speech recognition

- **Decision**: Use `SpeechRecognizer.isOnDeviceRecognitionAvailable()` and
  `createOnDeviceSpeechRecognizer()` only. Set Ukrainian as the initial language and expose clear
  unsupported/model-missing states. Destroy the recognizer with the IME lifecycle and reject stale
  callbacks using a session identifier.
- **Rationale**: The generic recognizer may use remote servers. The explicit on-device factory is
  the only path matching the constitution.
- **Alternatives considered**: Generic system speech recognition and bundled Whisper were rejected
  respectively for privacy ambiguity and MVP size/benchmark uncertainty.
- **Source**: [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer.html).

## Cloud transformation and authorization

- **Decision**: Use Firebase AI Logic with the Gemini Developer API and Firebase App Check. Pin the
  MVP to the stable free-tier `gemini-3.5-flash-lite` model. Debug builds use App Check's debug provider only
  when local Firebase configuration is present; a no-cloud implementation keeps clean CI builds
  functional and returns a configuration error at runtime.
- **Rationale**: Firebase AI Logic is designed for direct mobile access and keeps provider
  authorization on Google's gateway rather than embedding an LLM secret. The Spark/free tier needs
  no payment method.
- **Alternatives considered**: OpenAI BYOK was rejected for the first provider because OpenAI warns
  against mobile API-key storage. A custom backend and Proxmox proxy were explicitly excluded.
- **Sources**: [Firebase AI Logic](https://firebase.google.com/docs/ai-logic),
  [App Check](https://firebase.google.com/docs/ai-logic/app-check),
  [pricing](https://firebase.google.com/docs/ai-logic/pricing).

## Firebase build configuration

- **Decision**: Do not apply the Google Services Gradle plugin in the credential-free baseline.
  Load non-secret Firebase identifiers from an untracked `firebase.local.properties` and initialize
  `FirebaseApp` programmatically. If absent, the app still installs and every non-cloud flow works;
  cloud transformation reports `NotConfigured`.
- **Rationale**: A clean checkout and CI must build before a Firebase project exists, while the
  runtime must never contain a fabricated project identity.
- **Alternatives considered**: Committing `google-services.json` was rejected to keep project
  binding explicit and easily replaceable. A fake Firebase project was rejected because it makes
  runtime errors misleading.

## Text insertion safety

- **Decision**: Commit only the final transformed string. Undo/regeneration may delete and replace
  text only when the exact last insertion matches the immediate text before the cursor; otherwise
  retain host text and show a safe-replacement error.
- **Rationale**: Host applications may move the cursor or modify the field while a request is in
  flight. Exact suffix matching prevents deleting unrelated user content.
- **Alternatives considered**: Blind `deleteSurroundingText()` was rejected as destructive.

## Free-tier privacy and diagnostics

- **Decision**: Onboarding discloses cloud processing and free-tier data terms. Do not enable
  analytics or content-level AI monitoring. Application logs contain only state/error categories,
  never audio, transcripts, prompts, or model output.
- **Rationale**: A system keyboard receives sensitive material, and Google's free tier may use
  submitted data for product improvement.
- **Alternatives considered**: Full prompt/response monitoring was rejected despite debugging
  convenience.
