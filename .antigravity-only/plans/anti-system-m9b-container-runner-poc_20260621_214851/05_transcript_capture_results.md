# Transcript Capture Results

This document outlines the transcript structure, capture format, and cryptographic chaining logic.

## 1. Event Log Format
Transcripts are recorded as JSON Lines (`.jsonl`), including `event_id`, `run_id`, `backend`, `sandbox_mode`, and command details.

## 2. Cryptographic Hash Chain
Each transcript event contains a `transcript_hash` computed from its keys and the previous event's hash (`transcript_hash_prev`), preventing logs from being tampered with.
