# Phase 5 Android Push-To-Talk Security Decisions

Date: 2026-06-10

## Status

Accepted for Phase 5.

## Risk

Android microphone access, in-memory audio, cancellation, and confirmation handling introduce
privacy and unintended-execution risks. A UI cancellation race, reused recorder, auto-confirmation,
or persisted payload could execute or expose a command the user did not intend.

## Decision

- Android push-to-talk remains disabled by default and requires the existing runtime/voice flags.
- Microphone permission is requested contextually after a user gesture. Permission denial never
  starts capture or routing.
- Capture is one bounded batch per gesture. No continuous listener, wake word, background service,
  or streaming behavior is added.
- Audio remains in memory as PCM and is never written to files, logs, telemetry, saved state, or
  analytics. Temporary PCM buffers are cleared after the defensive speech payload copy is created.
- Android input/output adapters use configurable hard timeouts and release native resources on
  success, failure, timeout, and cancellation.
- Cancellation propagates and is allowed only before command execution starts. The UI never
  claims that cancellation reversed an already-executed action.
- Partial, low-confidence, ambiguous, unknown, unsafe, or expired inputs never execute.
- `NeedsConfirmation` never auto-executes. Confirmation remains bound to the gateway's
  request/session/action/token record and short TTL.
- Cancelling a pending confirmation consumes its bound token through the canonical gateway without
  executing it. A UI-only dismissal is insufficient because it would leave the token valid until
  TTL expiry.
- Text confirmation is explicit. Voice confirmation additionally requires a final, high-confidence
  affirmative transcript.
- TTS/output failure degrades to textual UX and never retries the command.
- Existing DM/feed AAC/M4A recorders are not reused for ASR capture.
- No NVIDIA/provider integration, credentials, streaming, barge-in, or background microphone work
  is included.

## Verification

- Run the Android voice state-machine and adapter unit tests after each Phase 5 prompt.
- Verify no file APIs or transcript/audio logging exist in the Android voice module.
- Verify cancellation and confirmation rejection paths.
- Run `.\gradlew.bat productionReady --console=plain`.

## Rollback

Keep the feature flag disabled and remove the Android voice module from consumers. No stored audio,
database migration, provider credential, or background service requires cleanup.
