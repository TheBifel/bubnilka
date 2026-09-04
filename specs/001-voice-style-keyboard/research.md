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

## Local-first speech recognition with system fallback

- **Decision**: Prefer `SpeechRecognizer.createOnDeviceSpeechRecognizer()`. If the on-device
  service is missing or rejects Ukrainian as unavailable, continue the same session with
  `createSpeechRecognizer()` and `EXTRA_PREFER_OFFLINE=false`. After a system-recognizer session,
  call `triggerModelDownload()` with `uk-UA` and offline preference so later sessions can migrate
  back to local recognition. Destroy recognizers with the IME lifecycle and reject stale callbacks
  with session and attempt identifiers.
- **Rationale**: Device testing showed the strict on-device prerequisite blocked the complete MVP.
  Android's default recognizer supplies the requested cloud-capable fallback, while the download
  request improves later offline availability without delaying the current dictation.
- **Privacy impact**: The system recognizer may send audio to its provider, so disclosure version 2
  explicitly covers audio fallback and forces renewed consent before voice capture.
- **Alternatives considered**: Requiring manual model installation was rejected because it blocks
  first use. Bundled Whisper remains outside the MVP due to package size and device benchmarking
  work.
- **Sources**: [Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer.html),
  [RecognizerIntent](https://developer.android.com/reference/android/speech/RecognizerIntent).

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

## Cloud latency diagnostics

- **Finding**: Each `generateContent()` invocation is an independent single-turn request. Pressing
  Regenerate re-sends the same raw transcript and original field snapshot; it does not continue a
  chat or provide the previous answer. A better regenerated answer is therefore normal model
  variance, not the model retaining a hidden warm-up example. A new dictation in combined mode is
  different: it includes the already cleaned field, which gives the model stronger context and can
  legitimately improve quality.
- **Latency finding**: The first request in a process can pay one-time client, TLS/network, and App
  Check token work. App Check attestation itself adds latency, tokens default to a one-hour TTL, and
  the SDK normally refreshes them around half-TTL. The selected `gemini-3.5-flash-lite` already
  defaults to `MINIMAL` thinking, so reducing thinking is not an available speed-up for this model.
  Current Gemini 3.x sampling controls are ignored, so temperature/top-p/top-k tuning is also not a
  valid latency or determinism fix.
- **Updated decision after device feedback**: Issue one minimal content-free warm-up generation when
  the first supported listening session starts in an application process. Run it concurrently with
  speech recognition, but make the first dictated transformation await its completion so the user
  request is the next model call. Ignore warm-up output and failure, never retry it automatically,
  and accept the cost of one additional free-tier request per process. Record
  `gemini_warmup`, `gemini_generate_first_user_process`, and
  `gemini_generate_repeat_user_process` separately. If warmed user requests still miss the target,
  next candidates are shorter output bounds or streaming for perceived latency.
- **Sources**: [Firebase streaming](https://firebase.google.com/docs/ai-logic/generate-text),
  [Firebase latency troubleshooting](https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting),
  [thinking configuration](https://firebase.google.com/docs/ai-logic/thinking),
  [App Check Play Integrity](https://firebase.google.com/docs/app-check/android/play-integrity-provider),
  [Gemini generateContent](https://ai.google.dev/api/generate-content), and
  [Firebase model parameters](https://firebase.google.com/docs/ai-logic/model-parameters).

## Loudness-reactive microphone feedback

- **Decision**: Normalize `RecognitionListener.onRmsChanged()` values to `0..1`, reject stale
  session callbacks in shared orchestration, and feed a custom lightweight Android `View` directly.
  Draw a subtle idle pulse while listening and three louder expanding rings as energy rises.
- **Rationale**: RMS callbacks can arrive many times per second. Keeping them outside
  `KeyboardState` avoids rebinding text, buttons, and scroll position at audio frequency while still
  making recording state and relative loudness immediately visible.
- **Alternatives considered**: A fixed blinking icon does not communicate input energy. Animated
  bitmap assets cannot follow live loudness cleanly and add package weight.

## Free-tier privacy and diagnostics

- **Decision**: Onboarding discloses cloud processing and free-tier data terms. Do not enable
  analytics or content-level AI monitoring. Application logs contain only state/error categories,
  never audio, transcripts, prompts, or model output.
- **Rationale**: A system keyboard receives sensitive material, and Google's free tier may use
  submitted data for product improvement.
- **Alternatives considered**: Full prompt/response monitoring was rejected despite debugging
  convenience.
