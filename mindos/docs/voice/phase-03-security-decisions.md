# Phase 3 Headless Pipeline Security Decisions

Date: 2026-06-10

## Status

Accepted for Phase 3.

## Risk

Phase 3 is the first component that connects speech-derived text to `TextCommandGateway`. A retry,
partial transcript, policy bypass, automatic confirmation, or telemetry leak could execute the
wrong action or expose audio/transcript data.

## Decision

- One `VoiceCommandOrchestrator.runOnce()` captures once, calls ASR once, and calls
  `TextCommandGateway.routeAndExecute()` at most once.
- Only a transcript accepted by `TranscriptPolicy` reaches the gateway.
- `NeedsConfirmation` is returned and may be spoken, but the voice orchestrator never calls
  `confirmAndExecute()` automatically.
- TTS and audio-output failures occur after dispatch and only produce `Degraded`; they never retry
  routing or execution.
- ASR and TTS calls use configured timeouts. Recoverable component exceptions become typed
  outcomes; coroutine cancellation and unrecoverable runtime errors still propagate.
- `requestId` and `sessionId` are propagated unchanged. The normalized transcript is sent only to
  the gateway and is never copied into outcomes or telemetry.
- Speech telemetry uses a closed metric catalog and allowlisted dimensions. Raw transcript,
  normalized text, audio bytes, prompts, response text, entity values, exception messages, and
  arbitrary action types are prohibited.
- Phase 3 remains headless and disabled by default. It adds no platform capture/output, UI,
  streaming, wake-word listener, or real provider integration.

## Consequences

The textual command gateway remains the sole route-to-execution boundary and continues to enforce
ambiguity, safety, dry-run, and confirmation policy. Voice response degradation cannot change the
already-produced dispatch result.

## Verification

- Run `.\gradlew.bat :mind-speech:jvmTest --console=plain`.
- Search `mind-speech` for platform/provider imports and raw text/audio telemetry.
- Run `.\gradlew.bat productionReady --console=plain`.
