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
2. Open Bubnilka and accept disclosure version 4 covering automatic listening, system-recognizer
   fallback, Gemini processing/translation, and default whole-field context. Earlier acceptance
   is intentionally invalidated.
3. Use the provided system buttons to enable and select Bubnilka.
4. Focus a normal text field in Notes, Telegram, a browser, Gmail, and one additional app.
5. Open a field and verify listening starts automatically. Select Structured, dictate a sentence
   containing filler, an unrelated comment, and a deliberate foreign term; verify speaking style is
   preserved, noise is removed, and the foreign term is not transliterated. While listening, verify
   the microphone has a gentle ripple in silence and visibly stronger waves while speaking.
6. In the gear popup select English and dictate Ukrainian, then select Ukrainian and dictate
   English. Verify each complete result uses the selected language and the selection survives IME
   recreation. Exercise Undo and Regenerate.
7. With Combine with field enabled by default, add dictation to existing editor text and verify
   Gemini returns one coherent result with correct spacing and the entire field is replaced. Change
   the field during the request and verify the cloud result still replaces its complete contents.
8. Disable Combine with field and verify the next transformed dictation is simply inserted at the
   cursor without sending the surrounding field.
9. Manually scroll a long live preview upward, verify new partial text does not force it down, then
   return to the bottom and verify auto-follow resumes.
10. Disable networking, dictate again, and verify Retry plus Insert raw are offered.
11. Focus a password field and verify voice/cloud controls are unavailable.
12. Tap Backspace and verify it removes one word or punctuation mark; hold it and verify deletion
   repeats, accelerates, and stops immediately on release without opening another keyboard.
13. Switch Android between light and dark system themes and verify the app, keyboard, and language
    popup follow without an in-app theme toggle.
14. Capture three cloud timings (first call and two warm calls) without transcript content:

    ```sh
    adb logcat -s Bubnilka | grep -E 'timing=gemini_generate_(first|repeat)_process'
    ```

    `first_process` is the first Gemini request since the app process started; later requests are
    `repeat_process`. Restart the app process before another cold sample.

15. Record observed latency, recognition accuracy, and app-specific limitations in the project
   feedback notes before claiming device verification.

If Ukrainian on-device recognition is missing, the first dictation should use the system recognizer.
After a fallback session Android receives an automatic request for its `uk-UA` or `en-US` model;
the system may show confirmation or schedule the download according to the configured recognition
service.
