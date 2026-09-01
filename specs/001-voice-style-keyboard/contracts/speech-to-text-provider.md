# Contract: SpeechToTextProvider

## Purpose

Produce partial and final text from one explicitly initiated recording session, preferring local
recognition without blocking the MVP on an absent offline model.

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

- The provider must prefer the explicit on-device recognizer.
- If the on-device service or requested language is unavailable, it must fall back to the system
  recognizer, which may use a network service.
- After a system-recognizer session, it should ask Android to download the requested offline model;
  Android may show user interaction or schedule the download.
- It must never persist audio or transcripts.
- It must release native recognition resources when the keyboard view is destroyed.
- Events from cancelled or superseded sessions must be ignored.
