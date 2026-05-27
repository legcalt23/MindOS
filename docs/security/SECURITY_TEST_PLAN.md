# Security Test Plan

**Date:** 2026-05-26

---

## 1. Test Suites

### 1.1 Firestore Rules — Existing (`firestore.username.rules.test.mjs`)
Covers username claims, profile visuals, posts, DMs, feeds, blocks, privacy.
**Status:** Passing

### 1.2 Firestore Rules — Security Audit (`firestore.security.rules.test.mjs`)
Comprehensive security-focused tests added by this audit.
**Status:** Created

### 1.3 Storage Rules — Existing (`storage.dm.rules.test.mjs`)
DM media upload/download rules.
**Status:** Passing

### 1.4 Cloud Functions — Existing (`functions/src/*.test.ts`)
704 tests covering wallet, admin, RBAC, audit, social graph, payments.
**Status:** Passing

### 1.5 KMP Client — Existing (`:shared:desktopTest`)
165 tests covering input validation, social graph, auth, publishing.
**Status:** Passing

---

## 2. Security Test Cases — `firestore.security.rules.test.mjs`

### Section 1: Unauthenticated access denied
| # | Test | Expected |
|---|---|---|
| T1 | Unauth cannot read privacy doc | DENY |
| T2 | Unauth cannot read user account | DENY |
| T3 | Unauth cannot read profile_visuals | DENY |
| T4 | Unauth cannot write to privacy | DENY |
| T5 | Unauth cannot write to profile_visuals | DENY |
| T6 | Unauth cannot read usernames | DENY |

### Section 2: Cross-user access denied
| # | Test | Expected |
|---|---|---|
| T7 | Bob cannot read Alice's privacy | DENY |
| T8 | Bob cannot read Alice's user account | DENY |
| T9 | Bob cannot read private profile_visual | DENY |
| T10 | Bob cannot write Alice's privacy | DENY |
| T11 | Bob cannot write Alice's profile_visual | DENY |

### Section 3: Immutable field protection
| # | Test | Expected |
|---|---|---|
| T12 | Cannot update userId | DENY |
| T13 | Cannot update createdAt | DENY |
| T14 | Cannot update schemaVersion | DENY |
| T15 | Cannot update layoutVersion | DENY |
| T16 | Cannot update observersCount directly | DENY |
| T17 | Cannot update usernameSearchTokens | DENY |

### Section 4: Mass assignment protection
| # | Test | Expected |
|---|---|---|
| T18 | Cannot add isAdmin field | DENY |
| T19 | Cannot add role field | DENY |
| T20 | Cannot add isPremium field | DENY |
| T21 | Cannot add billingStatus field | DENY |
| T22 | Cannot add permissions field | DENY |
| T23 | Cannot add securityFlags field | DENY |

### Section 5: Server-only collections
| # | Test | Expected |
|---|---|---|
| T24 | Cannot write to users | DENY |
| T25 | Cannot write to follows | DENY |
| T26 | Cannot write to blocks | DENY |
| T27 | Cannot write to counters | DENY |
| T28 | Cannot write to followRequests | DENY |
| T29 | Cannot write to auditEvents | DENY |
| T30 | Cannot read auditEvents | DENY |

### Section 6: Wallet collections fully denied
| # | Test | Expected |
|---|---|---|
| T31-T44 | Write/read denied for wallets, balance_snapshots, ledger_transactions, ledger_entries, idempotency_keys, purchases, fraud_signals, wallet_audit_logs, admin_actions | DENY |

### Section 7: Wallet owner-read allowed
| # | Test | Expected |
|---|---|---|
| T45 | Owner can read own wallet | ALLOW |
| T46 | Owner can read own balance_snapshot | ALLOW |
| T47 | Bob cannot read Alice's wallet | DENY |
| T48 | Bob cannot read Alice's balance_snapshot | DENY |

### Section 8: Bounded list queries
| # | Test | Expected |
|---|---|---|
| T49 | DM chats list with limit succeeds | ALLOW |
| T50 | DM messages list with limit succeeds | ALLOW |
| T51 | Feed list with limit succeeds | ALLOW |
| T52 | User posts list with limit succeeds | ALLOW |

### Section 9: Unknown collections catch-all
| # | Test | Expected |
|---|---|---|
| T53 | Cannot read unknown collection | DENY |
| T54 | Cannot write unknown collection | DENY |
| T55 | Unauth denied on unknown collection | DENY |
| T56 | Cannot read unknown subcollection | DENY |

### Section 10: Privacy validation
| # | Test | Expected |
|---|---|---|
| T57 | Owner can create privacy doc | ALLOW |
| T58 | Cannot create other user's privacy | DENY |
| T59 | Invalid visibility rejected | DENY |
| T60 | Missing required fields rejected | DENY |

### Section 11: Delete operations
| # | Test | Expected |
|---|---|---|
| T61 | Cannot delete privacy doc | DENY |
| T62 | Cannot delete profile_visuals | DENY |
| T63 | Cannot delete notifications | DENY |

### Section 12: DM participant isolation
| # | Test | Expected |
|---|---|---|
| T64 | Non-participant cannot read DM chat | DENY |
| T65 | Non-participant cannot read DM messages | DENY |
| T66 | Non-participant cannot write DM messages | DENY |
| T67 | Cannot modify DM participants | DENY |

### Section 13: Notification update restrictions
| # | Test | Expected |
|---|---|---|
| T68 | Recipient can mark notification read | ALLOW |
| T69 | Cannot modify notification type | DENY |
| T70 | Cannot modify notification actorUserId | DENY |

---

## 3. Execution

```bash
# All rules tests (requires Firebase Emulator)
npm run test:rules

# Security audit tests only
npm run test:security-rules

# Cloud Functions tests
cd functions && npm test

# KMP client tests
./gradlew :shared:desktopTest
```

---

## 4. Coverage Assessment

| Area | Tests | Coverage |
|---|---|---|
| Unauthenticated access | 6 | Complete |
| Cross-user access | 5 | Complete |
| Immutable fields | 6 | Complete |
| Mass assignment | 6 | Complete |
| Server-only collections | 7 | Complete |
| Wallet collections | 14 | Complete |
| Wallet owner reads | 4 | Complete |
| Bounded lists | 4 | Complete |
| Catch-all deny | 4 | Complete |
| Privacy validation | 4 | Complete |
| Delete restrictions | 3 | Complete |
| DM isolation | 4 | Complete |
| Notification restrictions | 3 | Complete |
| **Total new tests** | **70** | |
