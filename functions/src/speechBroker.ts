import { createHash, randomUUID } from 'crypto';
import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import { Request, Response } from 'express';
import { requireAppCheck, requireAuth } from './lib/guards';
import { clientIp, checkRateLimit, checkRateLimitByKey, RateLimitConfig } from './lib/rateLimiter';

const REQUIRED_SAMPLE_RATE_HZ = 16_000;
const REQUIRED_CHANNEL_COUNT = 1;
const MAX_AUDIO_DURATION_MS = 10_000;
const MAX_AUDIO_BYTES = REQUIRED_SAMPLE_RATE_HZ * REQUIRED_CHANNEL_COUNT * 2 * MAX_AUDIO_DURATION_MS / 1_000;
const MAX_TRANSCRIPT_LENGTH = 500;
const MAX_PROVIDER_RESPONSE_BYTES = 16 * 1024;
const PROVIDER_TIMEOUT_MS = 12_000;

const UID_RATE_LIMIT: RateLimitConfig = { maxCalls: 20, windowMs: 60_000 };
const SESSION_RATE_LIMIT: RateLimitConfig = { maxCalls: 10, windowMs: 60_000 };
const IP_RATE_LIMIT: RateLimitConfig = { maxCalls: 30, windowMs: 60_000 };

const IDENTIFIER = /^[A-Za-z0-9._:-]{1,128}$/;
const LANGUAGE_TAG = /^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$/;

interface SpeechHttpRequest extends Request {
  rawBody?: Buffer;
}

interface VerifiedIdToken {
  uid: string;
}

export interface SpeechProviderRequest {
  wavAudio: Buffer;
  languageTag: string;
}

export interface SpeechProviderTranscript {
  text: string;
  confidence: number | null;
  backendId: string;
}

type SpeechLogLevel = 'info' | 'warn' | 'error';

export interface SpeechBrokerDependencies {
  enabled: () => boolean;
  verifyIdToken: (token: string) => Promise<VerifiedIdToken>;
  verifyAppCheck: (token: string) => Promise<void>;
  hasCloudSpeechConsent: (uid: string) => Promise<boolean>;
  rateLimit: (bucketKey: string, config: RateLimitConfig) => Promise<void>;
  invokeProvider: (request: SpeechProviderRequest) => Promise<SpeechProviderTranscript>;
  log: (level: SpeechLogLevel, event: string, fields: Record<string, unknown>) => void;
  now: () => number;
}

export interface VoiceConsentStore {
  set(uid: string, allowed: boolean): Promise<void>;
}

interface ValidatedSpeechRequest {
  requestId: string;
  sessionId: string | null;
  languageTag: string;
  durationMs: number;
  pcmAudio: Buffer;
}

class SpeechRequestError extends Error {
  constructor(readonly status: number, readonly code: string) {
    super(code);
  }
}

class SpeechProviderError extends Error {
  constructor(readonly status: number, readonly code: string) {
    super(code);
  }
}

function header(req: Request, name: string): string | undefined {
  const value = req.headers[name.toLowerCase()];
  if (Array.isArray(value)) return value[0];
  return typeof value === 'string' ? value.trim() : undefined;
}

function bearerToken(req: Request): string | null {
  const authorization = header(req, 'authorization');
  if (!authorization?.startsWith('Bearer ')) return null;
  const token = authorization.slice('Bearer '.length).trim();
  return token || null;
}

function requiredIdentifier(req: Request, name: string): string {
  const value = header(req, name);
  if (!value || !IDENTIFIER.test(value)) {
    throw new SpeechRequestError(400, `invalid_${name.toLowerCase()}`);
  }
  return value;
}

function optionalIdentifier(req: Request, name: string): string | null {
  const value = header(req, name);
  if (value === undefined) return null;
  if (!IDENTIFIER.test(value)) {
    throw new SpeechRequestError(400, `invalid_${name.toLowerCase()}`);
  }
  return value;
}

function requiredInteger(req: Request, name: string): number {
  const value = header(req, name);
  if (!value || !/^[0-9]{1,10}$/.test(value)) {
    throw new SpeechRequestError(400, `invalid_${name.toLowerCase()}`);
  }
  return Number(value);
}

function validateSpeechRequest(req: SpeechHttpRequest): ValidatedSpeechRequest {
  const contentType = header(req, 'content-type')?.toLowerCase();
  if (contentType !== 'application/octet-stream') {
    throw new SpeechRequestError(415, 'unsupported_content_type');
  }

  const requestId = requiredIdentifier(req, 'x-mindos-request-id');
  const sessionId = optionalIdentifier(req, 'x-mindos-session-id');
  const languageTag = header(req, 'x-mindos-language-tag');
  const encoding = header(req, 'x-mindos-audio-encoding');
  const sampleRateHz = requiredInteger(req, 'x-mindos-sample-rate-hz');
  const channelCount = requiredInteger(req, 'x-mindos-channel-count');
  const durationMs = requiredInteger(req, 'x-mindos-duration-ms');
  const pcmAudio = req.rawBody;

  if (!languageTag || !LANGUAGE_TAG.test(languageTag)) {
    throw new SpeechRequestError(400, 'invalid_language_tag');
  }
  if (
    encoding !== 'pcm_s16le' ||
    sampleRateHz !== REQUIRED_SAMPLE_RATE_HZ ||
    channelCount !== REQUIRED_CHANNEL_COUNT
  ) {
    throw new SpeechRequestError(400, 'unsupported_audio_format');
  }
  if (!pcmAudio || pcmAudio.length === 0 || pcmAudio.length % 2 !== 0) {
    throw new SpeechRequestError(400, 'invalid_audio');
  }
  if (pcmAudio.length > MAX_AUDIO_BYTES || durationMs < 1 || durationMs > MAX_AUDIO_DURATION_MS) {
    throw new SpeechRequestError(413, 'audio_limit_exceeded');
  }
  if (pcmAudio.every((value) => value === 0)) {
    throw new SpeechRequestError(400, 'empty_audio');
  }

  const calculatedDurationMs = Math.floor(
    pcmAudio.length * 1_000 / (sampleRateHz * channelCount * 2),
  );
  if (Math.abs(calculatedDurationMs - durationMs) > 100) {
    throw new SpeechRequestError(400, 'audio_duration_mismatch');
  }

  return { requestId, sessionId, languageTag, durationMs, pcmAudio };
}

function bucketRef(value: string): string {
  return createHash('sha256').update(`speech-broker:${value}`).digest('hex').slice(0, 32);
}

function requestRef(requestId: string | undefined): string {
  return bucketRef(requestId || 'missing').slice(0, 12);
}

function durationBucket(durationMs: number): string {
  if (durationMs <= 2_000) return '0_2s';
  if (durationMs <= 5_000) return '2_5s';
  return '5_10s';
}

function sendError(res: Response, status: number, code: string): void {
  res.status(status).json({ error: code });
}

function logRejected(
  deps: SpeechBrokerDependencies,
  req: Request,
  status: number,
  code: string,
  stage: string,
): void {
  deps.log(status >= 500 ? 'error' : 'warn', 'speech_broker_rejected', {
    requestRef: requestRef(header(req, 'x-mindos-request-id')),
    result: code,
    stage,
    status,
  });
}

export function createSpeechAsrHandler(
  deps: SpeechBrokerDependencies,
): (req: SpeechHttpRequest, res: Response) => Promise<void> {
  return async (req, res): Promise<void> => {
    const startedAt = deps.now();
    if (req.method !== 'POST') {
      sendError(res, 405, 'method_not_allowed');
      return;
    }
    if (!deps.enabled()) {
      sendError(res, 503, 'speech_disabled');
      return;
    }

    const idToken = bearerToken(req);
    const appCheckToken = header(req, 'x-firebase-appcheck');
    if (!idToken || !appCheckToken) {
      logRejected(deps, req, 401, 'authentication_required', 'authentication');
      sendError(res, 401, 'authentication_required');
      return;
    }

    let uid: string;
    try {
      const [verified] = await Promise.all([
        deps.verifyIdToken(idToken),
        deps.verifyAppCheck(appCheckToken),
      ]);
      uid = verified.uid;
    } catch (_) {
      logRejected(deps, req, 401, 'authentication_failed', 'authentication');
      sendError(res, 401, 'authentication_failed');
      return;
    }

    let validated: ValidatedSpeechRequest;
    try {
      validated = validateSpeechRequest(req);
    } catch (error) {
      const typed = error as SpeechRequestError;
      const status = typed.status || 400;
      const code = typed.code || 'invalid_request';
      logRejected(deps, req, status, code, 'validation');
      sendError(res, status, code);
      return;
    }

    try {
      if (!(await deps.hasCloudSpeechConsent(uid))) {
        logRejected(deps, req, 403, 'cloud_speech_consent_required', 'consent');
        sendError(res, 403, 'cloud_speech_consent_required');
        return;
      }
    } catch (_) {
      logRejected(deps, req, 503, 'consent_check_unavailable', 'consent');
      sendError(res, 503, 'consent_check_unavailable');
      return;
    }

    try {
      await Promise.all([
        deps.rateLimit(`speech:uid:${bucketRef(uid)}`, UID_RATE_LIMIT),
        deps.rateLimit(`speech:session:${bucketRef(`${uid}:${validated.sessionId || 'none'}`)}`, SESSION_RATE_LIMIT),
        deps.rateLimit(`speech:ip:${bucketRef(clientIp(req))}`, IP_RATE_LIMIT),
      ]);
    } catch (error) {
      const isRateLimit = error instanceof functions.https.HttpsError &&
        error.code === 'resource-exhausted';
      const status = isRateLimit ? 429 : 503;
      const code = isRateLimit ? 'rate_limited' : 'rate_limit_unavailable';
      logRejected(deps, req, status, code, 'rate_limit');
      sendError(res, status, code);
      return;
    }

    try {
      const transcript = await deps.invokeProvider({
        wavAudio: pcmS16LeToWav(validated.pcmAudio, REQUIRED_SAMPLE_RATE_HZ, REQUIRED_CHANNEL_COUNT),
        languageTag: validated.languageTag,
      });
      if (
        !transcript.text.trim() ||
        transcript.text.length > MAX_TRANSCRIPT_LENGTH ||
        (transcript.confidence !== null &&
          (!Number.isFinite(transcript.confidence) || transcript.confidence < 0 || transcript.confidence > 1))
      ) {
        throw new SpeechProviderError(502, 'invalid_provider_response');
      }

      deps.log('info', 'speech_broker_completed', {
        requestRef: requestRef(validated.requestId),
        result: 'success',
        backendId: transcript.backendId,
        languageTag: validated.languageTag,
        audioDurationBucket: durationBucket(validated.durationMs),
        latencyMs: Math.max(0, deps.now() - startedAt),
      });
      res.status(200).json({
        requestId: validated.requestId,
        sessionId: validated.sessionId,
        transcript: transcript.text,
        confidence: transcript.confidence,
        languageTag: validated.languageTag,
        durationMs: validated.durationMs,
        backendId: transcript.backendId,
        final: true,
      });
    } catch (error) {
      const typed = error as SpeechProviderError;
      const status = typed.status || 502;
      const code = typed.code || 'provider_unavailable';
      logRejected(deps, req, status, code, 'provider');
      sendError(res, status, code);
    }
  };
}

export function pcmS16LeToWav(
  pcm: Buffer,
  sampleRateHz: number,
  channelCount: number,
): Buffer {
  const header = Buffer.alloc(44);
  const byteRate = sampleRateHz * channelCount * 2;
  const blockAlign = channelCount * 2;
  header.write('RIFF', 0, 'ascii');
  header.writeUInt32LE(36 + pcm.length, 4);
  header.write('WAVE', 8, 'ascii');
  header.write('fmt ', 12, 'ascii');
  header.writeUInt32LE(16, 16);
  header.writeUInt16LE(1, 20);
  header.writeUInt16LE(channelCount, 22);
  header.writeUInt32LE(sampleRateHz, 24);
  header.writeUInt32LE(byteRate, 28);
  header.writeUInt16LE(blockAlign, 32);
  header.writeUInt16LE(16, 34);
  header.write('data', 36, 'ascii');
  header.writeUInt32LE(pcm.length, 40);
  return Buffer.concat([header, pcm]);
}

export async function updateVoiceCloudSpeechConsent(
  uid: string,
  data: unknown,
  store: VoiceConsentStore,
): Promise<{ allowed: boolean }> {
  const record = data && typeof data === 'object' ? data as Record<string, unknown> : {};
  if (typeof record['allowed'] !== 'boolean' || Object.keys(record).some((key) => key !== 'allowed')) {
    throw new functions.https.HttpsError('invalid-argument', 'A single boolean allowed field is required.');
  }
  await store.set(uid, record['allowed']);
  return { allowed: record['allowed'] };
}

function isPrivateIpv4(hostname: string): boolean {
  return /^10\./.test(hostname) ||
    /^127\./.test(hostname) ||
    /^192\.168\./.test(hostname) ||
    /^172\.(1[6-9]|2[0-9]|3[01])\./.test(hostname);
}

function configuredProviderUrl(): string {
  const base = process.env['NVIDIA_SPEECH_NIM_BASE_URL'];
  if (!base) throw new SpeechProviderError(503, 'provider_not_configured');
  const endpoint = new URL(`${base.replace(/\/+$/, '')}/v1/audio/transcriptions`);
  const allowPrivateHttp = process.env['NVIDIA_SPEECH_NIM_ALLOW_PRIVATE_HTTP'] === 'true';
  if (
    endpoint.username ||
    endpoint.password ||
    (endpoint.protocol !== 'https:' &&
      !(allowPrivateHttp && endpoint.protocol === 'http:' && isPrivateIpv4(endpoint.hostname)))
  ) {
    throw new SpeechProviderError(503, 'provider_endpoint_rejected');
  }
  return endpoint.toString();
}

function multipartBody(wavAudio: Buffer, languageTag: string): { body: Buffer; contentType: string } {
  const boundary = `mindos-${randomUUID()}`;
  const parts = [
    Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="language"\r\n\r\n${languageTag}\r\n`),
    Buffer.from(
      `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="audio.wav"\r\n` +
      'Content-Type: audio/wav\r\n\r\n',
    ),
    wavAudio,
    Buffer.from(`\r\n--${boundary}--\r\n`),
  ];
  return {
    body: Buffer.concat(parts),
    contentType: `multipart/form-data; boundary=${boundary}`,
  };
}

export async function invokeNvidiaSpeechNim(
  request: SpeechProviderRequest,
): Promise<SpeechProviderTranscript> {
  const endpoint = configuredProviderUrl();
  const multipart = multipartBody(request.wavAudio, request.languageTag);
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), PROVIDER_TIMEOUT_MS);
  try {
    const headers: Record<string, string> = {
      'Content-Type': multipart.contentType,
      'Accept': 'application/json',
    };
    const providerToken = process.env['NVIDIA_SPEECH_NIM_BEARER_TOKEN'];
    if (!providerToken) throw new SpeechProviderError(503, 'provider_not_configured');
    headers['Authorization'] = `Bearer ${providerToken}`;

    const response = await fetch(endpoint, {
      method: 'POST',
      headers,
      body: Uint8Array.from(multipart.body),
      signal: controller.signal,
    });
    if (!response.ok) {
      throw new SpeechProviderError(response.status === 408 || response.status === 504 ? 504 : 502, 'provider_failed');
    }
    const bytes = Buffer.from(await response.arrayBuffer());
    if (bytes.length > MAX_PROVIDER_RESPONSE_BYTES) {
      throw new SpeechProviderError(502, 'provider_response_too_large');
    }
    const parsed = JSON.parse(bytes.toString('utf8')) as Record<string, unknown>;
    const text = parsed['text'];
    const confidence = parsed['confidence'];
    if (typeof text !== 'string') {
      throw new SpeechProviderError(502, 'invalid_provider_response');
    }
    return {
      text,
      confidence: typeof confidence === 'number' ? confidence : null,
      backendId: 'nvidia-nim',
    };
  } catch (error) {
    if (error instanceof SpeechProviderError) throw error;
    if (error instanceof Error && error.name === 'AbortError') {
      throw new SpeechProviderError(504, 'provider_timeout');
    }
    throw new SpeechProviderError(502, 'provider_unavailable');
  } finally {
    clearTimeout(timeout);
  }
}

const defaultDependencies: SpeechBrokerDependencies = {
  enabled: () => process.env['VOICE_SPEECH_BROKER_ENABLED'] === 'true',
  verifyIdToken: async (token) => admin.auth().verifyIdToken(token, true),
  verifyAppCheck: async (token) => {
    await admin.appCheck().verifyToken(token);
  },
  hasCloudSpeechConsent: async (uid) => {
    const consent = await admin.firestore().collection('voice_consents').doc(uid).get();
    return consent.exists && consent.data()?.['cloudSpeechProcessingAllowed'] === true;
  },
  rateLimit: checkRateLimitByKey,
  invokeProvider: invokeNvidiaSpeechNim,
  log: (level, event, fields) => functions.logger[level](event, fields),
  now: () => Date.now(),
};

export const speechAsrRecognize = functions
  .runWith({
    timeoutSeconds: 30,
    memory: '512MB',
    maxInstances: 20,
    secrets: ['NVIDIA_SPEECH_NIM_BEARER_TOKEN'],
  })
  .https.onRequest(createSpeechAsrHandler(defaultDependencies));

export const setVoiceCloudSpeechConsent = functions.https.onCall(async (data, context) => {
  requireAppCheck(context);
  const uid = requireAuth(context);
  await checkRateLimit(uid, 'voiceConsent');
  return updateVoiceCloudSpeechConsent(uid, data, {
    set: async (ownerUid, allowed) => {
      await admin.firestore().collection('voice_consents').doc(ownerUid).set({
        cloudSpeechProcessingAllowed: allowed,
        policyVersion: 1,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    },
  });
});
