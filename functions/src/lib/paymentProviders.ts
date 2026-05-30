/**
 * paymentProviders.ts
 * PaymentProvider interface and fail-closed stubs for real payment providers.
 *
 * Security contract:
 *  - Real providers (google_play, apple_iap, stripe) ALWAYS throw PROVIDER_NOT_CONFIGURED
 *    until credentials are configured AND a real implementation replaces the stub.
 *  - Even with credentials present, stubs fail closed — setting an env var alone is not
 *    enough to enable a real provider. The stub body must be replaced.
 *  - No secrets or credentials may be hardcoded here or in any provider file.
 *  - coinsToCredit in VerifyPurchaseResult always comes from the server-side PACK_CATALOG,
 *    never from the payment provider's response.
 */

import { walletError } from './walletErrors';
import { PurchaseProvider } from './walletSchema';

// ── Normalized models ─────────────────────────────────────────────────────────

export interface VerifyPurchaseParams {
  purchaseId:           string;
  walletId:             string;
  packId:               string;
  providerPurchaseToken: string; // raw token — hash stored, raw used only for verification
  correlationId:        string;
}

export interface VerifyPurchaseResult {
  isValid:               boolean;
  coinsToCredit:         number;  // from server PACK_CATALOG — never from provider response
  providerTransactionId: string | null;
  sanitizedResponse:     Record<string, unknown>; // no secrets stored
}

export interface RefundStatusParams {
  providerTransactionId: string;
  correlationId:         string;
}

export interface RefundStatusResult {
  isRefunded:      boolean;
  providerRefundId: string | null;
}

// ── Interface ─────────────────────────────────────────────────────────────────

export interface PaymentProvider {
  readonly providerName: PurchaseProvider;

  /** Verify that a purchase token is legitimate and retrieve the coins amount. */
  verify(params: VerifyPurchaseParams): Promise<VerifyPurchaseResult>;

  /** Check if a previously completed purchase has been refunded by the provider. */
  refundStatus(params: RefundStatusParams): Promise<RefundStatusResult>;
}

// ── Fail-closed stubs for real providers ─────────────────────────────────────
// These stubs ALWAYS throw PROVIDER_NOT_CONFIGURED.
// To enable a real provider: replace the stub body with a real implementation
// that also validates credentials. Setting an env var alone is NOT sufficient.

class GooglePlayPaymentProvider implements PaymentProvider {
  readonly providerName = 'google_play' as const;

  async verify(params: VerifyPurchaseParams): Promise<VerifyPurchaseResult> {
    // STUB — real implementation requires Google Play Developer API credentials.
    // Env check is advisory only; stub always fails closed.
    const configured = Boolean(process.env['GOOGLE_PLAY_SERVICE_ACCOUNT_JSON']);
    if (!configured) {
      throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
    }
    // Stub — real implementation not yet provided.
    throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
  }

  async refundStatus(params: RefundStatusParams): Promise<RefundStatusResult> {
    throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
  }
}

class ApplePaymentProvider implements PaymentProvider {
  readonly providerName = 'apple_iap' as const;

  async verify(params: VerifyPurchaseParams): Promise<VerifyPurchaseResult> {
    // STUB — real implementation requires Apple App Store shared secret.
    const configured = Boolean(process.env['APPLE_IAP_SHARED_SECRET']);
    if (!configured) {
      throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
    }
    throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
  }

  async refundStatus(params: RefundStatusParams): Promise<RefundStatusResult> {
    throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
  }
}

class StripePaymentProvider implements PaymentProvider {
  readonly providerName = 'stripe' as const;

  async verify(params: VerifyPurchaseParams): Promise<VerifyPurchaseResult> {
    // STUB — real implementation requires Stripe secret key.
    const configured = Boolean(process.env['STRIPE_SECRET_KEY']);
    if (!configured) {
      throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
    }
    throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
  }

  async refundStatus(params: RefundStatusParams): Promise<RefundStatusResult> {
    throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
  }
}

// ── Factory ───────────────────────────────────────────────────────────────────

/**
 * Returns the PaymentProvider for the given provider name.
 * The sandbox provider is imported lazily to keep this module dependency-free of admin SDK.
 * Throws PROVIDER_NOT_CONFIGURED for unknown providers.
 */
export function getPaymentProvider(
  provider: PurchaseProvider,
  correlationId: string,
): PaymentProvider {
  switch (provider) {
    case 'google_play': return new GooglePlayPaymentProvider();
    case 'apple_iap':   return new ApplePaymentProvider();
    case 'stripe':      return new StripePaymentProvider();
    case 'sandbox':
      // Imported here to avoid circular dependencies; SandboxPaymentProvider
      // does its own feature-flag check inside verify().
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      const { SandboxPaymentProvider } = require('./sandboxProvider') as { SandboxPaymentProvider: new () => PaymentProvider };
      return new SandboxPaymentProvider();
    default:
      throw walletError('PROVIDER_NOT_CONFIGURED', correlationId);
  }
}
