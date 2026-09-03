# Quickstart Validation: Bubnilka MVP

## Prerequisites

- JDK 17
- Android SDK platform 36 and build tools 36.0.0
- Android 12+ device with an enabled system speech-recognition service; installed Ukrainian and
  English offline models are preferred but not required for the first run
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
2. Open Bubnilka and accept disclosure version 3 covering automatic listening, system-recognizer
   fallback, Gemini processing/translation, and explicit whole-field context. Earlier acceptance
   is intentionally invalidated.
3. Use the provided system buttons to enable and select Bubnilka.
4. Focus a normal text field in Notes, Telegram, a browser, Gmail, and one additional app.
5. Open a field and verify listening starts automatically. Select Structured, dictate a Ukrainian
   sentence, and verify wording is preserved while light structure is added.
6. Use the gear popup to test EN, UA→EN, and EN→UA, then exercise Undo and Regenerate.
7. Enable Field + voice, add dictation to existing editor text, and verify the entire result is
   safely replaced; modify the field during a request and verify replacement is refused.
8. Manually scroll a long live preview upward, verify new partial text does not force it down, then
   return to the bottom and verify auto-follow resumes.
9. Disable networking, dictate again, and verify Retry plus Insert raw are offered.
10. Focus a password field and verify voice/cloud controls are unavailable.
11. Tap Backspace after insertion and verify it removes the selection or previous character without
   opening another keyboard.
12. Capture three cloud timings (first call and two warm calls) without transcript content:

    ```sh
    adb logcat -s Bubnilka | grep 'timing=gemini_generate'
    ```

13. Record observed latency, recognition accuracy, and app-specific limitations in the project
   feedback notes before claiming device verification.

If Ukrainian on-device recognition is missing, the first dictation should use the system recognizer.
After a fallback session Android receives an automatic request for its `uk-UA` or `en-US` model;
the system may show confirmation or schedule the download according to the configured recognition
service.
