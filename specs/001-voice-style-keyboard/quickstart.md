# Quickstart Validation: Bubnilka MVP

## Prerequisites

- JDK 17
- Android SDK platform 36 and build tools 36.0.0
- Android 12+ device with an installed Ukrainian on-device speech model
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
2. Open Bubnilka and accept the cloud-processing disclosure.
3. Use the provided system buttons to enable and select Bubnilka.
4. Focus a normal text field in Notes, Telegram, a browser, Gmail, and one additional app.
5. Select Structured, dictate a Ukrainian sentence, and verify only the final styled result is
   inserted.
6. Repeat with another style, then exercise Undo and Regenerate.
7. Disable networking, dictate again, and verify Retry plus Insert raw are offered.
8. Focus a password field and verify voice/cloud controls are unavailable.
9. Record observed latency, recognition accuracy, and app-specific limitations in the project
   feedback notes before claiming device verification.
