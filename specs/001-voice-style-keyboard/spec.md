# Feature Specification: Voice-to-Styled-Text Keyboard

**Feature Branch**: `main`

**Created**: 2026-09-01

**Status**: Ready for planning

**Input**: User description: "Build the Android MVP of Bubnilka: a voice-first keyboard that
prefers local Ukrainian or English speech transcription with an automatic system/cloud fallback,
optionally translates in either direction, transforms the transcript in a selected cloud style,
and inserts the result into the active text field. Ask the
system recognizer to download an offline model for future sessions. Keep the product future-ready
for shared mobile logic, but do not build or compile iOS. The installed app must not depend on the
development server and must stay within a no-payment cloud tier."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dictate and Insert Styled Text (Priority: P1)

After enabling Bubnilka as a keyboard, the user opens any supported text field and the keyboard
automatically starts listening. The user dictates a message and receives a polished version
inserted into the field without manually copying or pasting.

**Why this priority**: This is the complete value proposition and the smallest useful product
slice.

**Independent Test**: Enable the keyboard on the target phone, dictate one Ukrainian message in a
supported app, and verify that the transformed result appears at the cursor.

**Acceptance Scenarios**:

1. **Given** Bubnilka is enabled, disclosure consent is current, and a regular field is focused,
   **When** the keyboard opens, **Then** it starts listening without an extra microphone tap,
   displays recognized text
   while it is being prepared and inserts only the final transformed text into the field.
2. **Given** a transformation is in progress, **When** the cloud result has not arrived,
   **Then** no partial or raw transcript is inserted into the host field.
3. **Given** the user has selected no explicit style, **When** a transcript is transformed,
   **Then** the Structured style is applied by default.
4. **Given** a long partial transcript, **When** new words arrive, **Then** preview follows the
   bottom automatically unless the user has manually scrolled upward.

---

### User Story 2 - Choose and Reapply a Style (Priority: P2)

The user can choose how the dictated message should be rewritten and can regenerate the most
recent result without recording the message again.

**Why this priority**: Style control differentiates Bubnilka from ordinary voice typing.

**Independent Test**: Transform the same transcript with two different styles and verify that each
result follows the selected style while retaining the original facts and language.

**Acceptance Scenarios**:

1. **Given** the keyboard is idle, **When** the user chooses Structured, Concise, Formal, Friendly,
   or Viking and dictates text, **Then** the selected style is used for the transformation.
2. **Given** the last transformed text is still unchanged immediately before the cursor, **When**
   the user selects another style and regenerates, **Then** the prior result is replaced by the new
   result.
3. **Given** the host field changed after insertion, **When** replacement can no longer be proven
   safe, **Then** Bubnilka does not delete host text and explains that regeneration cannot replace
   the previous result automatically.

---

### User Story 3 - Recover From Recognition or Cloud Failure (Priority: P3)

The user receives a clear, recoverable state when speech recognition is unavailable, the network
fails, or the free cloud quota is exhausted.

**Why this priority**: A keyboard must never silently lose a dictated message or appear frozen.

**Independent Test**: Simulate each failure category and verify that the user can retry, insert the
raw transcript when one exists, or return to idle without restarting the keyboard.

**Acceptance Scenarios**:

1. **Given** the device lacks compatible on-device recognition, **When** the user taps the
   microphone, **Then** the keyboard uses the disclosed system recognizer fallback and requests an
   offline Ukrainian model for later sessions without blocking the current dictation.
2. **Given** a final raw transcript exists but cloud transformation fails, **When** the error is
   shown, **Then** the user can retry or insert the raw transcript.
3. **Given** the cloud quota is exhausted, **When** transformation fails, **Then** the keyboard
   identifies the temporary limit, avoids automatic retry loops, and preserves the raw transcript.

---

### User Story 4 - Understand and Control Data Use (Priority: P4)

Before the first cloud request, the user sees what leaves the phone and explicitly agrees. The
voice action is unavailable in sensitive fields.

**Why this priority**: Trust is essential for a system-wide keyboard, even in a personal MVP.

**Independent Test**: Complete onboarding, inspect the disclosure, and verify behavior in regular
and secure fields without examining implementation details.

**Acceptance Scenarios**:

1. **Given** current disclosure consent has not been granted, **When** the user first attempts
   dictation, **Then** the app explains that fallback speech recognition may send audio through the
   system provider and that the final transcript is sent to Gemini, then requires an affirmative
   choice before proceeding.
2. **Given** a password or other secure field is focused, **When** Bubnilka becomes active,
   **Then** microphone and cloud actions are disabled and no dictated content is captured.

---

### User Story 5 - Select Language, Translate, and Extend a Field (Priority: P2)

The user opens a gear menu to select Ukrainian, English, Ukrainian-to-English, or
English-to-Ukrainian. An explicit Field + voice option combines a snapshot of the entire editor
with new dictation and replaces the whole field only if it is still unchanged.

**Acceptance Scenarios**:

1. **Given** UA→EN is selected, **When** Ukrainian speech is recognized, **Then** the result is
   English and retains every name, number, fact, nuance, and intent.
2. **Given** EN→UA is selected, **When** English speech is recognized, **Then** the result is
   Ukrainian with the same meaning.
3. **Given** Field + voice is enabled, **When** dictation completes, **Then** the complete field and
   additional transcript are sent as separate untrusted data sections and the returned complete
   text safely replaces the field.
4. **Given** the host field changes after its snapshot, **When** the result arrives, **Then** no
   replacement occurs and an unsafe-replacement failure is shown.

### Edge Cases

- The user stops recording without speaking or recognition returns an empty result.
- Recognition returns several alternatives or late partial results after the session was stopped.
- The keyboard service is recreated while recording or transformation is active.
- The network disconnects, times out, or returns a malformed/empty response.
- The transformed result contains leading commentary instead of only the requested text.
- The host application closes, changes selection, or changes fields before the result arrives.
- A transcript is longer than the cloud request limit.
- Field + voice is selected for an editor that cannot expose complete extracted text or exceeds
  the 6,000-character context limit.
- The user taps the microphone or regenerate button repeatedly while work is already active.
- The selected style changes while an earlier request is still running.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The product MUST provide onboarding that explains how to enable and select Bubnilka as
  a system keyboard.
- **FR-002**: The keyboard MUST expose microphone, style selection, status, and keyboard-switch
  controls in every supported non-sensitive text field.
- **FR-003**: The keyboard MUST prefer the explicit on-device recognizer, MUST automatically fall
  back to the system recognizer when the local service or language is unavailable, and MUST request
  offline-model download for later sessions on supported Android versions.
- **FR-004**: Partial and final raw transcripts MUST remain inside the keyboard until the user
  inserts raw text or a successful transformation is ready.
- **FR-005**: The keyboard MUST provide Structured, Concise, Formal, Friendly, and Viking styles,
  with Structured selected by default.
- **FR-006**: A cloud transformation MUST use the selected output language and preserve names,
  numbers, links, factual meaning, nuances, and intent while returning only the rewritten text.
- **FR-007**: On successful transformation, the keyboard MUST insert the complete result at the
  current cursor position without a copy/paste step.
- **FR-008**: The keyboard MUST allow safe undo of its latest insertion when the inserted text is
  still unchanged immediately before the cursor.
- **FR-009**: The keyboard MUST allow regeneration from the most recent raw transcript and MUST
  replace prior output only when safe replacement can be proven.
- **FR-010**: When a raw transcript exists and cloud processing fails, the keyboard MUST offer
  Retry and Insert raw actions.
- **FR-011**: The keyboard MUST distinguish recognition, connectivity, quota, invalid-response,
  cancellation, and unsupported-device failures in user-facing states.
- **FR-012**: The product MUST disclose both possible system/cloud audio recognition and Gemini
  transcript processing, require versioned consent before voice capture, and make consent revocable
  in settings.
- **FR-013**: Voice capture and cloud transformation MUST be disabled for secure input fields.
- **FR-014**: The product MUST NOT persist audio or transcript content and MUST NOT include content
  in diagnostic logs.
- **FR-015**: The product MUST operate without login, registration, a payment method, or access to
  the development host.
- **FR-016**: Cloud usage MUST stay on a no-payment tier; quota exhaustion MUST fail closed without
  incurring a charge or starting automatic retry loops.
- **FR-017**: Only one recording or transformation session may be active; starting a new session
  MUST cancel or supersede stale work so stale results cannot be inserted.
- **FR-018**: The product MUST provide a visible way to switch to another enabled keyboard.
- **FR-019**: The keyboard MUST provide an always-available Backspace control that deletes the
  current selection or one Unicode code point before the cursor through the active input connection.
- **FR-020**: Cloud transformation diagnostics MUST record request duration and failure category
  without recording prompts, transcripts, generated output, or other user content.
- **FR-021**: The keyboard MUST support Ukrainian and English recognition plus UA→EN and EN→UA
  translation modes selected from a gear popup.
- **FR-022**: After current disclosure consent, the keyboard MUST automatically start listening
  whenever it opens in a supported non-sensitive field.
- **FR-023**: The transcript preview MUST automatically follow new partial text to the bottom and
  MUST allow manual scrolling; auto-follow pauses while the user remains scrolled upward.
- **FR-024**: Structured MUST preserve the author's wording, tone, length, and order, applying only
  punctuation, paragraphs, and numbering/list markers already implied by the content.
- **FR-025**: Field + voice MUST be explicit, MUST disclose complete field upload, and MUST replace
  the whole editor only after an exact unchanged-text check.

### Key Entities

- **Style preset**: A stable style identifier, user-facing title, and transformation intent.
- **Language mode**: Recognition and output language pair for monolingual or translation flows.
- **Dictation session**: The active session identity, selected style, partial/final transcript,
  lifecycle state, and recoverable failure if any.
- **Transformation result**: The returned text and the session/style identifiers that prove it
  belongs to the active request.
- **Insertion record**: The exact last inserted text and original transcript needed for safe undo
  or regeneration; it exists in memory only.
- **Consent preference**: Whether the user has acknowledged cloud processing and free-tier data
  handling; it contains no dictated content.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On the target phone, the user can go from tapping the microphone to seeing a styled
  message in a regular text field in one uninterrupted flow, without copy/paste.
- **SC-002**: For a typical message of up to 500 characters, the transformed text appears within
  10 seconds after speech recognition completes under a working network connection in at least 9 of
  10 manual trials.
- **SC-003**: In fixed Ukrainian and English corpora, monolingual and bidirectional translation
  results retain all names, numbers, links, meaning, and contain no assistant preamble.
- **SC-004**: All simulated recognition and cloud failure categories recover to an actionable
  state within 2 seconds and never insert partial text automatically.
- **SC-005**: Automated tests cover every deterministic state transition and style prompt rule,
  and the project produces a debug installation package from a clean checkout.
- **SC-006**: A manual check in at least five common text-entry applications confirms insertion,
  keyboard switching, and safe behavior, with any app-specific limitation recorded.
- **SC-007**: No payment method, server dependency, dictated-content log, or committed credential is
  required to complete the successful MVP flow.

## Assumptions

- The first tester uses a Samsung Galaxy S21 running Android 15 with internet access for cloud
  transformation.
- Android's configured speech service supports Ukrainian online or offline; model download may be
  scheduled or require system-provided confirmation.
- The free cloud tier is capacity-limited and may temporarily reject requests; temporary
  unavailability is acceptable if the raw transcript is preserved.
- The free cloud tier may process submitted text under provider data-improvement terms, and the MVP
  user explicitly accepts this during onboarding.
- The MVP is distributed privately as a debug APK and is not submitted to an app store.
- Full letter layouts, autocorrection, suggestions, accounts, iOS, analytics, and bundled language
  models are outside this feature.
