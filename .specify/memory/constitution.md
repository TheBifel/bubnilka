<!--
Sync Impact Report
- Version change: template -> 1.0.0
- Added principles: Privacy by Construction; Shared Core, Native Edges; Verifiable Changes;
  Free-Tier Cloud Boundary; MVP Simplicity and Real-Device Feedback
- Added sections: Product and Technology Constraints; Spec-Driven Delivery and Quality Gates
- Removed sections: none (template placeholders replaced)
- Follow-up TODOs: none
-->
# Bubnilka Constitution

## Core Principles

### I. Privacy by Construction
The keyboard MUST process only text the user explicitly dictates during the active session. It MUST
NOT read or transmit surrounding field content, clipboard content, passwords, PINs, payment data, or
other unrelated input. Audio and transcript content MUST NOT be persisted or written to logs. Cloud
processing MUST be disclosed during onboarding, and sensitive input fields MUST disable voice and
cloud actions. Privacy behavior is part of the acceptance criteria, not a release-time add-on.

### II. Shared Core, Native Edges
Product rules, state transitions, style definitions, prompt construction, validation, provider
contracts, and failure handling MUST live in Kotlin Multiplatform `commonMain` when they do not
depend on Android APIs. Android IME, microphone, speech recognition, Firebase, and text insertion
MUST remain behind platform adapters. The MVP MUST NOT contain iOS source, targets, builds, or Xcode
artifacts, but shared contracts MUST avoid Android types so that an iOS adapter can be added later.

### III. Verifiable Changes (NON-NEGOTIABLE)
Every feature MUST have explicit acceptance criteria and automated tests for its deterministic
behavior. Before every commit, formatting, static analysis, and unit tests MUST pass. Before every
push, the full verification script, including a debug APK build, MUST pass. Platform behavior that
cannot be proven without a device MUST have a documented manual test and MUST NOT be reported as
verified until the user confirms it on the target device.

### IV. Free-Tier Cloud Boundary
The MVP MUST use the Gemini Developer API through Firebase AI Logic on the no-cost Spark plan, with
no Cloud Billing account linked. Firebase App Check MUST protect requests. Only an allowlisted
free-tier text model and text-generation features may be used. The application MUST handle quota
exhaustion without retry storms and offer retry or raw-transcript insertion. Provider-specific code
MUST remain behind `CloudTransformProvider`; no OpenAI key, Gemini key, backend proxy, or Proxmox
runtime dependency is permitted in the MVP.

### V. MVP Simplicity and Real-Device Feedback
The first release MUST optimize for the shortest useful voice-to-text-to-styled-text path on the
Samsung Galaxy S21. It MUST provide a voice-first IME, local on-device speech recognition, a small
set of styles, automatic insertion, undo, regeneration, and clear progress/error states. Full
QWERTY layouts, autocorrection, cloud STT, accounts, payments, analytics, iOS, and local LLMs are
out of scope until the vertical slice works and real-device feedback justifies expansion.

## Product and Technology Constraints

- Product name: **Бубнілка**; repository slug: `bubnilka`; Android application ID:
  `com.thebifel.bubnilka`.
- Kotlin Multiplatform is mandatory for the shared core. The MVP builds Android/JVM targets only.
- The Android MVP targets Android 12/API 31 or newer and uses only
  `createOnDeviceSpeechRecognizer()`; it MUST NOT silently fall back to network speech recognition.
- Partial and raw transcripts appear only inside the keyboard UI. Only the final cloud-transformed
  text is automatically committed to the host field.
- Firebase configuration that identifies the project may be supplied locally, but credentials,
  debug App Check tokens, signing material, and private device data MUST never enter Git.
- The Proxmox environment is a development and CI-like build host only. The installed application
  MUST work when that host is offline.
- Free-tier prompts and responses may be processed under Google's free-tier data terms. Onboarding
  MUST make this explicit before the first cloud request.

## Spec-Driven Delivery and Quality Gates

Work MUST proceed through Constitution -> Specify -> Clarify when needed -> Plan -> Tasks -> Analyze
-> Implement -> Verify. A change that alters user-visible behavior MUST update its specification
and acceptance criteria before implementation. Architecture decisions with durable consequences
MUST be captured in `docs/adr/` or the applicable feature plan.

The repository MUST provide one documented verification entry point. CI MUST repeat the same core
checks on every push and pull request. Commits MUST be small, coherent, and free of secrets or
generated machine-local configuration. User device feedback MUST be recorded as evidence in the
feature spec or project documentation and converted into a new spec/task when it changes behavior.

## Governance

This constitution overrides informal implementation preferences and generated plans. Amendments
require an explicit documented change, a semantic version bump, and a sync-impact review of active
specifications and templates. MAJOR versions remove or redefine a principle, MINOR versions add or
materially expand governance, and PATCH versions clarify existing rules. Every plan, review, and
pre-push verification MUST check compliance; exceptions require a written rationale and migration
or removal plan.

**Version**: 1.0.0 | **Ratified**: 2026-09-01 | **Last Amended**: 2026-09-01
