# Data Model: Voice-to-Styled-Text Keyboard

## StylePreset

- `id`: stable machine identifier
- `title`: user-visible Ukrainian label
- `instruction`: transformation intent used by prompt construction
- `isDefault`: exactly one preset is the default

Validation: IDs are unique; title and instruction are nonblank; Structured is the sole default.

## DictationSession

- `id`: monotonically unique value for stale-result rejection
- `selectedStyle`: `StylePreset`
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
- `languageTag`: initially `uk-UA`

The request contains no host-field context, cursor context, application name, or device identifier.

## TransformationResult

- `sessionId`
- `styleId`
- `text`: trimmed, nonempty, at most the configured result limit

Validation rejects mismatched session/style IDs, empty output, and obvious assistant preambles.

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
