# Contract: SpeechToTextProvider

## Purpose

Produce partial and final text from an automatically or explicitly initiated recording session,
using automatic language detection/switching when the Android recognition service supports it,
the selected `uk-UA` or `en-US` output language as fallback locale, and preferring local recognition
without blocking the MVP on an absent offline model.

## Events

- Ready
- Listening started
- Audio level (`0.0..1.0`, normalized for visualization only)
- Partial transcript
- Final transcript
- End of speech
- Failure
- Cancelled

Every event includes the originating session identifier. Empty partial results are ignored. A final
result ends the recording session exactly once.

Audio-level callbacks are transient: stale-session levels are discarded, they never enter the
keyboard state model or diagnostics, and they drive only the microphone visualization.

## Privacy and lifecycle

- The provider must prefer the explicit on-device recognizer.
- On Android 14+, the provider should request language detection and balanced language switching;
  recognition services are permitted to ignore unsupported extras.
- If the on-device service or requested language is unavailable, it must fall back to the system
  recognizer, which may use a network service.
- After a system-recognizer session, it should ask Android to download the requested offline model;
  Android may show user interaction or schedule the download.
- It must never persist audio or transcripts.
- It must release native recognition resources when the keyboard view is destroyed.
- Events from cancelled or superseded sessions must be ignored.
