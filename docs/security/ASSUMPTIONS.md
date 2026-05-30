# Security Assumptions

**Date:** 2026-05-26
**Project:** MindOS

These assumptions were made during the security audit. Each is restrictive by default — if reality differs, the security posture is equal or stronger than assumed.

---

## A1 — Single-tenant user model
**Assumption:** Each user owns exactly one account. There is no multi-tenant organization model (no `orgId`, no org membership hierarchy).
**Basis:** No organization-related collections or fields found in Firestore rules or KMP code.
**Security implication:** Cross-tenant access controls (org isolation) are not applicable. User isolation is enforced per-UID.

## A2 — No admin UI exists
**Assumption:** Admin operations are performed exclusively via Cloud Functions callable endpoints (`adminRequestWalletAdjustment`, `adminApproveWalletAdjustment`), not through a web admin panel.
**Basis:** No admin dashboard code found in the repository.
**Security implication:** Admin RBAC is enforced server-side via custom claims. No client-side admin privilege escalation surface exists.

## A3 — Firebase Auth is the sole identity provider
**Assumption:** All authentication flows use Firebase Auth (email/password). No SSO, OAuth, or SAML providers are configured.
**Basis:** `SessionRepository` only exposes email/password methods. No social login code found.
**Security implication:** Token security depends on Firebase Auth infrastructure. Custom claims are set server-side only.

## A4 — Wallet is sandbox-only
**Assumption:** The virtual currency system operates with sandbox coins that have no real monetary value. Real payment provider integration is not yet active.
**Basis:** `PAYMENT_PROVIDER=sandbox` in `.env.example`. Webhook handlers fail-closed (403) for real providers. Release gate document explicitly blocks real-money operations.
**Security implication:** Financial fraud risk is limited to virtual currency abuse. Real payment security (signature validation, PCI compliance) is deferred.

## A5 — No server-side rendering or web frontend
**Assumption:** The application is mobile/desktop only. There is no web frontend serving HTML.
**Basis:** Only Android, Desktop (JVM), and iOS targets found in `settings.gradle.kts`.
**Security implication:** XSS, CSRF, and web-specific attacks are not applicable to the client. Storage rules still block HTML/JS/SVG uploads as defense-in-depth.

## A6 — Firebase project separation exists
**Assumption:** Development, staging, and production use separate Firebase projects (or at minimum, separate emulator instances for development).
**Basis:** `.env.example` references emulator hosts. No `.firebaserc` found, suggesting project alias is not committed.
**Security implication:** If dev and prod share a project, rule deployments could affect production. Separate projects are strongly recommended.

## A7 — Custom claims are set by trusted processes only
**Assumption:** Firebase Auth custom claims (`admin_role`, etc.) are set exclusively by trusted server-side processes (Cloud Functions with Admin SDK or Firebase Console).
**Basis:** `adminAuth.ts` reads claims from `context.auth.token`; no client-side claim-setting code found.
**Security implication:** Role escalation via client is impossible as long as no Cloud Function allows arbitrary claim modification.

## A8 — App Check is in audit mode (not enforcement)
**Assumption:** Firebase App Check is configured but may be in audit mode (logging violations without blocking requests).
**Basis:** Release gate document lists "App Check enforcement-mode in production" as a pre-production requirement.
**Security implication:** In audit mode, requests from modified apps are logged but not blocked at the Firebase level. Cloud Functions still enforce `requireAppCheck()` independently.

## A9 — DM conversations are 1-to-1 only
**Assumption:** Direct messaging supports exactly 2 participants per conversation. Group chats are not supported.
**Basis:** `validDMParticipants()` enforces `participants.size() == 2`. Chat ID format encodes exactly two UIDs.
**Security implication:** Multi-party access control complexity is avoided. Each conversation has exactly two authorized participants.

## A10 — Post creation is dual-write (client rules + Cloud Function)
**Assumption:** Posts can be created both via client-side Firestore write (validated by rules) and via the `createPost` Cloud Function. The Cloud Function provides additional server-side validation (author identity from DB, suspension check).
**Basis:** Both `firestore.rules` and `functions/src/posts.ts` define post creation logic.
**Security implication:** The Cloud Function is the recommended path (server-sourced author identity). Client-side post creation relies on `validPostAuthorIdentity()` which checks against the existing `profile_visuals` document.

## A11 — No file scanning/antivirus
**Assumption:** Uploaded files are not scanned for malware. Storage rules validate MIME type and size but cannot detect malicious content within allowed file types.
**Basis:** No Cloud Function trigger for malware scanning found. Storage quota enforcement exists but doesn't scan content.
**Security implication:** A valid JPEG could contain steganographic data or exploit image parser vulnerabilities. This is a residual risk documented in the threat model.

## A12 — iOS target exists but is not audited
**Assumption:** iOS-specific code exists but the security audit focuses on shared (commonMain), Android, and Desktop targets. iOS-specific App Check configuration (DeviceCheck/App Attest) is assumed to follow the same pattern as Android (Play Integrity).
**Basis:** iOS source files exist in `iosMain` but were not deeply inspected in this audit.
**Security implication:** iOS-specific vulnerabilities (if any) are not covered by this audit.
