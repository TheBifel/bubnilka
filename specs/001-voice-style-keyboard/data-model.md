# Data Model: Voice-to-Styled-Text Keyboard

## StylePreset

- `id`: stable machine identifier
- `title`: editable user-visible label, 1..60 characters
- `instruction`: editable transformation intent, 1..4,000 characters
- `isDefault`: seed metadata retained when a factory preset is edited

Validation: IDs are unique and nonblank; title and instruction are trimmed and nonblank. The five
factory values seed Android local storage only when no stored list exists. Their IDs are retained on
edit; new presets receive generated IDs. Order and selected ID persist locally, and deletion may not
leave the collection empty.

## DictationSession

- `id`: monotonically unique value for stale-result rejection
- `selectedStyle`: `StylePreset`
- `selectedLanguageMode`: persistent Ukrainian or English output target
- `existingFieldText`: optional memory-only snapshot; present by default and absent when field
  combination is disabled
- `state`: one `KeyboardState`
- `partialTranscript`: transient text visible only while listening
- `rawTranscript`: transient final recognition result
- `lastResult`: optional `TransformationResult`
- `failure`: optional `KeyboardFailure`

State transitions:

```text
Idle -> Listening -> Transforming -> Inserted -> Idle
  |        |              |             |
  |        +-> Failure <- +             +-> Regenerating -> Inserted
  +-> Unsupported

Failure -> Listening | Transforming (retry) | Inserted (raw) | Idle
```

Only events carrying the current session ID may change state.

Recognizer RMS is mapped to a transient normalized `AudioLevel` event. It is routed directly to
the listening visualization, never persisted, and does not trigger a full `KeyboardState` render.

## KeyboardState

- `Idle(selectedStyle)`
- `Listening(sessionId, partialTranscript)`
- `Transforming(sessionId, rawTranscript, selectedStyle)`
- `Inserted(sessionId, rawTranscript, transformedText, selectedStyle)`
- `Failure(sessionId?, rawTranscript?, category, recoverableActions)`
- `SensitiveField`

## TransformationRequest

- `sessionId`
- `rawTranscript`: 1..2,000 trimmed characters
- `styleId`
- `styleInstruction`: immutable snapshot of the selected editable prompt for this request
- `sourceLanguageTag`: `auto` for normal sessions; an explicit supported tag remains valid for
  provider-level tests and compatibility
- `targetLanguageTag`: `uk-UA` or `en-US`
- `existingFieldText`: optional, at most 6,000 characters; present by default, while `null` means
  the user disabled field combination and requested transcript-only insertion

The request contains no application name, device identifier, or history. Host-field content is
included by default, can be disabled with a persisted preference, and is never persisted itself.

## TransformationResult

- `sessionId`
- `styleId`
- `text`: trimmed, nonempty, at most the configured result limit

Validation rejects mismatched session/style IDs, empty output, and obvious assistant preambles.

`InsertionRecord` keeps the optional pre-replacement field snapshot in memory so whole-field Undo
can restore it. Whole-field replacement is unconditional; the snapshot is cleared with the session
and is never persisted.

## InsertionRecord

- `sessionId`
- `rawTranscript`
- `insertedText`
- `styleId`

The record is memory-only and is cleared when the input connection changes or the IME session ends.

## ConsentPreference

- `cloudProcessingAccepted`: Boolean, default false
- `acceptedDisclosureVersion`: integer

No transcript or audio data is associated with this preference.

## KeyboardFailure

- `UnsupportedOnDeviceRecognition`
- `PermissionDenied`
- `NoSpeech`
- `RecognitionError`
- `CloudNotConfigured`
- `NetworkUnavailable`
- `QuotaExceeded`
- `InvalidCloudResponse`
- `UnsafeReplacement`
- `Cancelled`
- `Unexpected`

Each category maps to a stable user message and an explicit set of allowed recovery actions.
