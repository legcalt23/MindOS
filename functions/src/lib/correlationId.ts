/**
 * correlationId.ts
 * Generates and extracts correlation IDs for request tracing.
 *
 * Callable functions don't expose HTTP headers directly, so the
 * correlation ID is either supplied by the client in the data payload
 * or generated server-side. The ID travels through all log entries
 * and error responses for end-to-end tracing.
 */

import * as crypto from 'crypto';

const CORRELATION_ID_PATTERN = /^[A-Za-z0-9_-]{8,128}$/;

export function resolveCorrelationId(data: unknown): string {
  if (
    data &&
    typeof data === 'object' &&
    'correlationId' in data &&
    typeof (data as Record<string, unknown>)['correlationId'] === 'string'
  ) {
    const clientId = (data as Record<string, unknown>)['correlationId'] as string;
    if (CORRELATION_ID_PATTERN.test(clientId)) {
      return clientId;
    }
  }
  return generateCorrelationId();
}

export function generateCorrelationId(): string {
  return crypto.randomUUID();
}
