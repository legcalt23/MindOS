# Backend Detection Results

This document presents host container runtime detection findings, showing that Docker is currently unavailable.

## 1. Command Verification Outputs
- **`docker --version`**: Command not recognized.
- **`docker info`**: Service not available.
- **`wsl --status`**: Default Ubuntu version 2 detected.
- **`wsl -l -v`**: Ubuntu stopped, version 2.

## 2. Final Backend Availability
Docker is determined to be `unavailable`. The final status for this phase is `CONTAINER_BACKEND_NOT_AVAILABLE`.
