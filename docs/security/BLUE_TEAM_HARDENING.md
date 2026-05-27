# Blue Team Hardening

**Date:** 2026-05-26
**Scope:** Defensive controls, monitoring, incident response, operational security

---

## 1. App Check Enforcement

### Current state
- Android debug: `DebugAppCheckProviderFactory`
- Android release: `PlayIntegrityAppCheckProviderFactory`
- iOS: expected DeviceCheck/App Attest (not deeply audited)
- Firebase Console: assumed audit mode

### Action items
1. Switch Firebase Console App Check to **enforcement mode** before production
2. Verify iOS provider configuration for App Attest (iOS 14+)
3. Register debug tokens for all developer devices in Firebase Console
4. Monitor App Check metrics weekly during initial rollout

---

## 2. Monitoring & Alerts

### 2.1 Firestore monitoring
| Metric | Threshold | Action |
|---|---|---|
| Reads per minute (project) | > baseline x 5 | Investigate cost amplification |
| Writes per minute (project) | > baseline x 5 | Investigate spam/abuse |
| Document count per collection | Anomalous growth | Investigate data injection |
| `permission-denied` rate | > 5% of requests | Investigate attack or client bug |
| Rule evaluation time | p95 > 100ms | Optimize complex rules |

### 2.2 Storage monitoring
| Metric | Threshold | Action |
|---|---|---|
| Upload rate per user | > 20/min | Auto-suspended (already enforced) |
| Storage usage per user | > 400 MB | Notify user; > 500 MB auto-suspend |
| Project storage growth | > 10 GB/day | Investigate |
| Bandwidth (egress) | > baseline x 3 | Investigate URL leakage |

### 2.3 Cloud Functions monitoring
| Metric | Threshold | Action |
|---|---|---|
| Error rate per function | > 1% | Investigate |
| p95 latency per function | > 2s | Optimize |
| Cold start rate | > 20% | Consider min instances |
| `unauthenticated` rate | > 5% | Investigate attack |
| Rate limit hits per UID | Spike | Investigate compromised account |

### 2.4 Auth monitoring
| Metric | Threshold | Action |
|---|---|---|
| Failed login rate | > 100/min | Investigate brute force |
| New account rate | > baseline x 10 | Investigate bot signup |
| Suspended account count | Growth spike | Investigate fraud |

---

## 3. Audit Logs

### Current implementation
- `AuditLogService` writes to `wallet_audit_logs` and `admin_actions`
- Append-only (no update/delete API)
- Sensitive keys redacted: `token|secret|password|credential|apikey|authorization|sessionid`
- Recursive sanitization for nested objects

### Retention policy (recommended)
- Wallet audit logs: 7 years (financial compliance)
- Admin actions: 5 years
- General audit events: 1 year
- Cloud Logging: 30 days standard, longer for security events

### Access control
- Cloud Logging IAM: only `roles/logging.privateLogViewer` for security team
- Wallet audit logs in Firestore: deny all client access (already enforced)

---

## 4. IAM Review

### Service accounts
- **Cloud Functions runtime service account**
  - Should have: Firestore Admin, Storage Admin, App Engine default
  - Should NOT have: Project Owner, Project Editor (use granular roles)
  - Review quarterly

- **Firebase Admin service account** (if exported for CI)
  - Treat as critical secret
  - Rotate every 90 days
  - Store in Google Secret Manager, not in code or env files
  - Audit usage via Cloud Audit Logs

### Recommendations
1. Apply principle of least privilege
2. Use separate service accounts for dev/staging/prod
3. Enable IAM Recommender for unused permission detection
4. Set up alerts for IAM policy changes

---

## 5. Project Separation

### Recommended
- `mindos-dev` — local development, ephemeral data
- `mindos-staging` — pre-production testing, sanitized data
- `mindos-prod` — production, real users

### Enforcement
- Different Firebase project IDs
- Different service accounts
- Different App Check provider keys
- Different Firebase Auth user pools

### Action items
1. Create `.firebaserc` with project aliases
2. Document project selection in deployment runbook
3. Add CI guard to prevent accidental prod deployment

---

## 6. Backups

### Current state
No documented backup policy.

### Recommended policy
1. **Firestore daily export** to Cloud Storage bucket
   - Bucket location: separate region from primary
   - Lifecycle: 30-day standard, 1-year archive
   - Encryption: Google-managed keys (default) or CMEK for sensitive data
2. **Storage bucket versioning** enabled
3. **Restore drill** quarterly — verify backups are usable
4. **Document RPO/RTO** in incident response playbook

### Action items
1. Set up daily Firestore export via Cloud Scheduler + CF
2. Enable Storage bucket versioning
3. Create backup-restore runbook
4. Schedule first restore drill

---

## 7. Rollback Procedures

### Firestore rules rollback
```bash
# Tag current rules
git tag rules-pre-deploy-$(date +%Y%m%d)
git push origin --tags

# Deploy new rules
firebase deploy --only firestore:rules --project mindos-prod

# If issue:
git checkout rules-pre-deploy-YYYYMMDD -- firestore.rules
firebase deploy --only firestore:rules --project mindos-prod
```

### Cloud Functions rollback
```bash
# Functions support traffic splitting in Cloud Run
# Roll back via Firebase Console or:
firebase functions:rollback --project mindos-prod
```

### Storage rules rollback
Same pattern as Firestore rules.

### Action items
1. Document rollback procedure in runbook
2. Practice rollback during quarterly drill

---

## 8. Cost Protection

### Firebase budget alerts
1. Set monthly budget (e.g., $500 dev, $5000 prod)
2. Alert thresholds: 50%, 90%, 100%, 150%
3. Notify: security team, finance, on-call

### Quota limits
1. Firestore: review default quotas, request increases proactively
2. Cloud Functions: set max instances per function
3. Storage: enforced per-user via CF (already implemented)

### Anomaly detection
1. Enable Google Cloud anomaly detection
2. Set up custom dashboards for cost metrics
3. Review weekly during initial rollout

---

## 9. Security Review Cadence

| Activity | Frequency |
|---|---|
| Rules review | Monthly |
| Threat model update | Quarterly |
| Penetration test (external) | Annually + before major launches |
| Dependency audit | Weekly (automated) |
| Secret scan | On every commit (automated) |
| Admin role audit | Quarterly |
| IAM permission review | Quarterly |
| Backup restore drill | Quarterly |
| Incident response drill | Annually |

---

## 10. Pre-deploy Review Flow

```
1. Developer creates PR with rule changes
2. Automated checks:
   - npm run scan:secrets
   - npm run typecheck
   - npm test (Cloud Functions)
   - npm run test:rules (Firestore + Storage rules)
3. Security reviewer required for:
   - Changes to firestore.rules
   - Changes to storage.rules
   - Changes to functions/src/lib/auth*.ts
   - Changes to functions/src/lib/adminAuth.ts
   - Changes to functions/src/lib/auditLogService.ts
4. Merge to main triggers staging deploy
5. Smoke test in staging
6. Manual approval for prod deploy
7. Deploy with feature flag (if applicable)
8. Monitor for 1 hour post-deploy
```

---

## 11. Incident Response Checklist

### Detection
1. Alert triggered (monitoring, user report, audit log review)
2. Acknowledge alert within 15 minutes
3. Severity classification: P0 (data breach), P1 (active attack), P2 (suspicious activity), P3 (anomaly)

### Containment
1. Revoke compromised auth tokens (`revokeRefreshTokens()`)
2. Suspend affected user accounts (set `status: "Suspended"`)
3. Tighten relevant Firestore rules if needed
4. Block IP at Cloud Armor if available

### Eradication
1. Identify root cause
2. Patch vulnerability
3. Deploy fix to staging, verify, deploy to prod

### Recovery
1. Restore affected data from backup if needed
2. Verify integrity via reconciliation
3. Notify affected users per privacy policy/regulations

### Lessons learned
1. Post-mortem within 5 business days
2. Update threat model
3. Add detection/prevention controls
4. Update runbooks

---

## 12. Secrets Management

### Current implementation
- `.env` files gitignored
- `.env.example` documents knobs only (no real values)
- `scripts/scan-secrets.js` scans for hardcoded secrets pre-commit/deploy
- 7 secret patterns scanned: Google API keys, private keys, GitHub tokens, AWS, Stripe, Slack, Firebase DB secrets

### Production recommendations
1. Use **Google Secret Manager** for all production secrets
2. Access via Cloud Functions runtime config (`firebase functions:config:set`)
3. Rotate secrets every 90 days
4. Audit secret access via Cloud Audit Logs
5. Use service accounts with minimum required Secret Manager permissions

---

## 13. CI/CD Security

### Current state
- GitHub Actions for testing
- Pre-deploy hooks: scan-secrets → typecheck → test → build

### Hardening recommendations
1. **OIDC for GitHub → GCP** (no long-lived service account keys)
2. **Branch protection** on `main`: require reviews, status checks
3. **Signed commits** for sensitive paths (rules, auth code)
4. **Dependabot** for dependency updates
5. **CodeQL** for static analysis
6. **Secret scanning** at GitHub level (in addition to local scan)

---

## 14. Service Account Restrictions

### Cloud Functions service account
- Default: project's App Engine default service account
- **Recommended:** Custom service account per function group:
  - `mindos-social-fn@` — Firestore read/write to social collections only
  - `mindos-wallet-fn@` — Firestore read/write to wallet collections only
  - `mindos-admin-fn@` — Firestore admin (broader access)
- Apply IAM conditions: time-bound, IP-bound for admin
- Audit usage via Cloud Audit Logs

---

## 15. Compromised User Recovery

### Procedure
1. **Detect:** Anomalous activity (fraud signals, audit log review, user report)
2. **Suspend:** Set `users/{uid}.status = "Suspended"` via Admin SDK
3. **Revoke:** Call `revokeRefreshTokens(uid)` to invalidate all sessions
4. **Investigate:** Review audit logs for affected scope
5. **Notify:** Email user via Firebase Auth notification
6. **Restore:** Reset password, re-verify identity
7. **Reactivate:** Set status back to `Active`

### Action items
1. Document procedure as runbook
2. Add admin CF endpoint for one-click suspension
3. Add audit log query for "affected users in last X hours"
