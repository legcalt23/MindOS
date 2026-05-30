# Cloud Functions hardening — MindOS

This document is the Firebase-stack equivalent of the "Docker hardening"
phase of the plan. MindOS does not use containers we operate ourselves — the
backend runs on **Google Cloud Functions**, fronted by **Firebase**.
Hardening is therefore expressed as runtime config, source-tree hygiene,
and supply-chain controls.

## Stack reality

| Plan term         | MindOS equivalent                                    |
|-------------------|---------------------------------------------------|
| Dockerfile        | `functions/package.json` + `functions/tsconfig.json` |
| Container image   | Cloud Functions runtime, managed by Google        |
| Compose           | `firebase emulators` (see `local-development.md`) |
| Read-only FS      | Functions are stateless; FS is read-only by design |
| Healthcheck       | Functions liveness handled by GCP                 |
| Process user      | Non-root by default on managed runtime            |

What we DO control on this stack:
- Runtime version (Node)
- Dependency tree (`package.json` + `package-lock.json`)
- Source-tree contents that ship to the runtime
- Deploy ignore list (`firebase.json` `functions.ignore`)
- App Check + Auth + Firestore Rules (separate hardening docs)

## Controls applied in this phase

### 1. Node runtime pinned

`functions/package.json` declares `"engines": { "node": "18" }`. The Cloud
Functions runtime is pinned to Node 18. We do NOT use `node: "*"` or `latest`.

### 2. Strict TypeScript

`functions/tsconfig.json` enables:
- `"strict": true`
- `"noImplicitReturns": true`
- `"noUnusedLocals": true`

`npm run typecheck` runs `tsc --noEmit` and is a CI gate.

### 3. Secret-tree hygiene

`scripts/scan-secrets.js` scans the function source and rule files for known
secret patterns (Google API keys, private keys, GitHub tokens, AWS keys,
Stripe live keys, Slack tokens, Firebase DB secrets).

- Run locally: `npm run scan:secrets`
- Run from CI: same command
- Exit code 1 if any pattern matches
- Honors `// FAKE_SECRET_FIXTURE` for unit-test data

Tests for the scanner itself live in `src/infra-secret-scan.test.ts` and
verify each pattern triggers, the allow-tag is honored, and clean content
yields zero findings.

### 4. gitignore lockdown

`.gitignore` (root) and `functions/.gitignore` deny:
- `.env*` (with explicit allow for `.env.example`)
- Firebase service-account JSON files (multiple patterns)
- `*.pem`, `*.key`, `*_rsa*`, `*.p12`, `*.pfx`
- `*.log` + Firebase debug logs
- `lib/`, `coverage/`, `node_modules/`

### 5. Deploy surface restriction

`firebase.json` `functions.ignore` already excludes `node_modules`, `.git`,
and Firebase debug logs from the deployed bundle. The deployed surface is:
- `src/`
- `lib/` (the compiled output, produced fresh by `tsc`)
- `package.json` / `package-lock.json`

### 6. Dependency audit

`npm run audit:deps` runs `npm audit --omit=dev --audit-level=high`. CI
fails on high/critical advisories for production deps. (Dev-only deps are
not deployed, so they are excluded.)

### 7. Aggregate verify command

`npm run verify` runs, in order:
1. `scan:secrets`
2. `typecheck`
3. `test`

This is the single command CI uses to gate PRs.

## Things explicitly OUT of scope here

- Real container hardening (no containers we manage)
- CIS Docker Benchmark items (N/A)
- Read-only root FS / capability drops (managed by GCP)
- Healthcheck endpoints (managed by GCP)
- Multi-stage Docker build optimization (N/A)

## Smoke test

A clean run of:

```
cd functions
npm install
npm run verify
```

…must finish with zero secret findings, zero TypeScript errors, and a
green test suite.
