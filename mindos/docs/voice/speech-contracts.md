# Mind Speech Contracts

`mind-speech` contains provider-neutral contracts and a batch orchestrator for voice adapters. It
has no provider credential, Android API, Apple API, or direct NVIDIA dependency. Android capture
and broker transport live in `mind-android-voice`; NVIDIA integration lives behind the backend
broker.

## Boundaries

- `SpeechInputProvider` captures a typed `AudioInput`.
- `ASRBackend` converts audio into a typed `AsrResult`.
- `TranscriptPolicy` is the mandatory gate before any future router call.
- `TTSBackend` converts a safe response request into a typed `TtsResult`.
- `AudioOutputProvider` plays synthesized audio.

The ASR contract returns `AsrResult` rather than a raw `TranscriptResult`. This deliberately
strengthens the minimal sketch from the implementation pack: timeout, backend error, and invalid
input are expected typed outcomes, not exception-based control flow.

## Safe defaults

`VoiceRuntimeConfig` is disabled by default. Its kill switch overrides enablement, raw transcript
logging is prohibited, streaming is rejected as outside this phase, and ASR/TTS timeouts are
explicit typed configuration.

Audio payloads use defensive byte-array copies. Broker responses must match request/session/locale
correlation before becoming transcripts. Telemetry uses a closed catalog and redacted textual
representation. See [`phase-02-security-decisions.md`](phase-02-security-decisions.md).

## Phase boundary

`VoiceCommandOrchestrator` may call `TextCommandGateway` once after the transcript policy accepts a
final transcript. It never calls an executor directly, auto-confirms, or retries an execution.
Android batch capture and the broker client are implemented, but visible root UI/navigation wiring,
iOS, streaming partial transcripts, live NIM evidence, and brokered TTS remain release blockers.

The `com.mindos.speech.mock` package provides deterministic headless test doubles and synthetic
fixtures. Mock timeout and backend failures are immediate typed results; they do not sleep, throw
expected errors, retain raw audio payloads, or call command runtime APIs.

`DefaultTranscriptPolicy` is the pre-router gate for finality, empty text, confidence, maximum raw
length, optional or required leading wake words, and terminal ASR punctuation. It preserves locale
and internal content. It does not route or execute.

See [`production-batch-broker.md`](production-batch-broker.md) for deployment, trust boundaries,
remaining release blockers, and rollback.

## Rollback

Keep voice disabled, remove headless composition from callers, remove `mind-speech` from the Gradle
build, and remove these speech documents. No platform/provider integration needs rollback.
