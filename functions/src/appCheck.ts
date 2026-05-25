/**
 * appCheck.ts
 * App Check smoke-test callable.
 * All other callables use requireAppCheck() from lib/guards.ts directly.
 */
import * as functions from 'firebase-functions';
import { requireAppCheck } from './lib/guards';

export const verifyClientIntegrity = functions.https.onCall(
  async (_data, context) => {
    requireAppCheck(context);
    return { status: 'ok', message: 'App Check verified.' };
  },
);
