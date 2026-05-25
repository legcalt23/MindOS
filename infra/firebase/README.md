# Active IaC — Firebase

This directory is a pointer document. The actual canonical files live in
the repo root because Firebase tooling expects them there.

| What                  | Where                              |
|-----------------------|------------------------------------|
| Firestore rules       | `../../firestore.rules`            |
| Firestore indexes     | `../../firestore.indexes.json`     |
| Storage rules         | `../../storage.rules`              |
| Firebase project cfg  | `../../firebase.json`              |
| Functions sources     | `../../functions/src/**`           |
| Functions tsconfig    | `../../functions/tsconfig.json`    |
| Functions deps        | `../../functions/package*.json`    |

Editing any of these is editing infrastructure. Every change MUST go through
CI (see `.github/workflows/ci.yml`).

## How to "apply"

```bash
# Plan (dry-run):
firebase deploy --only functions,firestore:rules,storage:rules --dry-run

# Apply:
firebase deploy --only functions,firestore:rules,storage:rules
```

The deploy is gated by the `predeploy` hooks declared in `firebase.json`:

1. `scan:secrets` — no secrets in source
2. `typecheck` — strict TypeScript
3. `test` — full Jest suite
4. `build` — compiles `lib/`

A failure at any step aborts the deploy before any side effect occurs.
