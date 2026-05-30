/**
 * sandboxProvider.ts
 * Sandbox payment provider — simulates purchases without real money.
 *
 * Security:
 *  - Sandbox tokens are generated server-side (UUID); client never controls them.
 *  - Only active when SANDBOX_PAYMENTS_ENABLED=true.
 *  - coinsToCredit always derived from server-side PACK_CATALOG, never from client.
 *  - For beta/development use only — requires explicit feature flag.
 *
 * This file is intentionally kept separate from paymentProviders.ts to allow
 * tree-shaking and to make the fail-closed property of real-provider stubs obvious.
 */

import { PaymentProvider, VerifyPurchaseParams, VerifyPurchaseResult, RefundStatusParams, RefundStatusResult } from './paymentProviders';
import { PACK_CATALOG } from './walletValidators';
import { walletError } from './walletErrors';

// ── Feature flag ──────────────────────────────────────────────────────────────

/** Sandbox is only active when explicitly enabled via environment variable. */
function isSandboxEnabled(): boolean {
  return process.env['SANDBOX_PAYMENTS_ENABLED'] === 'true';
}

// ── SandboxPaymentProvider ────────────────────────────────────────────────────

export class SandboxPaymentProvider implements PaymentProvider {
  readonly providerName = 'sandbox' as const;

  /**
   * Verify a sandbox purchase.
   *
   * The sandbox token was generated server-side during initiatePurchase.
   * Verification just validates the packId from the server catalog.
   * No external API calls are made.
   */
  async verify(params: VerifyPurchaseParams): Promise<VerifyPurchaseResult> {
    if (!isSandboxEnabled()) {
      throw walletError('PROVIDER_NOT_CONFIGURED', params.correlationId);
    }

    const pack = PACK_CATALOG[params.packId];
    if (!pack) {
      throw walletError('INVALID_PACK_ID', params.correlationId);
    }

    return {
      isValid:               true,
      coinsToCredit:         pack.coins, // from server catalog — never from client
      providerTransactionId: params.providerPurchaseToken.slice(0, 36),
      sanitizedResponse:     { sandbox: true, packId: params.packId, coinsToCredit: pack.coins },
    };
  }

  /**
   * Sandbox purchases are never refunded by an external provider.
   * Admin refunds use LedgerService.reverse() directly.
   */
  async refundStatus(_params: RefundStatusParams): Promise<RefundStatusResult> {
    return { isRefunded: false, providerRefundId: null };
  }
}
