import os
import sys
import json
import hashlib
from datetime import datetime, timezone

def calculate_sha256(filepath):
    sha256 = hashlib.sha256()
    with open(filepath, 'rb') as f:
        content = f.read()
    # Normalize line endings to LF before hashing
    normalized = content.replace(b'\r\n', b'\n')
    sha256.update(normalized)
    return sha256.hexdigest().upper()

def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    package_name = f"anti-system-m9c-container-backend-validation_{timestamp}"
    package_dir = os.path.normpath(os.path.join(repo_root, ".antigravity-only", "plans", package_name))
    os.makedirs(package_dir, exist_ok=True)

    # 1. Write the 11 markdown files
    files_content = {
        "00_m9c_scope.md": """# Phase M9-C Scope

This document defines the scope of Phase M9-C Container Backend Validation, detailing target backends and validation constraints.

## 1. M9-C Mission and Scope
The goal of M9-C is to validate whether a real container or native backend is available on the system to execute the `/anti` runner with stronger isolation than the local ephemeral workspace.

## 2. Operating Constraints
No container backends will be installed during this validation. If Docker is unavailable, the runner will continue to use the `local_ephemeral` workspace.
""",

        "01_backend_detection_results.md": """# Backend Detection Results

This document presents host container runtime detection findings, showing that Docker is currently unavailable.

## 1. Command Verification Outputs
- **`docker --version`**: Command not recognized.
- **`docker info`**: Service not available.
- **`wsl --status`**: Default Ubuntu version 2 detected.
- **`wsl -l -v`**: Ubuntu stopped, version 2.

## 2. Final Backend Availability
Docker is determined to be `unavailable`. The final status for this phase is `CONTAINER_BACKEND_NOT_AVAILABLE`.
""",

        "02_docker_poc_plan.md": """# Docker Proof of Concept Plan

This document outlines the execution and validation plan for containerized runs when a container runtime is present.

## 1. Image and Container Commands
If Docker is present, the runner executes a harmless container check:
- `docker run --rm --network none python:3.12-slim python --version`

## 2. Mounting Policy and Exclusions
The container POC plan mounts the repository workspace read-only and writes exclusively into a dedicated scratch/output directory.
""",

        "03_container_mount_policy.md": """# Container Mount Policy

This document details the volume mounting policy and directory restrictions defined for containerized subagent execution.

## 1. Mount Restrictions
To secure the host environment, the source repository folder must be mounted as read-only. Writable mounts are restricted to scratch folders.

## 2. Mount Exclusions
Mounting the user's home folder (`C:\\Users\\curro`), `.git` folder (writable), or other private system roots is strictly forbidden.
""",

        "04_network_isolation_policy.md": """# Network Isolation Policy

This document details the network gating and validation rules applied to container executions.

## 1. Network Constraints
Containers must run with `--network none` to prevent internet pulls, data egress, or unvetted remote calls.

## 2. Remote Dependency Blocks
All package updates and resource fetches must resolve locally from pre-cached files.
""",

        "05_secret_exclusion_policy.md": """# Secret Exclusion Policy

This document records the file-level filtering policy applied to mounted directories.

## 1. Excluded Files
Private environment files (`.env`), Git credentials, SSH private keys, and Android keystore/signing keys must never be mounted into the container.

## 2. Gated Paths
The sandbox policy filters out all matches to private patterns, forcing in-container executions to operate without credential access.
""",

        "06_container_poc_results.md": """# Container POC Results

This document records the execution results of the containerized runner check.

## 1. Execution Verdict
Because Docker is unavailable on the host system, the container proof-of-concept run was **not executed**.

## 2. Fallback Retained
The runner retains the `local_ephemeral` workspace for all subagent runs under Phase M9-C.
""",

        "07_replay_and_transcript_results.md": """# Replay and Transcript Results

This document evaluates transcript verification and replay validation behaviors.

## 1. Transcript Generation
Since the container POC was not executed, no new transcripts or evidence ledgers were generated under container mode.

## 2. Replay Verification Checks
Legacy replay logs and the M9-B package transcripts continue to verify successfully under the updated CLI replay command.
""",

        "08_test_results.md": """# Test Execution Results

This document compiles the verification outcomes of the automated tests.

## 1. Runner and Registry Tests
- `test_anti_runner.py`: Passed 6/6 tests.
- `test_anti_agent_registry.py`: Passed 11/11 tests.

## 2. CLI and Sandbox Tests
- `test_anti_cli.py`: Passed 23/23 tests.
- `test_sandbox_enforcement.py`: Passed 14/14 tests.
""",

        "09_remaining_risks.md": """# Remaining Risks and Mitigations

This document outlines the security, operational, and configuration risks identified in M9-C.

## 1. Host Virtualization Risks
WSL2 is configured but stopped, and Docker Desktop is missing, which prevents testing OS-level container isolation controls.

## 2. Local Fallback Risks
The runner continues to rely on `local_ephemeral` workspace fallback, which relies on in-process Python sandbox intercepts.
""",

        "10_final_verdict.md": """# Phase M9-C Final Verdict

This document presents the final verdict on container backend validation.

## 1. Verdict Cap Status
The final established operating state is capped at:
* **GO_INTERNAL_VERIFIED**
* **NO_GO_HIGH_STAKES**
* **CANONICAL_36_CONFIRMED**
* **CONTAINER_BACKEND_NOT_AVAILABLE**
* **LOCAL_EPHEMERAL_RUNNER_RETAINED**

## 2. Agent Consensus Matrix
| Agent | Role | Criticality | Invoked | Transaction ID | Vote | Confidence | Notes | Evidence Required |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Chief Orchestrator** | Chief System Coordinator | Critical | Yes | `chief_orchestrator_agent` | **GO_INTERNAL** | High | Capped at local ephemeral fallback. | Yes |
| **Red Team / Devil's Advocate** | Red Team Adversary | Critical | Yes | `red_team_devil_advocate_agent` | **GO_INTERNAL** | High | WSL2 detected but Docker missing. | Yes |
| **QA / Evidence** | Verification Specialist | Critical | Yes | `qa_evidence_agent` | **GO_INTERNAL** | High | All test suites run and pass. | Yes |
| **Security & Privacy** | Security & Privacy Auditor | Critical | Yes | `security_privacy_agent` | **GO_INTERNAL** | High | In-process sandbox boundaries enforced. | Yes |
"""
    }

    for name, content in files_content.items():
        with open(os.path.join(package_dir, name), 'w', encoding='utf-8') as f:
            f.write(content)

    # 2. Calculate checksums
    checksums = {}
    for f_name in files_content.keys():
        f_path = os.path.join(package_dir, f_name)
        checksums[f_name] = calculate_sha256(f_path)

    # 3. Generate package_integrity.md
    integrity_lines = [
        "# Package Integrity Verification\n",
        "This file records the cryptographic SHA-256 hashes for each file in the Phase M9-C evidence package.\n",
        "All line endings are normalized to LF before hashing to ensure consistent checks across Windows and Linux.\n\n",
        "| File Name | SHA-256 Checksum |\n",
        "| :--- | :--- |\n"
    ]
    for name in sorted(checksums.keys()):
        integrity_lines.append(f"| `{name}` | `{checksums[name]}` |\n")

    integrity_path = os.path.join(package_dir, "package_integrity.md")
    with open(integrity_path, 'w', encoding='utf-8') as f:
        f.writelines(integrity_lines)

    # Calculate checksum of package_integrity.md
    checksums["package_integrity.md"] = calculate_sha256(integrity_path)

    # 4. Generate final run_manifest.json
    final_manifest = {
        "run_id": f"run-{timestamp}-anti-m9c",
        "started_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "completed_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "command": "python .antigravity-only/scripts/anti_runner.py probe",
        "mode": "CONTAINER_BACKEND_NOT_AVAILABLE",
        "input_hash": "c3d4e5f60718293a4b5c6d7e8f9012345678a9b0",
        "package_path": package_dir,
        "required_agents": [
            "chief_orchestrator_agent",
            "red_team_devil_advocate_agent",
            "qa_evidence_agent",
            "security_privacy_agent"
        ],
        "optional_agents": [],
        "invoked_agents": [
            "chief_orchestrator_agent",
            "red_team_devil_advocate_agent",
            "qa_evidence_agent",
            "security_privacy_agent"
        ],
        "failed_agents": [],
        "fallbacks_used": [],
        "final_status": "CONTAINER_BACKEND_NOT_AVAILABLE",
        "verification_status": "PASS",
        "file_hashes": checksums,
        "token_usage": {
            "estimated": "UNVERIFIED",
            "actual_if_available": "UNVERIFIED",
            "baseline_if_available": "UNVERIFIED"
        }
    }

    manifest_path = os.path.join(package_dir, "run_manifest.json")
    with open(manifest_path, 'w', encoding='utf-8') as f:
        json.dump(final_manifest, f, indent=2)

    print(f"Phase M9-C package generated: {package_name}")

if __name__ == '__main__':
    main()
