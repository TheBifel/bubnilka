# Quickstart Validation: Bubnilka MVP

## Prerequisites

- JDK 17
- Android SDK platform 36 and build tools 36.0.0
- Android 12+ device with an enabled system speech-recognition service; an installed Ukrainian
  offline model is preferred but no longer required for the first run
- Optional Firebase Spark project configured for Firebase AI Logic, Gemini Developer API, and App
  Check debug provider

## Credential-free verification

```sh
./scripts/verify.sh
```

Expected: shared/common tests, Android unit tests, lint, and debug APK build pass without Firebase
credentials. Cloud actions report a configuration error at runtime rather than crashing.

## Local Firebase configuration

Copy `firebase.local.properties.example` to the ignored `firebase.local.properties` and fill in the
Firebase Android app identifiers from project settings. Never commit the local file or an App Check
debug token.

On first debug run, register the App Check debug token printed by Firebase in the Firebase console.
Keep the project on Spark and do not link a Cloud Billing account.

The current allowlisted default is `gemini-3.5-flash-lite`. If the model is retired, update the
allowlist and specification before changing the local model value.

## Device smoke test

1. Install `android-app/build/outputs/apk/debug/android-app-debug.apk`.
2. Open Bubnilka and accept disclosure version 2 covering system-recognizer audio fallback and
   Gemini transcript processing. An earlier acceptance is intentionally invalidated.
3. Use the provided system buttons to enable and select Bubnilka.
4. Focus a normal text field in Notes, Telegram, a browser, Gmail, and one additional app.
5. Select Structured, dictate a Ukrainian sentence, and verify only the final styled result is
   inserted.
6. Repeat with another style, then exercise Undo and Regenerate.
7. Disable networking, dictate again, and verify Retry plus Insert raw are offered.
8. Focus a password field and verify voice/cloud controls are unavailable.
9. Tap Backspace after insertion and verify it removes the selection or previous character without
   opening another keyboard.
10. Capture three cloud timings (first call and two warm calls) without transcript content:

    ```sh
    adb logcat -s Bubnilka | grep 'timing=gemini_generate'
    ```

11. Record observed latency, recognition accuracy, and app-specific limitations in the project
   feedback notes before claiming device verification.

If Ukrainian on-device recognition is missing, the first dictation should use the system recognizer.
After that session Android receives an automatic `uk-UA` offline-model download request; the system
may show confirmation or schedule the download according to the configured recognition service.
