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

The user opens a gear menu to select Ukrainian or English as the persistent output language.
Input language is detected automatically and ordinary prose is always translated to the selected
language. Combine with field is enabled by default: it gives the complete editor and new dictation
to the model, which returns one coherent text that replaces the entire field. The user can disable
it to transform and insert only the new dictation at the cursor.

**Acceptance Scenarios**:

1. **Given** English is selected, **When** Ukrainian or mixed-language speech is recognized,
   **Then** ordinary prose is English while deliberate foreign terms retain their original spelling.
2. **Given** Ukrainian is selected, **When** English or mixed-language speech is recognized,
   **Then** ordinary prose is Ukrainian with the same meaning.
3. **Given** Combine with field is enabled, **When** dictation completes, **Then** the complete field
   and additional transcript are sent as separate untrusted data sections and the model returns one
   coherent text with natural whitespace and transitions.
4. **Given** the host field changes after its snapshot, **When** the unified result arrives,
   **Then** it replaces the complete current editor without an exact-match check.
5. **Given** Combine with field is disabled, **When** dictation completes, **Then** no surrounding
   field text is sent and the transformed dictation is inserted at the cursor.

### Edge Cases

- The user stops recording without speaking or recognition returns an empty result.
- Recognition returns several alternatives or late partial results after the session was stopped.
- The keyboard service is recreated while recording or transformation is active.
- The network disconnects, times out, or returns a malformed/empty response.
- The transformed result contains leading commentary instead of only the requested text.
- The host application closes, changes selection, or changes fields before the result arrives.
- A transcript is longer than the cloud request limit.
- Field combination is enabled for an editor that cannot expose complete extracted text or exceeds
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
- **FR-019**: The keyboard MUST provide an always-available Backspace control through the active
  input connection.
- **FR-020**: Cloud transformation diagnostics MUST record request duration and failure category
  without recording prompts, transcripts, generated output, or other user content.
- **FR-021**: The gear popup MUST persist Ukrainian or English as the output language; input
  language MUST be auto-detected where the system recognizer supports it, and ordinary prose MUST
  always be translated to the selected output language.
- **FR-022**: After current disclosure consent, the keyboard MUST automatically start listening
  whenever it opens in a supported non-sensitive field.
- **FR-023**: The transcript preview MUST automatically follow new partial text to the bottom and
  MUST allow manual scrolling; auto-follow pauses while the user remains scrolled upward.
- **FR-024**: Structured MUST preserve the author's wording, tone, and speaking style where useful,
  remove filler, meaningless repetitions, unfinished rambling, and unrelated side comments, retain
  deliberate foreign words in their original spelling without transliteration, and add punctuation,
  paragraphs, and list structure where appropriate.
- **FR-025**: Combine with field MUST default to enabled, persist the user's toggle, and disclose
  complete field upload and unconditional whole-editor replacement.
- **FR-026**: In combined mode the prompt MUST ask the model to retain already suitable field text
  or format the field and dictation as one coherent result with natural whitespace and transitions.
- **FR-027**: When combined mode is disabled, the request MUST omit surrounding field content and
  insert only the transformed new dictation at the cursor.
- **FR-028**: A Backspace tap MUST delete a selection, one preceding word, or one punctuation mark;
  trailing whitespace belongs to the deleted module.
- **FR-029**: Holding Backspace MUST repeat modular deletion and accelerate from 130 ms to 90 ms
  and then 55 ms intervals until release or cancellation.
- **FR-030**: The app and IME MUST expose light and dark palettes selected solely by the current
  Android system night-mode configuration.
- **FR-031**: While speech recognition is active, the microphone MUST show a continuous ripple;
  its expansion and opacity MUST react to normalized recognizer loudness and disappear outside the
  listening state.
- **FR-032**: Cloud timing diagnostics MUST distinguish the first Gemini request in an application
  process from repeat requests without logging field text, transcripts, prompts, or responses.
- **FR-033**: When listening first starts in an application process, the cloud provider MUST begin
  one content-free warm-up generation immediately; the first dictated transformation MUST wait for
  that attempt to finish and MUST proceed normally if warm-up fails.
- **FR-034**: The style strip MUST end with an edit icon that opens a dedicated preset list; the
  list MUST show every preset with edit and delete actions and a final add item.
- **FR-035**: Add and edit MUST open a dedicated form with a name field, a remaining-height prompt
  field, and a top-right save checkmark; both trimmed fields are required.
- **FR-036**: The five built-in presets MUST seed local storage only once and thereafter behave like
  editable, deletable user data. At least one preset MUST remain available.
- **FR-037**: Every transformation and regeneration MUST capture the selected preset's current
  instruction in its request rather than reconstructing the instruction from a factory ID.
- **FR-038**: Preset names, instructions, order, and selected ID MUST persist locally. Disclosure
  MUST state that the selected instruction is sent to Gemini with the transformed content.
- **FR-039**: When the IME input view stops being visible, active speech recognition MUST be
  cancelled immediately and any queued automatic-listening callback MUST NOT restart it while the
  keyboard remains hidden.

### Key Entities

- **Style preset**: A stable identifier, editable user-facing title, editable transformation
  instruction, and local ordering. Factory presets are initial values rather than immutable types.
- **Language mode**: Persistent Ukrainian or English output target and recognition fallback locale.
- **Dictation session**: The active session identity, selected style, partial/final transcript,
  lifecycle state, and recoverable failure if any.
- **Transformation result**: The returned text and the session/style identifiers that prove it
  belongs to the active request.
- **Insertion record**: The last inserted text, original transcript, and optional pre-dictation field
  snapshot needed for undo or regeneration; it exists in memory only.
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
- **SC-008**: Listening state is visually identifiable without reading status text, and louder
  physical-device speech visibly produces stronger microphone waves than silence.

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
