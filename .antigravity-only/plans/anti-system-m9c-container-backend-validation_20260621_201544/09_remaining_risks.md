# Remaining Risks and Mitigations

This document outlines the security, operational, and configuration risks identified in M9-C.

## 1. Host Virtualization Risks
WSL2 is configured but stopped, and Docker Desktop is missing, which prevents testing OS-level container isolation controls.

## 2. Local Fallback Risks
The runner continues to rely on `local_ephemeral` workspace fallback, which relies on in-process Python sandbox intercepts.
