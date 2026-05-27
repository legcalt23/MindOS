# Local development — Firebase emulators

This is the Firebase-stack equivalent of the "docker-compose" phase. MindOS has
no PostgreSQL/Redis services to orchestrate — the local environment is
**Firebase Emulators**, which run all backend services in-process for
deterministic local testing.

## What you need installed

| Tool          | Version   | Why                            |
|---------------|-----------|--------------------------------|
| Node.js       | 18.x      | Matches `engines.node` in `functions/package.json` |
| npm           | 9.x+      | bundled with Node 18           |
| firebase-tools| latest    | `npm install -g firebase-tools` — runs the emulators |
| Java JDK      | 11+       | required by Firestore emulator |

## First-time setup

```bash
# At the repo root
cp .env.example .env
cd functions
cp .env.example .env
npm install
cd ..
```

`.env` files are gitignored — only `.env.example` is committed.

## Daily workflow

Two terminals are typical:

### Terminal A — emulators

```bash
firebase emulators:start
```

This starts:
- Auth on :9099
- Firestore on :8080
- Functions on :5001
- Storage on :9199
- Emulator UI on :4000 (open http://127.0.0.1:4000 in a browser)

The emulator UI lets you browse Firestore docs, inspect Functions logs,
and clear state between test runs.

### Terminal B — your client / tests

For the KMP app: run `./gradlew :shared:desktopTest` (or your IDE).
For Functions iteration: edit `.ts` under `functions/src/`. The emulator
auto-reloads on build. Re-build via:

```bash
cd functions
npm run build
```

## Pre-commit gate

Before pushing, run the aggregate verifier:

```bash
cd functions
npm run verify
```

This runs, in order:
1. `scan:secrets` — fails the build on any leaked credential pattern
2. `typecheck` — TypeScript strict mode, no emit
3. `test` — full Jest suite

## Pre-deploy gate

`firebase.json` declares a `predeploy` hook on the `functions` codebase. Any
`firebase deploy --only functions` triggers, in order:

1. `scan:secrets`
2. `typecheck`
3. `test`
4. `build` (TypeScript → `lib/`)

A failure at any step aborts the deploy.

## Where production secrets actually live

NEVER in `.env` for production. Use one of:

1. **Firebase Functions runtime config** (`firebase functions:config:set`)
   — values mounted as env vars at runtime
2. **Google Secret Manager** — accessed via Functions runtime via service
   account (the recommended path for any value classed as a secret)

Local `.env.example` exists only to document what knobs exist. Production
secrets are out-of-band.

## Smoke test (verify your local env works)

```bash
# 1. Start emulators in terminal A
firebase emulators:start

# 2. In terminal B:
cd functions
npm run build
npm run verify         # should print all green
```

Then open http://127.0.0.1:4000 — you should see the Firebase Emulator UI
with all four services listed as "Running".
