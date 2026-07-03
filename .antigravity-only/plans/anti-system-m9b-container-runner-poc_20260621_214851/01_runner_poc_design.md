# Runner POC Design

This document details the design of the local runner POC, outlining the command CLI structure and isolation mechanics.

## 1. CLI Commands
The runner implements two primary commands:
- `probe`: Detects available container backends (Docker, WSL2) and recommends the best option.
- `run`: Orchestrates subagent execution based on a manifest under POC mode.

## 2. Isolation Mechanics
- When Docker is unavailable, the fallback local ephemeral workspace copies inputs to a temporary directory in `scratch/`.
- Validates the manifest path, checks canonical registry against manifest, and writes transcript files.
