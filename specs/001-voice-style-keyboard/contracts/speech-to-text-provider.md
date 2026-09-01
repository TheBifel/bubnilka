# Contract: SpeechToTextProvider

## Purpose

Produce partial and final text from one explicitly initiated on-device recording session.

## Events

- Ready
- Listening started
- Partial transcript
- Final transcript
- End of speech
- Failure
- Cancelled

Every event includes the originating session identifier. Empty partial results are ignored. A final
result ends the recording session exactly once.

## Privacy and lifecycle

- The provider must use only a recognizer explicitly identified as on-device.
- It must never persist audio or transcripts.
- It must release native recognition resources when the keyboard view is destroyed.
- Events from cancelled or superseded sessions must be ignored.
