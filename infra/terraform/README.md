# Terraform placeholder (NOT active)

This directory is a **non-destructive blueprint**, not a working Terraform
module. Nothing here will plan or apply against a real cloud project.

Phase 7 of the wallet plan asks for "Terraform base" infra. Because MindOS's
current stack is Firebase-native (no managed PostgreSQL, no Redis, no
Cloud Run, no VPC), there is nothing for a Terraform module to manage
*today*. This blueprint exists so that **when** we add resources that
*do* need Terraform (Secret Manager rotations, Pub/Sub fraud queues, a
KMS keyring, Cloud Scheduler reconciliation jobs, etc.), the path is
already mapped out and reviewed.

## What's here

- `main.tf.example`     — provider + state backend skeleton
- `variables.tf.example` — input variable contract with `sensitive` markers
- `modules/`            — empty stubs (`secret_manager/`, `pubsub_fraud/`, `kms/`, `cloud_scheduler/`)

The `.example` suffix is deliberate: `terraform init` will not pick them
up, so it is impossible to accidentally `terraform apply` from this dir.

To activate a module:

1. Rename `*.tf.example` → `*.tf`
2. Configure a remote backend (GCS bucket with versioning + object lifecycle)
3. Wire workload-identity-federation in a SEPARATE GitHub Actions workflow
   (do NOT extend `.github/workflows/ci.yml` — IaC apply belongs in its
   own gated workflow with mandatory human approval)
4. Add an `infra/terraform/SECURITY.md` documenting blast radius

## Non-destructive guarantees

- No `provider` block in active state
- No `backend` block in active state
- No `resource` blocks
- All sample resources are commented out or live in `.example` files
- A smoke test (`infra-terraform-blueprint`) verifies these guarantees on
  every CI run.

## Forbidden patterns (CI-enforced)

The smoke test will fail if any of these appear in `*.tf` (active) files:

| Pattern                          | Why forbidden                              |
|----------------------------------|--------------------------------------------|
| Real credentials                 | Use Secret Manager + WIF, never inline      |
| `terraform { backend "local" }`  | State must be remote + versioned            |
| Unmarked `sensitive` variables   | Every secret-bearing var must be marked     |
| `provider "google" { credentials = }` with file path | Credentials must come from environment / WIF |
