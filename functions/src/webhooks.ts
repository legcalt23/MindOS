/**
 * webhooks.ts
 * HTTP webhook handlers for payment provider server-to-server notifications.
 *
 * Security contract (enforced in this file):
 *  - Real providers (google_play, apple_iap, stripe) ALWAYS fail closed (403) until
 *    real cryptographic signature validation is implemented in the stub body.
 *    Configuring the env-var secret alone is NOT sufficient to open the path.
 *  - Sandbox mode (SANDBOX_PAYMENTS_ENABLED=true) is the ONLY path that accepts events.
 *    This flag must never be set in production.
 *  - All stored payloads are sanitized — no raw signatures, provider secrets, or PII stored.
 *  - Events are deduplicated by {provider}_{providerEventId}. A second delivery of the
 *    same event returns 200 without re-processing (safe for provider retry logic).
 *  - No balance mutations or payment processing happen here — webhooks only store events.
 *
 * Env vars checked (advisory only for stubs):
 *  - GOOGLE_PLAY_WEBHOOK_SECRET
 *  - APPLE_WEBHOOK_SECRET
 *  - STRIPE_WEBHOOK_SECRET
 *  - SANDBOX_PAYMENTS_ENABLED
 */

import * as crypto from 'crypto';
import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import { Request, Response } from 'express';
import {
  COLLECTIONS,
  SCHEMA_VERSION,
  PaymentProviderEventDocument,
  PurchaseProvider,
} from './lib/walletSchema';
import { walletLog } from './lib/walletLogger';
import { isProductionFirebaseProject } from './lib/projectEnvironment';

const db = () => admin.firestore();

// ── Helpers ───────────────────────────────────────────────────────────────────

function isSandboxEnabled(): boolean {
  return process.env['SANDBOX_PAYMENTS_ENABLED'] === 'true';
}

function headerValue(req: Request, name: string): string | undefined {
  const value = req.headers[name.toLowerCase()];
  if (Array.isArray(value)) return value[0];
  return typeof value === 'string' ? value : undefined;
}

function matchesSecret(provided: string | undefined, expected: string): boolean {
  if (!provided || provided.length !== expected.length) return false;
  return crypto.timingSafeEqual(Buffer.from(provided), Buffer.from(expected));
}

function eventDocId(provider: PurchaseProvider, providerEventId: string): string {
  return `${provider}_${providerEventId}`;
}

// ── Core handler ──────────────────────────────────────────────────────────────

/**
 * Shared webhook handler for all payment providers.
 *
 * Decision tree:
 *  1. Non-POST → 405
 *  2. Sandbox disabled + secret absent  → 403 (not configured)
 *  3. Sandbox disabled + secret present → 403 (stub: real validation not implemented)
 *  4. Sandbox enabled + invalid body    → 400
 *  5. Sandbox enabled + duplicate event → 200 (already processed)
 *  6. Sandbox enabled + new event       → 201 (stored)
 */
async function handleProviderWebhook(
  provider: PurchaseProvider,
  secretEnvVar: string,
  req: Request,
  res: Response,
): Promise<void> {
  const correlationId = crypto.randomUUID();
  const action = `webhook_${provider}` as const;

  // ── Method guard ────────────────────────────────────────────────────────────
  if (req.method !== 'POST') {
    res.status(405).send('Method Not Allowed');
    return;
  }

  const sandboxMode     = isSandboxEnabled();
  const secretConfigured = Boolean(process.env[secretEnvVar]);

  // ── Fail closed: real provider path ────────────────────────────────────────
  if (!sandboxMode) {
    if (!secretConfigured) {
      walletLog.warn(
        { correlationId, action },
        `Webhook rejected: ${secretEnvVar} not configured`,
      );
      res.status(403).json({ error: 'Webhook not configured.', correlationId });
      return;
    }

    // Secret is configured but real cryptographic signature validation is not
    // yet implemented. This stub always fails closed to prevent processing
    // unverified events. Replace this block with real HMAC/JWT validation.
    walletLog.warn(
      { correlationId, action },
      'Webhook rejected: signature validation not yet implemented (stub — fail closed)',
    );
    res.status(403).json({ error: 'Webhook signature validation not implemented.', correlationId });
    return;
  }

  // ── Sandbox path: body validation ───────────────────────────────────────────
  if (isProductionFirebaseProject()) {
    walletLog.warn(
      { correlationId, action },
      'Webhook rejected: sandbox mode is blocked in production project',
    );
    res.status(403).json({ error: 'Sandbox webhooks are disabled in production.', correlationId });
    return;
  }

  const sandboxSecret = process.env['SANDBOX_WEBHOOK_SECRET'];
  if (!sandboxSecret || !matchesSecret(headerValue(req, 'x-mindos-sandbox-webhook-secret'), sandboxSecret)) {
    walletLog.warn(
      { correlationId, action },
      'Webhook rejected: sandbox secret missing or invalid',
    );
    res.status(403).json({ error: 'Sandbox webhook secret required.', correlationId });
    return;
  }

  const body = req.body as Record<string, unknown>;

  if (typeof body['providerEventId'] !== 'string' || !body['providerEventId']) {
    walletLog.warn({ correlationId, action }, 'Webhook rejected: missing providerEventId');
    res.status(400).json({ error: 'Missing required field: providerEventId.', correlationId });
    return;
  }
  if (typeof body['eventType'] !== 'string' || !body['eventType']) {
    walletLog.warn({ correlationId, action }, 'Webhook rejected: missing eventType');
    res.status(400).json({ error: 'Missing required field: eventType.', correlationId });
    return;
  }

  const providerEventId = body['providerEventId'] as string;
  const eventType       = body['eventType']       as string;
  const purchaseId      = typeof body['purchaseId'] === 'string' ? body['purchaseId'] : null;
  const docId           = eventDocId(provider, providerEventId);

  // Sanitized payload — only known-safe fields stored; no raw signatures or tokens.
  const sanitizedPayload: Record<string, unknown> = {
    eventType,
    ...(purchaseId !== null ? { purchaseId } : {}),
    sandbox: true,
  };

  const ref = db().collection(COLLECTIONS.PAYMENT_PROVIDER_EVENTS).doc(docId);

  // ── Deduplication: return 200 if event already stored ───────────────────────
  const existing = await ref.get();
  if (existing.exists) {
    walletLog.info(
      { correlationId, action },
      'Webhook deduplicated: event already stored',
      { eventId: docId },
    );
    res.status(200).json({ status: 'already_processed', eventId: docId, correlationId });
    return;
  }

  // ── Store sanitized event ───────────────────────────────────────────────────
  const doc: PaymentProviderEventDocument = {
    eventId:           docId,
    provider,
    providerEventId,
    purchaseId,
    eventType,
    sanitizedPayload,
    processedAt:       Date.now(),
    schemaVersion:     SCHEMA_VERSION,
  };

  await ref.set(doc);

  walletLog.info(
    { correlationId, action },
    'Webhook event stored',
    { eventId: docId, eventType },
  );

  res.status(201).json({ status: 'processed', eventId: docId, correlationId });
}

// ── Exported HTTP handlers ────────────────────────────────────────────────────

/**
 * Google Play Real-time Developer Notifications webhook.
 * Fail closed: requires GOOGLE_PLAY_WEBHOOK_SECRET or SANDBOX_PAYMENTS_ENABLED.
 * Real JWT validation stub — always fails closed in non-sandbox environments.
 */
export const webhookGooglePlay = functions.https.onRequest(async (req, res) => {
  await handleProviderWebhook('google_play', 'GOOGLE_PLAY_WEBHOOK_SECRET', req, res);
});

/**
 * Apple App Store Server Notifications webhook.
 * Fail closed: requires APPLE_WEBHOOK_SECRET or SANDBOX_PAYMENTS_ENABLED.
 * Real signed-JWS validation stub — always fails closed in non-sandbox environments.
 */
export const webhookAppStore = functions.https.onRequest(async (req, res) => {
  await handleProviderWebhook('apple_iap', 'APPLE_WEBHOOK_SECRET', req, res);
});

/**
 * Stripe webhook events handler.
 * Fail closed: requires STRIPE_WEBHOOK_SECRET or SANDBOX_PAYMENTS_ENABLED.
 * Real Stripe-Signature validation stub — always fails closed in non-sandbox environments.
 */
export const webhookStripe = functions.https.onRequest(async (req, res) => {
  await handleProviderWebhook('stripe', 'STRIPE_WEBHOOK_SECRET', req, res);
});
