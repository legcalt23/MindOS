# NEU — Infrastructure as Code blueprint

This directory holds the IaC contract for NEU's backend infrastructure.

## What IaC means for a Firebase-native stack

NEU runs on Firebase: Cloud Functions, Firestore, Storage, Auth, App Check.
We do NOT operate raw GCP resources — Google manages the runtime. That has
two consequences for IaC:

1. There is no provisioner that *creates* most of what we depend on.
   Firebase resources are declared in `firebase.json` + the `.rules` files
   + Firestore composite indexes. These ARE our IaC.

2. A traditional Terraform module would only be useful if/when we add
   resources outside Firebase (Google Secret Manager, KMS, Cloud Run,
   Pub/Sub, Memorystore, etc.) OR migrate the backend to a non-Firebase
   platform.

So this directory contains:

- **`firebase/`** — the *active* IaC for our current stack. Documentation
  pointing back at the canonical files in repo root.
- **`terraform/`** — a *non-destructive placeholder* showing how a future
  cloud migration would be structured. Contains only `.example` files and
  validation tests; never applies.

## Active IaC files (canonical)

| Resource              | File                          |
|-----------------------|-------------------------------|
| Firestore security    | `firestore.rules`             |
| Firestore indexes     | `firestore.indexes.json`      |
| Storage security      | `storage.rules`               |
| Functions config      | `functions/package.json`      |
| Emulator topology     | `firebase.json` (`emulators`) |
| Deploy hooks          | `firebase.json` (`functions.predeploy`) |
| CI                    | `.github/workflows/ci.yml`    |
| Weekly security scan  | `.github/workflows/security.yml` |

All of these are version-controlled, change-reviewable, and re-runnable.
That IS the IaC contract.

## Future-state IaC (not active)

When/if NEU adds resources outside Firebase (e.g. dedicated Google Secret
Manager secrets, a Pub/Sub topic for fraud signals, a Cloud Scheduler job
for reconciliation), they should land under `infra/terraform/` and be
applied via a separate, audited workflow that:

- Uses a service account with minimum-privilege roles
- Stores `.tfstate` in a versioned GCS bucket with object versioning + retention
- Never runs from a developer machine — only CI with workload-identity-federation
- Requires a separate human reviewer for every plan/apply

See `infra/terraform/README.md` for the placeholder layout and intent.

## What goes in `secrets/` (never committed)

Nothing in this repo. Production secrets live in:
- **Firebase Functions runtime config** for non-rotating tunables
- **Google Secret Manager** for everything else (real payment keys, webhook
  signing secrets, Stripe keys, etc.)

Local development uses `.env` files (gitignored) seeded from `.env.example`.

## Smoke test

Validate the IaC files compile / parse cleanly:

```bash
# Firebase config + emulator structure
cd functions
npm test -- infra-firebase-config

# CI workflow structure
npm test -- infra-ci-workflows

# Terraform placeholder (does NOT actually plan/apply)
npm test -- infra-terraform-blueprint
```
