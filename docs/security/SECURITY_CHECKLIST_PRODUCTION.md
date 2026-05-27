# Production Security Checklist

**Date:** 2026-05-26
**Use:** Before any deployment to production Firebase project.

---

## Pre-Deploy Checklist

### Rules & Code
- [ ] All Firestore rules tests pass (`npm run test:rules`)
- [ ] All Cloud Functions tests pass (`cd functions && npm test`)
- [ ] All KMP client tests pass (`./gradlew :shared:desktopTest`)
- [ ] No permissive rules (no `allow read, write: if true`)
- [ ] No broad `allow ... if request.auth != null` without further checks
- [ ] Catch-all deny present (`match /{document=**} { allow read, write: if false; }`)
- [ ] All `list` queries enforce `request.query.limit`
- [ ] Server-only collections deny all client writes

### Configuration
- [ ] App Check configured for Android (Play Integrity)
- [ ] App Check configured for iOS (App Attest)
- [ ] App Check Firebase Console set to **enforcement mode**
- [ ] Firebase Auth providers configured correctly
- [ ] Custom claims reviewed (`admin_role` set only for authorized accounts)
- [ ] IAM policies reviewed (least privilege)
- [ ] Service accounts have minimum required permissions

### Secrets & Config
- [ ] No secrets hardcoded (`npm run scan:secrets` passes)
- [ ] `.env` files NOT committed (verify `.gitignore`)
- [ ] `.env.example` present, no real values
- [ ] Production secrets stored in Google Secret Manager
- [ ] Service account keys rotated within 90 days
- [ ] `.firebaserc` set to correct project

### Indices & Queries
- [ ] All Firestore queries have matching indices
- [ ] `firestore.indexes.json` reviewed and deployed
- [ ] No queries return more than `limit` documents

### Storage
- [ ] Storage rules deny dangerous MIME types (HTML, SVG, JS, EXE)
- [ ] Storage quotas enforced via `enforceStorageQuotas` CF
- [ ] Default file visibility is private
- [ ] No `application/octet-stream` allowed (except justified WAV variants)

### Backend
- [ ] All Cloud Functions enforce `requireAppCheck()`
- [ ] All Cloud Functions enforce `requireAuth()`
- [ ] All Cloud Functions validate input schema
- [ ] Economic functions use `checkRateLimitFailClosed()`
- [ ] Admin functions use `requireAdmin(permission)`
- [ ] All write operations are transactional where needed
- [ ] Audit logs sanitize sensitive keys

### Operational
- [ ] Backups configured (daily Firestore export)
- [ ] Backup restore drill performed within 90 days
- [ ] Monitoring dashboards set up
- [ ] Budget alerts configured (50%, 90%, 100%, 150%)
- [ ] Incident response runbook reviewed
- [ ] On-call rotation defined
- [ ] Rollback procedure documented and tested

### Documentation
- [ ] `SECURITY_AUDIT_INITIAL.md` reflects current state
- [ ] `THREAT_MODEL.md` reviewed within 3 months
- [ ] `DATA_INVENTORY.md` up to date
- [ ] `BACKEND_SECURITY.md` reflects current CFs
- [ ] Residual risks explicitly accepted by stakeholder

### Sandbox / Real-money gate
- [ ] If real-money: webhook signature validation implemented
- [ ] If real-money: admin 2FA enabled
- [ ] If real-money: admin IP allowlist enabled
- [ ] If real-money: reconciliation job scheduled
- [ ] If real-money: PCI compliance reviewed (if storing card data)
- [ ] If real-money: external penetration test passed

### Deployment
- [ ] Staging deploy successful
- [ ] Staging smoke tests pass
- [ ] Rules deployed in dry-run mode first (where possible)
- [ ] Feature flags configured if rollout is gradual
- [ ] Rollback plan defined
- [ ] Communication plan ready (status page, customer notice if needed)

---

## Post-Deploy Monitoring (first 24 hours)

- [ ] Cloud Functions error rate < 1%
- [ ] Firestore `permission-denied` rate < 5%
- [ ] Storage upload rate normal
- [ ] No unusual auth failure patterns
- [ ] App Check token verification success rate > 99%
- [ ] No budget alerts triggered
- [ ] No audit log anomalies
