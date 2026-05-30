# Backend Security — Cloud Functions

**Date:** 2026-05-26

---

## 1. Backend-Only Operations

The following operations are enforced as server-side only. No client-side path exists to perform these actions.

| Operation | Function | Why backend-only |
|---|---|---|
| Create user account | Admin SDK (registration flow) | Server sets initial account state |
| Update user status (suspend/ban) | Admin SDK | Prevents self-unsuspension |
| Create/update profile | Admin SDK | Server-sourced identity fields |
| Follow user | `followUser()` | Atomic counter updates + privacy check |
| Unfollow user | `unfollowUser()` | Atomic counter decrements |
| Send follow request | `sendFollowRequest()` | State machine + counter updates |
| Accept/deny/cancel request | `acceptFollowRequest()` etc. | State transitions + cascading updates |
| Block user | `blockUser()` | 7-read cascade: block + unfollows + request invalidation |
| Unblock user | `unblockUser()` | Ownership-verified block removal |
| Create post (recommended) | `createPost()` | Server-sourced author identity from DB |
| Delete post | `deletePost()` | Ownership check + async attachment purge |
| Record profile visit | `recordProfileVisit()` | Deduplication window + block check |
| Get wallet balance | `getWalletBalance()` | Server-computed balance from snapshot |
| Spend coins | `spendCoins()` | Ledger integrity, idempotency, fraud check |
| Get transactions | `getWalletTransactions()` | Server-side pagination |
| Initiate purchase | `initiatePurchase()` | Provider integration, fraud check |
| Verify purchase | `verifyPurchase()` | Provider verification, credit flow |
| Admin wallet adjustment | `adminRequestWalletAdjustment()` | RBAC + maker/checker |
| Approve wallet adjustment | `adminApproveWalletAdjustment()` | RBAC + self-approval prevention |
| Set custom claims | Admin SDK | Role management (super_admin only) |
| Write audit logs | `AuditLogService` | Append-only, sanitized |
| Write fraud signals | `FraudSignalService` | Server-computed risk scoring |
| Enforce storage quotas | `enforceStorageQuotas` trigger | Auto-suspend on breach |

## 2. Security Envelope

Every callable function follows this sequence:

```
1. requireAppCheck(context)     — reject if no valid App Check token
2. requireAuth(context)         — reject if no valid auth; extract callerUid
3. validateInput(data)          — reject if schema invalid; reject extra fields
4. checkRateLimit(callerUid)    — reject if rate exceeded (fail-closed for economic)
5. Business logic               — transactions, ownership checks, writes
6. Audit log                    — record sensitive operations
```

## 3. Security Controls by Function

### Social Graph Functions
- All enforce: AppCheck + Auth + Input validation + Rate limit
- Block check before operation (bidirectional)
- Idempotency: silent no-op on duplicate (follow already exists, etc.)
- Counter clamping: `Math.max(0, count - 1)` prevents negative counters
- Transaction discipline: all multi-document operations are atomic

### Post Functions
- `createPost()`: Author identity fetched from DB (never from client input)
- `deletePost()`: Ownership verified in transaction before delete
- `onPostDeleted` trigger: Path validation (`isSafeStorageSegment()`) before GCS operations
- Attachment purge: best-effort, logged on failure

### Wallet Functions
- Fail-closed rate limiting for all economic operations
- Fraud risk assessment before every spend/purchase
- Optimistic locking with version field
- Idempotency key with request hash verification
- Spend priority: promotional → earned → purchased
- Auto-freeze on risk threshold breach

### Admin Functions
- RBAC with closed-set roles: `super_admin`, `admin`, `support`
- Maker/checker pattern: requester ≠ approver
- Self-approval prevention (throws FRAUD_DETECTED)
- Low-value self-execute allowed only with explicit flag + permission
- All actions audit-logged with justification

## 4. Error Handling

- Client receives generic Firebase error codes (`unauthenticated`, `permission-denied`, `invalid-argument`, `resource-exhausted`, `already-exists`, `not-found`)
- No internal paths, IDs, or state leaked in error messages
- Detailed errors logged server-side for debugging

## 5. Audit Logging

- Service: `AuditLogService` with `record()` and `recordInTx()` methods
- Sanitization: recursive redaction of `token|secret|password|credential|apikey|authorization|sessionid`
- Storage: `wallet_audit_logs` collection (deny all client access)
- Append-only: no update/delete API exposed

## 6. Recommendations

1. **Enable admin 2FA** before production admin operations
2. **Enable IP allowlist** for admin functions
3. **Implement webhook signature validation** before real payment integration
4. **Schedule reconciliation job** for daily wallet integrity checks
5. **Add structured logging** with PII filters for production Cloud Logging
