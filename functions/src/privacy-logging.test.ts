/**
 * privacy-logging.test.ts
 *
 * Regression coverage for backend logging privacy:
 *  - walletLog must redact user/wallet/admin identifiers.
 *  - rateLimiter/adminAuth/storage must not interpolate raw UIDs or file paths.
 */

jest.mock('firebase-functions', () => ({
  logger: {
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  },
  https: {
    HttpsError: class extends Error {
      code: string;
      constructor(code: string, message: string) {
        super(message);
        this.code = code;
      }
    },
  },
}));

import * as fs from 'fs';
import * as path from 'path';
import * as functions from 'firebase-functions';
import { walletLog } from './lib/walletLogger';

describe('backend privacy logging', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('redacts walletLog user and wallet identifiers', () => {
    walletLog.info(
      {
        correlationId: 'corr-privacy',
        action: 'spendCoins',
        userId: 'raw-user-123',
        walletId: 'raw-wallet-456',
      },
      'wallet event',
      {
        targetWalletId: 'raw-target-789',
        actorId: 'raw-actor-000',
        accessToken: 'secret-token-value',
      },
    );

    const logged = (functions.logger.info as jest.Mock).mock.calls[0][1];
    const asJson = JSON.stringify(logged);
    expect(asJson).not.toContain('raw-user-123');
    expect(asJson).not.toContain('raw-wallet-456');
    expect(asJson).not.toContain('raw-target-789');
    expect(asJson).not.toContain('raw-actor-000');
    expect(asJson).not.toContain('secret-token-value');
    expect(asJson).toContain('<redacted:userId:');
    expect(asJson).toContain('<redacted:walletId:');
    expect(asJson).toContain('<redacted:targetWalletId:');
    expect(logged.accessToken).toBe('secret-t***');
  });

  it('keeps critical log sources free of raw UID/path interpolation patterns', () => {
    const files = [
      'lib/adminAuth.ts',
      'lib/rateLimiter.ts',
      'storage.ts',
      'accountDeletion.ts',
      'posts.ts',
      'socialGraph.ts',
    ];

    for (const file of files) {
      const source = fs.readFileSync(path.join(__dirname, file), 'utf8');
      expect(source).not.toContain('uid=${');
      expect(source).not.toContain('uid=');
      expect(source).not.toContain('User ${');
      expect(source).not.toContain('File ${');
      expect(source).not.toContain('prefix=${');
      expect(source).not.toContain('path=${');
    }
  });
});
