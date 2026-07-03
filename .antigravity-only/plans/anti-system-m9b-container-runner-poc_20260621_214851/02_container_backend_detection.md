# Container Backend Detection Results

This document presents findings from host environment detection and probing for container runtimes.

## 1. Backend Probing Results
Probing the host environment reveals:
- **Docker Desktop:** Unavailable (command execution failed).
- **WSL2:** Available/Unavailable (probed via system tools).
- **Fallback Workspace:** Available.

## 2. Selected Backend
Because Docker is unavailable, the recommended and executed backend is `local_ephemeral`.
