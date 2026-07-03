# Phase 2 Speech Security Decisions

Date: 2026-06-10

## Status

Accepted for Phase 2.

## Risk

Raw audio and transcripts can contain personal, behavioral, contact, and contextual information.
Backend failures can also expose provider details or echo user content. Phase 2 introduces contracts
and deterministic mocks that handle these values before any production speech provider exists.

## Decision

- Speech contracts remain KMP-pure and in-memory. They are not serializable contracts.
- No speech contract, mock, fixture, or test logs raw audio or transcript text.
- `AudioPayload` owns defensive copies so callers cannot mutate captured audio after construction.
- Expected capture, ASR, TTS, and playback failures are typed results. Provider exception messages
  and raw payloads are not part of the failure contract.
- Test fixtures contain synthetic bytes and synthetic command text only.
- `VoiceRuntimeConfig` is disabled by default, has an independent kill switch, and defaults raw
  transcript logging to false.
- Phase 2 does not create an orchestrator, call `TextCommandGateway`, execute commands, navigate, or
  integrate any real provider.
- Transcript normalization is deliberately narrow: the wake word is recognized only at the start
  of the transcript, only immediate wake-word separators are removed, and only terminal `.?!`
  punctuation is stripped. Internal punctuation and content are preserved.
- Maximum transcript length is checked before wake-word or punctuation removal so normalization
  cannot hide an oversized input.
- Real provider adapters must keep credentials outside clients and apply bounded duration/payload
  enforcement before network transmission in their later implementation phase.

## Consequences

Raw transcript text necessarily exists transiently in `TranscriptResult` so policy can validate it,
but this phase creates no persistence, telemetry, or network path for it. Production integrations
must perform a separate privacy and security review before enabling voice.

## Verification

- Search `mind-speech` for logging, platform, and provider imports.
- Run `.\gradlew.bat :mind-speech:jvmTest --console=plain`.
- Run `.\gradlew.bat productionReady --console=plain`.
