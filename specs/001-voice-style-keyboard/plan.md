# Implementation Plan: Voice-to-Styled-Text Keyboard

**Branch**: `main` (initial vertical slice) | **Date**: 2026-09-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-voice-style-keyboard/spec.md`

## Summary

Deliver an Android 12+ voice-first input method that prefers on-device speech recognition, falls
back to Android's system recognizer when the local service or language is missing, requests an
offline model for later use, sends the final transcript to the free Gemini Developer API through
Firebase AI Logic, and automatically commits the styled result to the focused field. A Kotlin
Multiplatform shared module owns styles, prompt construction, state transitions, validation, and
provider contracts. Android-only adapters own speech recognition, Firebase, preferences, and
`InputConnection`. No iOS target or runtime backend is built in the MVP.

## Technical Context

**Language/Version**: Kotlin 2.4.10; Java 17 toolchain

**Primary Dependencies**: Android Gradle Plugin 9.1.0, Gradle 9.3.1,
`com.android.kotlin.multiplatform.library`, Kotlin coroutines, Firebase Android BoM 34.18.0,
Firebase AI Logic 17.16.0, Firebase App Check 19.4.1, AndroidX Core/Activity/Lifecycle

**Storage**: Android preferences for consent, selected output language, whole-field mode, and the
ordered editable preset collection; transcripts and insertion records remain in memory

**Testing**: Kotlin `commonTest`, Android host/JVM unit tests, Android Lint, fake provider/connection
contract tests, manual Samsung Galaxy S21 smoke test

**Target Platform**: Android 12/API 31 or newer; initial device Samsung Galaxy S21 on Android 15

**Project Type**: Android IME application with a Kotlin Multiplatform shared library

**Performance Goals**: No partial insertion; visible state update within 200 ms of local callbacks;
cloud result committed within 10 seconds after recognition in 9/10 normal-network trials

**Constraints**: No payment method; no server/proxy; system cloud STT only as a disclosed fallback;
no content logging; no iOS build; no full alphabet layout; clean build must not require private
credentials

**Scale/Scope**: One private tester, five factory presets plus user-created presets, one active
session, messages up to 2,000 characters with an MVP UX optimized for typical messages up to 500
characters

## Constitution Check

*GATE: Passed before research and re-checked after design.*

- **Privacy by Construction**: PASS. System-recognizer fallback may send audio only after versioned
  disclosure consent; application content remains memory-only, content-free logs are enforced, and
  secure fields are blocked.
- **Shared Core, Native Edges**: PASS. Shared state and contracts contain no Android or Firebase
  types; all platform APIs are adapters.
- **Verifiable Changes**: PASS. Deterministic state/prompt behavior has host tests; build/lint/unit
  checks form the verification gate; device-only claims remain manual.
- **Free-Tier Cloud Boundary**: PASS. Firebase AI Logic uses Gemini Developer API on Spark with App
  Check; no billing or API key is embedded.
- **MVP Simplicity**: PASS. One Android app, one shared module, native XML/View IME, no accounts,
  backend, iOS, local LLM, QWERTY layout, or analytics.

Post-design review found no constitutional violations and requires no complexity exceptions.

## Project Structure

### Documentation (this feature)

```text
specs/001-voice-style-keyboard/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── cloud-transform-provider.md
│   ├── speech-to-text-provider.md
│   └── text-insertion-port.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
android-app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/thebifel/bubnilka/
    │   │   ├── BubnilkaApp.kt
    │   │   ├── MainActivity.kt
    │   │   ├── cloud/
    │   │   ├── ime/
    │   │   ├── speech/
    │   │   └── storage/
    │   └── res/
    └── test/

shared-core/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/com/thebifel/bubnilka/core/
    ├── commonTest/kotlin/com/thebifel/bubnilka/core/
    ├── androidMain/kotlin/
    └── androidHostTest/kotlin/

scripts/verify.sh
.github/workflows/ci.yml
```

**Structure Decision**: Use one Android application module and one KMP library module. The
application is kept outside the shared module because AGP 9 requires the dedicated Android-KMP
library plugin for shared Android code and a separate Android application entry point. Firebase and
IME code remain in the application module; shared code is independently host-testable.

## Complexity Tracking

No constitution violations require justification.
