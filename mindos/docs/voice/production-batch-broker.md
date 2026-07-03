# Production Batch Speech Broker

Date: 2026-06-15

## Implemented Path

```text
Android microphone (PCM S16LE, mono, 16 kHz)
  -> AndroidSpeechBrokerTransport (HTTPS + Firebase ID token + App Check)
  -> speechAsrRecognize Cloud Function
  -> server-side consent + UID/session/IP rate limits
  -> PCM validation and WAV envelope
  -> NVIDIA Speech NIM /v1/audio/transcriptions
  -> correlated final transcript
  -> TranscriptPolicy
  -> TextCommandGateway
  -> CommandRouterImpl
  -> ActionExecutor / NavigationCoordinator
```

The Android app consumes the canonical `mind-android-voice` module through the root composite
build. `MindOSVoicePlatform` creates the brokered batch voice factory. Both app-side Remote Config
flags default to `false`, and the backend broker also defaults to disabled.

## Trust Boundaries

- The mobile app never receives an NVIDIA credential or direct NIM URL.
- The broker requires a valid Firebase ID token and Firebase App Check token.
- Cloud processing requires `voice_consents/{uid}.cloudSpeechProcessingAllowed == true`.
- `setVoiceCloudSpeechConsent` is the only provided consent mutation path. It requires auth,
  App Check, strict input, and rate limiting.
- Audio is transient request data. It is not written to Firestore, Storage, analytics, or logs.
- Logs use allowlisted dimensions only. They exclude audio, transcript, UID, session ID, provider
  payloads, and exception messages.

## Configuration

Client build environment:

| Variable | Purpose | Default |
| --- | --- | --- |
| `MINDOS_SPEECH_BROKER_URL` | HTTPS URL of `speechAsrRecognize` | disabled invalid endpoint |

Firebase Remote Config:

| Key | Purpose | Default |
| --- | --- | --- |
| `voice_commands_enabled` | Enables command/voice surface | `false` |
| `voice_cloud_speech_enabled` | Enables cloud ASR path | `false` |

Cloud Functions environment:

| Variable | Purpose | Default |
| --- | --- | --- |
| `VOICE_SPEECH_BROKER_ENABLED` | Server kill switch | `false` |
| `NVIDIA_SPEECH_NIM_BASE_URL` | Broker-only NIM base URL | unset |
| `NVIDIA_SPEECH_NIM_BEARER_TOKEN` | Required Secret Manager provider token | unset |
| `NVIDIA_SPEECH_NIM_ALLOW_PRIVATE_HTTP` | Allows RFC1918 HTTP NIM endpoint | `false` |

Create `NVIDIA_SPEECH_NIM_BEARER_TOKEN` in Firebase/Google Secret Manager before deployment. Never
put provider credentials in Gradle properties, `BuildConfig`, Remote Config, the app bundle, or
committed environment files.

## Rollout Order

1. Deploy a private/HTTPS NVIDIA Speech NIM endpoint and verify `/v1/audio/transcriptions`.
2. Configure broker environment and deploy Functions with `VOICE_SPEECH_BROKER_ENABLED=false`.
3. Verify auth, App Check, consent, rate limits, timeout, and privacy-safe logs in staging.
4. Set explicit test-user consent through `setVoiceCloudSpeechConsent`.
5. Enable the server switch for staging, then the two Remote Config flags for a small cohort.
6. Monitor success rate, provider latency, rejection rate, and rate-limit denials.
7. Roll back by disabling either cloud speech Remote Config flag or the server switch.

## Release Blockers

The following evidence is still mandatory before calling the feature production-ready:

- Mount the Android push-to-talk/confirmation UI into the root `MindOSApp` navigation graph and
  provide the production `NavigationCoordinator`, authorization policy, durable idempotency store,
  and observability sinks. The platform factory is linked, but deliberately not auto-enabled.
- Validate the real broker against a deployed NVIDIA NIM instance. Unit tests use a provider fake.
- Run microphone, permission, cancellation, navigation, and App Check tests on a physical Android
  device.
- Add an iOS capture/broker adapter. The current canonical command build targets JVM/Android only.
- Add brokered TTS before enabling spoken responses. Current production batch factory is text-only.
- Add a streaming transport before claiming partial transcript support. The implemented HTTP batch
  path returns final transcripts only.

No release gate may treat these items as complete without captured evidence.
