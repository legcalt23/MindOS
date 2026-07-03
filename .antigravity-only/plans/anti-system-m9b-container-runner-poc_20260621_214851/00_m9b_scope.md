# Phase M9-B Scope

This document defines the scope of Phase M9-B Local Runner POC. It details the boundaries, constraints, and non-goals of this local proof of concept.

## 1. Scope and Targets
This phase implements a minimal, local-only proof of concept for an isolated runner. The goal is to move beyond Python process-level sandbox interception toward stronger container/workspace isolation.

## 2. Core Constraints
- Docker is not installed on the system, which requires fallback to the local ephemeral workspace.
- The readiness cap status is NOT_NATIVE_SANDBOX.
- High-stakes operation is NOT approved (NO_GO_HIGH_STAKES).
