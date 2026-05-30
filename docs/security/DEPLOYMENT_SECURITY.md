# Deployment Security Guide

**Date:** 2026-05-26

---

## 1. Prerequisites

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login
firebase login

# Verify project access
firebase projects:list
```

Create `.firebaserc` (not currently committed):

```json
{
  "projects": {
    "default": "mindos-dev",
    "staging": "mindos-staging",
    "production": "mindos-prod"
  }
}
```

---

## 2. Local Development with Emulators

```bash
# Start emulators (Firestore, Storage, Auth, Functions)
firebase emulators:start

# Or specific emulators:
firebase emulators:start --only firestore,storage

# Emulator UI: http://localhost:4000
```

Set environment variables in `.env`:
```
FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080
FIREBASE_STORAGE_EMULATOR_HOST=127.0.0.1:9199
FUNCTIONS_EMULATOR_HOST=127.0.0.1:5001
```

---

## 3. Running Tests

### Firestore Rules tests
```bash
# Existing username tests
npm run test:firestore-rules

# Security audit tests (added in this audit)
npm run test:security-rules

# All rules tests
npm run test:rules
```

### Storage Rules tests
```bash
npm run test:storage-rules
```

### Cloud Functions tests
```bash
cd functions
npm test                    # All tests
npm run verify              # Scan secrets + typecheck + test
```

### KMP Client tests
```bash
./gradlew :shared:desktopTest
./gradlew :shared:allTests   # All KMP targets
```

---

## 4. Deploying Firestore Rules

```bash
# Dry run (validates rules without deploying)
firebase deploy --only firestore:rules --project staging --dry-run

# Deploy to staging first
firebase deploy --only firestore:rules --project staging

# Smoke test in staging
# Then deploy to production
firebase deploy --only firestore:rules --project production
```

---

## 5. Deploying Storage Rules

```bash
firebase deploy --only storage --project staging
# Smoke test
firebase deploy --only storage --project production
```

---

## 6. Deploying Indices

```bash
firebase deploy --only firestore:indexes --project staging
# Wait for index build (can take minutes to hours)
firebase deploy --only firestore:indexes --project production
```

Indices must be deployed **before** rules that depend on them.

---

## 7. Deploying Cloud Functions

```bash
cd functions

# Pre-deploy gates (run automatically via firebase.json predeploy hook):
# - scan:secrets
# - typecheck
# - test
# - build

# Deploy specific function (recommended for staged rollouts)
firebase deploy --only functions:followUser --project staging

# Deploy all functions
firebase deploy --only functions --project staging

# Verify in staging
# Then production
firebase deploy --only functions --project production
```

---

## 8. Full Deploy Sequence

```bash
# 1. Run all tests
npm run test:rules
cd functions && npm run verify && cd ..
./gradlew :shared:desktopTest

# 2. Tag current state
git tag pre-deploy-$(date +%Y%m%d-%H%M)
git push origin --tags

# 3. Deploy to staging
firebase deploy --project staging

# 4. Smoke test staging (manual or automated)

# 5. Deploy to production
firebase deploy --project production

# 6. Post-deploy monitoring (24 hours)
```

---

## 9. Rollback Procedures

### Rollback Firestore rules
```bash
# Find the previous rules version
git log --all --oneline firestore.rules

# Checkout previous version
git checkout <commit-hash> -- firestore.rules

# Deploy rollback
firebase deploy --only firestore:rules --project production

# Restore working tree
git checkout HEAD -- firestore.rules
```

### Rollback Storage rules
Same pattern as Firestore rules.

### Rollback Cloud Functions
```bash
# Via Firebase Console:
# Functions → select function → Versions → Rollback to previous

# Or via gcloud:
gcloud functions describe FUNCTION_NAME --region=REGION
gcloud functions rollback FUNCTION_NAME --region=REGION
```

### Rollback indices
Indices cannot be "rolled back" — they must be deleted and recreated. If a new index causes issues, delete it via Firebase Console.

---

## 10. Post-Deploy Verification

### Within first 5 minutes
- [ ] Firebase Console → Functions: all functions deployed successfully
- [ ] Firebase Console → Firestore: rules version updated
- [ ] Firebase Console → Storage: rules version updated
- [ ] Smoke test: authenticate, perform key operation, verify expected behavior

### Within first hour
- [ ] Monitor Cloud Functions error rate (< 1%)
- [ ] Monitor Firestore `permission-denied` rate (< 5%)
- [ ] Monitor Auth failure rate (no spike)
- [ ] Check audit logs for anomalies

### Within first 24 hours
- [ ] No budget alerts triggered
- [ ] No security alerts triggered
- [ ] User feedback (if applicable)
- [ ] Performance metrics stable

---

## 11. Emergency Procedures

### Active attack detected
1. **Tighten rules immediately** — push restrictive rule version
2. **Suspend affected users** — Cloud Function: `suspendUser(uid)`
3. **Revoke tokens** — `admin.auth().revokeRefreshTokens(uid)`
4. **Alert team** — page on-call
5. **Block at edge** — Cloud Armor IP block if available
6. **Document** — incident response runbook

### Data corruption detected
1. **Stop writes** — deploy temporary deny-all rule
2. **Investigate** — audit logs, ledger reconciliation
3. **Restore from backup** — Firestore export from Cloud Storage
4. **Verify integrity** — reconciliation check
5. **Re-enable writes** — restore production rules

### Cost spike detected
1. **Investigate** — Firebase Console → Usage and billing
2. **Identify source** — Cloud Logging filter by function/collection
3. **Mitigate** — tighten rate limits, suspend abusive accounts
4. **Communicate** — notify finance/leadership

---

## 12. Pre-Production Deployment Gates (Real-money)

Before enabling real payment processing, all of these must be completed:

1. ✅ Webhook signature validation implemented and tested
2. ✅ Admin 2FA enabled (Authy/TOTP)
3. ✅ Admin IP allowlist configured
4. ✅ Daily reconciliation job scheduled
5. ✅ Backup policy documented and drilled
6. ✅ External penetration test passed
7. ✅ PCI compliance reviewed (if storing card data)
8. ✅ Real payment provider integration verified in staging
