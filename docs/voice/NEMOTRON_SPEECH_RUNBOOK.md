# Nemotron Speech Broker Runbook

## Fast Rollback

Disable `VOICE_SPEECH_BROKER_ENABLED` on Cloud Functions. This rejects new ASR requests before
authentication or provider work. Also set Remote Config keys `voice_commands_enabled=false` and
`voice_cloud_speech_enabled=false`.

## Expected Request

`speechAsrRecognize` accepts `POST application/octet-stream` containing mono 16 kHz PCM S16LE,
bounded to 10 seconds. It requires:

- `Authorization: Bearer <Firebase ID token>`
- `X-Firebase-AppCheck`
- opaque request/session correlation headers
- language, encoding, sample-rate, channel-count, and duration headers

The broker verifies consent, applies UID/session/IP limits, wraps PCM in WAV, and calls the
configured NVIDIA Speech NIM `/v1/audio/transcriptions` endpoint.

## Privacy Rules

Never log request bodies, WAV buffers, transcripts, Firebase tokens, provider tokens, raw UID,
session ID, provider payloads, or exception messages. Operational logs may contain only the
allowlisted result/stage/status, hashed request reference, backend ID, language tag, duration
bucket, and latency.

## Incident Triage

| Symptom | First checks |
| --- | --- |
| `speech_disabled` | Server kill switch and deployment environment |
| `authentication_failed` | Firebase Auth token age and App Check enforcement |
| `cloud_speech_consent_required` | `voice_consents/{uid}` server document |
| `rate_limited` | UID/session/IP buckets and client retry behavior |
| `provider_timeout` | NIM health, network path, provider saturation |
| `invalid_provider_response` | NIM model/API compatibility; do not log raw response |

Do not enable private HTTP unless the NIM endpoint is RFC1918-only and reached through an approved
private network path.
