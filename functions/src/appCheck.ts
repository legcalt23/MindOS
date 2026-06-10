/**
 * appCheck.ts
 * App Check smoke-test callable.
 * All other callables use requireAppCheck() from lib/guards.ts directly.
 */
import * as functions from 'firebase-functions';
import { requireAppCheck } from './lib/guards';
import { clientIp, checkRateLimitByKey, IP_RATE_LIMITS } from './lib/rateLimiter';

export const verifyClientIntegrity = functions.https.onCall(
  async (_data, context) => {
    requireAppCheck(context);
    // App Check gates this callable, but throttle per IP so a single attested
    // client cannot use it as an amplification/probing endpoint.
    await checkRateLimitByKey(
      `ip:${clientIp(context.rawRequest)}:verifyClientIntegrity`,
      IP_RATE_LIMITS['verifyClientIntegrity'],
    );
    return { status: 'ok', message: 'App Check verified.' };
  },
);
