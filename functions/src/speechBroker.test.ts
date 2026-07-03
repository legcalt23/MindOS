import * as functions from 'firebase-functions';
import {
  createSpeechAsrHandler,
  pcmS16LeToWav,
  SpeechBrokerDependencies,
  SpeechProviderRequest,
  updateVoiceCloudSpeechConsent,
} from './speechBroker';

function pcm(durationMs = 100): Buffer {
  return Buffer.alloc(16_000 * 2 * durationMs / 1_000, 1);
}

function makeReq(overrides: Record<string, unknown> = {}) {
  return {
    method: 'POST',
    rawBody: pcm(),
    headers: {
      authorization: 'Bearer firebase-id-token',
      'x-firebase-appcheck': 'app-check-token',
      'content-type': 'application/octet-stream',
      'x-mindos-request-id': 'request-1',
      'x-mindos-session-id': 'session-1',
      'x-mindos-language-tag': 'es-US',
      'x-mindos-audio-encoding': 'pcm_s16le',
      'x-mindos-sample-rate-hz': '16000',
      'x-mindos-channel-count': '1',
      'x-mindos-duration-ms': '100',
    },
    ...overrides,
  };
}

function makeRes() {
  const statusMock = jest.fn();
  const jsonMock = jest.fn();
  const res = {
    statusMock,
    jsonMock,
    status(code: number) { statusMock(code); return res; },
    json(body: unknown) { jsonMock(body); return res; },
  };
  return res;
}

function dependencies(overrides: Partial<SpeechBrokerDependencies> = {}) {
  const logs: unknown[] = [];
  const providerRequests: SpeechProviderRequest[] = [];
  const deps: SpeechBrokerDependencies = {
    enabled: () => true,
    verifyIdToken: async () => ({ uid: 'user-1' }),
    verifyAppCheck: async () => undefined,
    hasCloudSpeechConsent: async () => true,
    rateLimit: async () => undefined,
    invokeProvider: async (request) => {
      providerRequests.push(request);
      return { text: 'abre ajustes', confidence: 0.95, backendId: 'nvidia-nim' };
    },
    log: (level, event, fields) => logs.push({ level, event, fields }),
    now: () => 1_000,
    ...overrides,
  };
  return { deps, logs, providerRequests };
}

type HttpHandler = (req: unknown, res: unknown) => Promise<void>;

describe('speech broker audio adapter', () => {
  it('wraps PCM S16LE in a valid mono WAV header', () => {
    const raw = pcm();
    const wav = pcmS16LeToWav(raw, 16_000, 1);

    expect(wav.toString('ascii', 0, 4)).toBe('RIFF');
    expect(wav.toString('ascii', 8, 12)).toBe('WAVE');
    expect(wav.readUInt32LE(40)).toBe(raw.length);
    expect(wav.length).toBe(raw.length + 44);
  });
});

describe('voice cloud speech consent', () => {
  it('persists only an explicit boolean decision', async () => {
    const set = jest.fn().mockResolvedValue(undefined);

    await expect(updateVoiceCloudSpeechConsent('user-1', { allowed: true }, { set }))
      .resolves.toEqual({ allowed: true });
    expect(set).toHaveBeenCalledWith('user-1', true);
    await expect(updateVoiceCloudSpeechConsent('user-1', { allowed: true, extra: 'x' }, { set }))
      .rejects.toMatchObject({ code: 'invalid-argument' });
  });
});

describe('speechAsrRecognize handler', () => {
  it('fails closed while the server feature switch is disabled', async () => {
    const { deps, providerRequests } = dependencies({ enabled: () => false });
    const res = makeRes();

    await (createSpeechAsrHandler(deps) as HttpHandler)(makeReq(), res);

    expect(res.statusMock).toHaveBeenCalledWith(503);
    expect(providerRequests).toHaveLength(0);
  });

  it('requires both Firebase Auth and App Check tokens', async () => {
    const { deps } = dependencies();
    const req = makeReq({ headers: { 'content-type': 'application/octet-stream' } });
    const res = makeRes();

    await (createSpeechAsrHandler(deps) as HttpHandler)(req, res);

    expect(res.statusMock).toHaveBeenCalledWith(401);
  });

  it('requires server-side cloud speech consent', async () => {
    const { deps, providerRequests } = dependencies({ hasCloudSpeechConsent: async () => false });
    const res = makeRes();

    await (createSpeechAsrHandler(deps) as HttpHandler)(makeReq(), res);

    expect(res.statusMock).toHaveBeenCalledWith(403);
    expect(providerRequests).toHaveLength(0);
  });

  it('rejects silent or mismatched audio before provider invocation', async () => {
    const { deps, providerRequests } = dependencies();
    const silentRes = makeRes();
    const mismatchedRes = makeRes();

    await (createSpeechAsrHandler(deps) as HttpHandler)(makeReq({ rawBody: Buffer.alloc(pcm().length) }), silentRes);
    await (createSpeechAsrHandler(deps) as HttpHandler)(
      makeReq({
        headers: {
          ...makeReq().headers,
          'x-mindos-duration-ms': '9000',
        },
      }),
      mismatchedRes,
    );

    expect(silentRes.statusMock).toHaveBeenCalledWith(400);
    expect(mismatchedRes.statusMock).toHaveBeenCalledWith(400);
    expect(providerRequests).toHaveLength(0);
  });

  it('sends WAV to the provider and returns a correlated final transcript', async () => {
    const { deps, providerRequests, logs } = dependencies();
    const res = makeRes();

    await (createSpeechAsrHandler(deps) as HttpHandler)(makeReq(), res);

    expect(res.statusMock).toHaveBeenCalledWith(200);
    expect(providerRequests).toHaveLength(1);
    expect(providerRequests[0].wavAudio.toString('ascii', 0, 4)).toBe('RIFF');
    expect(res.jsonMock).toHaveBeenCalledWith(expect.objectContaining({
      requestId: 'request-1',
      sessionId: 'session-1',
      transcript: 'abre ajustes',
      backendId: 'nvidia-nim',
      final: true,
    }));
    expect(JSON.stringify(logs)).not.toContain('abre ajustes');
    expect(JSON.stringify(logs)).not.toContain('user-1');
  });

  it('maps rate limiting and provider timeout to typed HTTP failures', async () => {
    const rateLimited = dependencies({
      rateLimit: async () => {
        throw new functions.https.HttpsError('resource-exhausted', 'rate');
      },
    });
    const providerTimeout = dependencies({
      invokeProvider: async () => {
        const error = new Error('provider_timeout') as Error & { status: number; code: string };
        error.status = 504;
        error.code = 'provider_timeout';
        throw error;
      },
    });
    const rateRes = makeRes();
    const timeoutRes = makeRes();

    await (createSpeechAsrHandler(rateLimited.deps) as HttpHandler)(makeReq(), rateRes);
    await (createSpeechAsrHandler(providerTimeout.deps) as HttpHandler)(makeReq(), timeoutRes);

    expect(rateRes.statusMock).toHaveBeenCalledWith(429);
    expect(timeoutRes.statusMock).toHaveBeenCalledWith(504);
  });
});
