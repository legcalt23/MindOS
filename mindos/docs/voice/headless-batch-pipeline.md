# Headless Batch Voice Pipeline

Phase 3 connects deterministic speech mocks to the existing textual command gateway without using
platform audio APIs, UI, streaming, wake words, or real speech providers.

## Architecture

```text
VoiceCommandOrchestrator.runOnce(VoiceSession)
  -> SpeechInputProvider.capture()
  -> ASRBackend.transcribe()
  -> TranscriptPolicy.evaluate()
  -> TextCommandGateway.routeAndExecute()
  -> ResponseTextResolver.resolve()
  -> TTSBackend.synthesize()
  -> AudioOutputProvider.play()
  -> VoiceCommandOutcome + PII-safe SpeechTelemetryEvent records
```

The transcript policy is mandatory before routing. One `runOnce` call captures and transcribes
once, calls the gateway at most once, and never confirms automatically. TTS or output failures
happen after dispatch, return `VoiceCommandOutcome.Degraded`, and never retry execution.

`VoiceRuntimeConfig` is disabled by default. Headless tests must explicitly set
`VoiceRuntimeConfig(enabled = true)`. The kill switch overrides enablement.

## Configure Mocks

Use the deterministic implementations in `com.mindos.speech.mock`:

```kotlin
val input = FakeSpeechInputProvider.captured()
val asr = MockASRBackend.success(SpeechFixtures.transcript(text = "abre ajustes"))
val tts = MockTTSBackend.success()
val output = FakeAudioOutputProvider()

val orchestrator = DefaultVoiceCommandOrchestrator(
    speechInput = input,
    asr = asr,
    transcriptPolicy = DefaultTranscriptPolicy(),
    gateway = configuredTextCommandGateway,
    responseTextResolver = DefaultResponseTextResolver(),
    tts = tts,
    audioOutput = output,
    config = VoiceRuntimeConfig(enabled = true),
    telemetry = RecordingSpeechTelemetrySink(),
)

val outcome = orchestrator.runOnce(VoiceSession("request-1", "session-1"))
```

The gateway must be configured separately with a router, action adapter, executor, safety policy,
confirmation store, and `CommandRuntimeConfig(enabled = true)`. Use `dryRun = true` in
`VoiceRuntimeConfig` when validating routing and responses without calling the executor.

## Fixtures And Failure Modes

`SpeechFixtures` provides mono 16 kHz synthetic input, final Spanish transcripts, and synthesized
audio. Payload bytes are synthetic and mocks retain only safe observations such as byte count,
duration, sample rate, text length, and invocation count.

| Component | Useful deterministic configurations |
| --- | --- |
| Input | `captured()`, `permissionDenied()`, `timeout()`, `emptyAudio()`, `backendError()` |
| ASR | `success()`, `lowConfidence()`, `partial()`, `emptyTranscript()`, `timeout()`, `backendError()` |
| TTS | `success()`, `emptyAudio()`, `timeout()`, `backendError()` |
| Output | `PLAY`, `TIMEOUT`, `BACKEND_ERROR` |

## Example Commands And Expected Outcomes

| Transcript or setup | Expected outcome |
| --- | --- |
| `abre ajustes` with successful router/executor/mocks | `Completed`, executor called once, audio played once |
| `abre ajustes` with `dryRun = true` | `Completed` containing a dry-run dispatch, executor not called |
| Low-confidence or partial transcript | `Rejected`; gateway and executor not called |
| Ambiguous router result | `Rejected(AMBIGUOUS_COMMAND)`; executor not called |
| Sensitive action requiring confirmation | Dispatch reports `NeedsConfirmation`; voice never auto-confirms |
| TTS timeout after successful dispatch | `Degraded(TTS_TIMEOUT)`; command remains executed exactly once |
| Output backend error after successful dispatch | `Degraded(AUDIO_OUTPUT_ERROR)`; command remains executed exactly once |
| Default disabled configuration | `Rejected(VOICE_DISABLED)`; capture and gateway not called |

## Gradle Commands

Run commands from `mindos/`:

```powershell
.\gradlew.bat :mind-speech:jvmTest --console=plain
.\gradlew.bat :mind-command-runtime:jvmTest :mind-speech:jvmTest --console=plain
.\gradlew.bat verifyProductionRepository --console=plain
.\gradlew.bat productionReady --console=plain
```

The primary end-to-end reference is
`mind-speech/src/commonTest/kotlin/com/mindos/speech/VoiceCommandOrchestratorTest.kt`.

## Telemetry And Privacy

The default sink is `NoOpSpeechTelemetrySink`. `RecordingSpeechTelemetrySink` is for deterministic
headless verification. Events use a closed metric catalog and allowlisted dimensions. Never add
raw or normalized transcript text, audio bytes, response text, prompts, entity values, exception
messages, or arbitrary action types. Upstream callers must ensure `requestId` and `sessionId` are
non-PII correlation identifiers.

## Troubleshooting

- `VOICE_DISABLED`: explicitly enable `VoiceRuntimeConfig`; also verify the kill switch.
- `CONFIDENCE_TOO_LOW` or `TRANSCRIPT_NOT_FINAL`: configure a final transcript above the policy
  threshold; do not bypass the policy.
- Gateway is not called: inspect the capture, ASR, and transcript-policy outcome first.
- Executor is not called: check dry-run, ambiguity, safety rejection, and confirmation policy.
- `Degraded` after a successful dispatch: inspect TTS/output mocks; do not retry the command.
- Missing telemetry: inject a sink and verify the pipeline reached the relevant stage. Telemetry
  failures are intentionally swallowed and cannot change the command result.

## Rollback

Keep voice disabled, remove headless composition from callers, and remove `mind-speech` from the
Gradle build if necessary. No platform integration or provider credentials need rollback. Do not
remove or bypass `TextCommandGateway`; it remains the sole route-to-execution boundary.
