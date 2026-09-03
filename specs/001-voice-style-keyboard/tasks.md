# Tasks: Voice-to-Styled-Text Keyboard

**Input**: Design documents from `specs/001-voice-style-keyboard/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Required by the constitution and feature success criteria. Story tests are written before
their implementations and must fail for the intended reason.

**Organization**: Tasks are grouped by user story so each story can be validated as a coherent
increment.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Reproducible KMP/Android build, documentation, and verification entry points.

- [X] T001 Create Gradle 9.3.1 wrapper and root build configuration in `gradlew`,
  `gradle/wrapper/`, `settings.gradle.kts`, `build.gradle.kts`, and `gradle/libs.versions.toml`
- [X] T002 Create Android application and KMP module build definitions in
  `android-app/build.gradle.kts` and `shared-core/build.gradle.kts`
- [X] T003 [P] Add repository hygiene and local Firebase templates in `.gitignore`,
  `.editorconfig`, and `firebase.local.properties.example`
- [X] T004 [P] Add build-host bootstrap and verification commands in `scripts/bootstrap-android.sh`
  and `scripts/verify.sh`
- [X] T005 [P] Add project overview, privacy boundary, and device-feedback workflow in `README.md`
  and `docs/device-feedback.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared contracts and Android shells required by every user story.

**Critical**: No user story work starts until this phase passes shared tests and assembles a shell
APK.

- [X] T006 [P] Add shared style, request, result, failure, and state models in
  `shared-core/src/commonMain/kotlin/com/thebifel/bubnilka/core/model/`
- [X] T007 [P] Add provider and insertion contracts in
  `shared-core/src/commonMain/kotlin/com/thebifel/bubnilka/core/port/`
- [X] T008 Add shared session reducer and stale-event rules in
  `shared-core/src/commonMain/kotlin/com/thebifel/bubnilka/core/session/KeyboardSession.kt`
- [X] T009 [P] Add baseline shared reducer/model tests in
  `shared-core/src/commonTest/kotlin/com/thebifel/bubnilka/core/session/KeyboardSessionTest.kt`
- [X] T010 Create application, activity, and IME declarations in
  `android-app/src/main/AndroidManifest.xml`, `BubnilkaApp.kt`, `MainActivity.kt`, and
  `BubnilkaInputMethodService.kt`
- [X] T011 [P] Create baseline resources and voice-first keyboard layout in
  `android-app/src/main/res/`
- [X] T012 Add safe structured logging and coroutine lifecycle ownership in
  `android-app/src/main/java/com/thebifel/bubnilka/runtime/`

**Checkpoint**: Shared tests pass and a credential-free shell APK assembles.

---

## Phase 3: User Story 1 - Dictate and Insert Styled Text (Priority: P1) MVP

**Goal**: Complete the local-recording to cloud-transform to automatic-insertion flow.

**Independent Test**: On the target phone, enable Bubnilka, dictate one Ukrainian message in a
regular field, and observe only the final transformed text inserted at the cursor.

### Tests for User Story 1

- [X] T013 [P] [US1] Add prompt builder tests for language/fact preservation and output-only rules
  in `shared-core/src/commonTest/kotlin/com/thebifel/bubnilka/core/prompt/PromptBuilderTest.kt`
- [X] T014 [P] [US1] Add P1 session transition tests with fake speech/cloud/insertion ports in
  `shared-core/src/commonTest/kotlin/com/thebifel/bubnilka/core/session/VoiceFlowTest.kt`
- [X] T015 [P] [US1] Add sensitive-field and input-connection adapter unit tests in
  `android-app/src/test/java/com/thebifel/bubnilka/ime/`

### Implementation for User Story 1

- [X] T016 [US1] Implement invariant prompt construction in
  `shared-core/src/commonMain/kotlin/com/thebifel/bubnilka/core/prompt/PromptBuilder.kt`
- [X] T017 [US1] Implement local-first Android speech adapter in
  `android-app/src/main/java/com/thebifel/bubnilka/speech/AndroidSpeechToTextProvider.kt`
- [X] T018 [US1] Implement local Firebase property loading and runtime initialization in
  `android-app/src/main/java/com/thebifel/bubnilka/cloud/FirebaseConfiguration.kt`
- [X] T019 [US1] Implement Firebase AI Logic/App Check provider and credential-free fallback in
  `android-app/src/main/java/com/thebifel/bubnilka/cloud/`
- [X] T020 [US1] Implement safe `InputConnection` insertion adapter in
  `android-app/src/main/java/com/thebifel/bubnilka/ime/AndroidTextInsertionPort.kt`
- [X] T021 [US1] Wire microphone, transcript/status rendering, transform, and auto-insert in
  `android-app/src/main/java/com/thebifel/bubnilka/ime/BubnilkaInputMethodService.kt`

**Checkpoint**: P1 works end-to-end with fakes on the host and is ready for the first device test.

---

## Phase 4: User Story 2 - Choose and Reapply a Style (Priority: P2)

**Goal**: Select five styles and safely undo or regenerate the most recent insertion.

**Independent Test**: Transform one stored transcript with two styles, safely replace unchanged
output, and refuse replacement after the host field changes.

### Tests for User Story 2

- [X] T022 [P] [US2] Add all-style prompt snapshot/contract tests in
  `shared-core/src/commonTest/kotlin/com/thebifel/bubnilka/core/prompt/StylePromptTest.kt`
- [X] T023 [P] [US2] Add undo/regenerate safe replacement tests in
  `shared-core/src/commonTest/kotlin/com/thebifel/bubnilka/core/session/RegenerationTest.kt`

### Implementation for User Story 2

- [X] T024 [US2] Implement five style presets and default selection in
  `shared-core/src/commonMain/kotlin/com/thebifel/bubnilka/core/model/StylePreset.kt`
- [X] T025 [US2] Implement memory-only insertion record, undo, and regeneration transitions in
  `shared-core/src/commonMain/kotlin/com/thebifel/bubnilka/core/session/KeyboardSession.kt`
- [X] T026 [US2] Wire style buttons, Undo, and Regenerate actions in
  `android-app/src/main/java/com/thebifel/bubnilka/ime/BubnilkaInputMethodService.kt`

**Checkpoint**: P1 and P2 flows are independently host-tested and ready for device feedback.

---

## Phase 5: User Story 3 - Recover From Recognition or Cloud Failure (Priority: P3)

**Goal**: Preserve the raw transcript and present actionable, non-looping failures.

**Independent Test**: Simulate unsupported recognition, offline, quota, timeout, invalid response,
and cancellation; verify Retry/Insert raw/Idle actions and no automatic partial insertion.

### Tests for User Story 3

- [X] T027 [P] [US3] Add failure mapping and recovery transition tests in
  `shared-core/src/commonTest/kotlin/com/thebifel/bubnilka/core/session/FailureRecoveryTest.kt`
- [X] T028 [P] [US3] Add Android speech/Firebase error mapping tests in
  `android-app/src/test/java/com/thebifel/bubnilka/runtime/ErrorMappingTest.kt`

### Implementation for User Story 3

- [X] T029 [US3] Implement typed error mapping without content logs in
  `android-app/src/main/java/com/thebifel/bubnilka/runtime/AndroidFailureMapper.kt`
- [X] T030 [US3] Add Retry, Insert raw, quota, unsupported-model, and cancellation UI states in
  `android-app/src/main/java/com/thebifel/bubnilka/ime/BubnilkaInputMethodService.kt`

**Checkpoint**: Every specified failure is actionable and raw text is never lost or auto-inserted.

---

## Phase 6: User Story 4 - Understand and Control Data Use (Priority: P4)

**Goal**: Obtain revocable cloud consent and disable voice/cloud in sensitive fields.

**Independent Test**: Verify first-use disclosure and preference revocation, then focus a password
field and confirm no voice or cloud action can begin.

### Tests for User Story 4

- [X] T031 [P] [US4] Add consent preference and disclosure-version tests in
  `android-app/src/test/java/com/thebifel/bubnilka/storage/ConsentRepositoryTest.kt`
- [X] T032 [P] [US4] Add comprehensive secure-input classification tests in
  `android-app/src/test/java/com/thebifel/bubnilka/ime/SensitiveFieldPolicyTest.kt`

### Implementation for User Story 4

- [X] T033 [US4] Implement consent storage and revocation in
  `android-app/src/main/java/com/thebifel/bubnilka/storage/ConsentRepository.kt`
- [X] T034 [US4] Implement onboarding disclosure and keyboard setup controls in
  `android-app/src/main/java/com/thebifel/bubnilka/MainActivity.kt`
- [X] T035 [US4] Enforce sensitive-field and consent gates in
  `android-app/src/main/java/com/thebifel/bubnilka/ime/BubnilkaInputMethodService.kt`

**Checkpoint**: Privacy acceptance scenarios pass without dictated-content persistence.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Make the complete MVP reproducible and ready for the user's device loop.

- [X] T036 [P] Configure CI verification in `.github/workflows/ci.yml`
- [X] T037 [P] Add secret-pattern checks and local hook installers in `scripts/check-secrets.sh` and
  `scripts/install-git-hooks.sh`
- [X] T038 Add app accessibility labels, rotation/resizing resilience, and Ukrainian strings in
  `android-app/src/main/res/`
- [X] T039 Run `scripts/verify.sh`, repair all failures, and record host evidence in
  `docs/verification.md`
- [X] T040 Review `specs/001-voice-style-keyboard/quickstart.md`, package the debug APK, and record
  remaining device-only checks in `docs/device-feedback.md`

---

## Phase 8: Device Feedback - Speech Fallback

**Purpose**: Remove the missing-offline-model blocker discovered on the first physical device.

- [X] T041 Add host tests for local-first mode selection and system fallback in
  `android-app/src/test/java/com/thebifel/bubnilka/speech/SpeechRecognitionModeTest.kt`
- [X] T042 Replace the strict local adapter with local-first/system fallback and automatic `uk-UA`
  model download in `android-app/src/main/java/com/thebifel/bubnilka/speech/`
- [X] T043 Update disclosure versioning, user-facing copy, specification, contract, research, and
  quickstart for the expanded audio-processing boundary
- [X] T044 Run the complete verification gate, publish the configured APK, and record artifact
  evidence in `docs/verification.md`

---

## Phase 9: Device Feedback - Editing, Latency, and Visual Validation

- [X] T045 Add a host test and an always-visible Backspace control using the active
  `InputConnection`.
- [X] T046 Add content-free Gemini request timing and a device command for collecting cold/warm
  latency samples.
- [X] T047 Inspect Proxmox/LXC emulator prerequisites and document the KVM-enabled headless test
  path in `docs/emulator-capability.md`.
- [X] T048 Run the complete verification gate and publish the new APK with device-safe latency
  instrumentation; device samples remain the input to the next optimization decision.

---

## Phase 10: Headless Emulator and Keyboard UI Feedback

- [X] T049 Hot-plug KVM into the build LXC, install API 35 emulator tooling, and create a reusable
  headless AVD launcher.
- [X] T050 Add an ADB/UIAutomator active-window inspector with exact view bounds and actions.
- [X] T051 Add a debug-only direct IME inspector because UIAutomator omits the keyboard window.
- [X] T052 Replace native gray controls with stateful style chips, vector action icons, a transcript
  panel, contextual recovery actions, and navigation-bar-safe spacing.
- [X] T053 Validate style selection and `abc → ab` Backspace behavior on the emulator, run host and
  UI performance checks, and publish the verified APK.

---

## Phase 11: Bilingual Translation, Live Preview, and Whole-Field Context

- [X] T054 Add tested UA, EN, UA→EN, and EN→UA language-mode contracts and prompt invariants.
- [X] T055 Add bounded whole-field snapshot/replace/undo operations with exact concurrent-edit
  protection and explicit privacy disclosure version 3.
- [X] T056 Add a gear popup for language/translation and Field + voice selection.
- [X] T057 Start recognition automatically when the consented keyboard opens outside secure fields.
- [X] T058 Make transcript preview auto-follow new partials while retaining manual scroll control.
- [X] T059 Change Structured to preserve original wording and apply only light document structure.
- [X] T060 Run full verification, emulator smoke tests, publish APK, commit, and push.

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup has no dependencies.
- Foundational depends on Setup and blocks every story.
- US1 depends on Foundational.
- US2 uses the P1 insertion flow but is independently testable with fakes after Foundational.
- US3 exercises the P1 provider flow and follows US1 in the single-developer sequence.
- US4 can be developed after Foundational but must gate the final integrated P1 flow.
- Polish depends on all selected stories.

### Parallel Opportunities

- T003-T005 can proceed independently after the repository exists.
- T006, T007, T009, and T011 affect separate files.
- Test tasks within each story target separate files and can be prepared together.
- T036 and T037 are independent of UI polish.

## Parallel Example: User Story 1

```text
T013: prompt contract tests
T014: shared state-flow tests
T015: Android policy/connection tests
```

These tests touch separate files and can be authored before T016-T021.

## Implementation Strategy

1. Complete Setup and Foundational, producing the first shell APK.
2. Complete US1 and stop for the first real-device voice/cloud/insertion test.
3. Add US2 and US3 while retaining P1 behavior.
4. Add US4 gates before sharing the APK beyond the initial tester.
5. Run the complete verification gate before commit and again before push.

## Format Validation

All 40 tasks use the required checkbox, sequential task ID, optional parallel marker, user-story
label for story phases, and an explicit repository path.
