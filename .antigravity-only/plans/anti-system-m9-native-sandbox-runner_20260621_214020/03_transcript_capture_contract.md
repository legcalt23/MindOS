# Transcript Capture Contract Specification

This document defines the schema and tamper-proofing requirements for agent execution transcripts.

## 1. Transcript Capture Validation
Every subagent execution cycle produces a transaction record stored in a line-delimited JSON format (`transcript.jsonl`). Transcripts are stored under the package transcripts directory. No transcript fabrication is permitted.

## 2. Transcript Schema
Each transcript record must conform to the following schema structure:

| Field | Type | Description |
| :--- | :--- | :--- |
| **agent_id** | `string` | Unique identifier of the executing subagent from the canonical 36 roster. |
| **registry_hash** | `string` | SHA-256 hash of `anti_agent_registry.py` matching the run instance. |
| **start_time** | `string` | UTC timestamp when the agent execution began. |
| **end_time** | `string` | UTC timestamp when the agent execution ended. |
| **sandbox_mode** | `string` | The active sandbox isolation profile (e.g. `isolated` or `partial`). |
| **input_refs** | `list[string]` | File paths read by the subagent during the run. |
| **output_refs** | `list[string]` | File paths modified or created by the subagent during the run. |
| **stdout_ref** | `string` | Hash of stdout stream captures. |
| **stderr_ref** | `string` | Hash of stderr stream captures. |
| **exit_code** | `integer` | Exit code of the subagent run process. |
| **file_deltas** | `map[string, string]`| List of target files and their computed hash diffs. |
| **forbidden_write_attempts** | `list[string]`| Log of files the agent tried to write but was blocked by policy. |
| **network_policy** | `string` | Network constraint value (e.g. `none`). |
| **replay_status** | `string` | Expected status on replay (e.g. `VERIFIED`). |
| **transcript_hash** | `string` | Cryptographic checksum hash of the transcript file content. |

## 3. Replay Inconclusive Fallback
If any transcript is absent or contains mismatched hashes during CLI replay, the replay execution MUST abort immediately and output:
```text
REPLAY_INCONCLUSIVE
```
No validation status update or GO status verification is permitted when transcripts are missing or untraceable.
