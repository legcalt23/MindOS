import os
import sys
import json
import argparse
import hashlib
import re

# Import verify_package from verify_subagents
from verify_subagents import verify_package, calculate_sha256, verify_transcript_hash_chain
from sandbox_enforcer import safe_open, assert_allowed, AntiSandboxViolation

def calculate_normalized_hash(filepath):
    """
    Calculates the SHA-256 hash of a file with normalized LF line endings.
    
    WARNING:
    This function implements a cross-platform line-ending normalization policy:
    it replaces all occurrences of b'\r\n' (CRLF) with b'\n' (LF) before hashing.
    This guarantees that Git's autocrlf conversion does not cause cryptographic
    integrity mismatches across different operating systems (Windows vs. Linux/macOS).
    """
    sha256 = hashlib.sha256()
    with safe_open(filepath, 'rb') as f:
        content = f.read()
    # Normalize line endings to LF
    normalized = content.replace(b'\r\n', b'\n')
    sha256.update(normalized)
    return sha256.hexdigest().upper()

def cmd_status(manifest_path):
    """Loads the manifest and prints the status details."""
    assert_allowed(manifest_path, 'read')
    
    if not os.path.exists(manifest_path):
        print(f"Error: Manifest file does not exist: {manifest_path}", file=sys.stderr)
        return 1

    try:
        with safe_open(manifest_path, 'r', encoding='utf-8') as f:
            manifest = json.load(f)
    except Exception as e:
        print(f"Error: Failed to parse manifest: {str(e)}", file=sys.stderr)
        return 1

    # Check required fields
    required_fields = ["run_id", "package_path", "required_agents", "invoked_agents", 
                       "failed_agents", "final_status", "verification_status", "file_hashes"]
    missing_fields = [field for field in required_fields if field not in manifest]
    if missing_fields:
        print(f"Error: Manifest is missing required fields: {', '.join(missing_fields)}", file=sys.stderr)
        return 1

    # Extract info safely
    run_id = manifest["run_id"]
    final_status = manifest["final_status"]
    verification_status = manifest["verification_status"]
    package_path = manifest["package_path"]
    required_agents = manifest["required_agents"]
    invoked_agents = manifest["invoked_agents"]
    failed_agents = manifest["failed_agents"]
    fallbacks_used = manifest.get("fallbacks_used", [])
    file_hashes = manifest["file_hashes"]
    token_usage = manifest.get("token_usage", {})

    print(f"Run ID: {run_id}")
    print(f"Final Status: {final_status}")
    print(f"Verification Status: {verification_status}")
    print(f"Package Path: {package_path}")
    print(f"Required Agents: {', '.join(required_agents)}")
    print(f"Invoked Agents: {', '.join(invoked_agents)}")
    print(f"Failed Agents: {', '.join(failed_agents) if failed_agents else 'None'}")
    print(f"Fallback Usage: {', '.join(fallbacks_used) if fallbacks_used else 'None'}")
    print(f"File Hash Count: {len(file_hashes)}")
    
    est = token_usage.get("estimated", "UNKNOWN")
    act = token_usage.get("actual_if_available", "UNKNOWN")
    base = token_usage.get("baseline_if_available", "UNKNOWN")
    print(f"Token Usage Status: Estimated={est}, Actual={act}, Baseline={base}")

    from anti_agent_registry import reconcile_agents_against_manifest, STRICT_CANONICAL_36_MODE, LEGACY_COMPAT_MODE
    is_m8 = "m8" in str(run_id).lower() or "m8" in str(manifest_path).lower()
    mode = STRICT_CANONICAL_36_MODE if is_m8 else LEGACY_COMPAT_MODE
    recon = reconcile_agents_against_manifest(manifest, mode=mode)
    print(f"Reconciliation Status: {recon['status']}")
    if recon['failures']:
        print(f"Reconciliation Failures: {', '.join(recon['failures'])}")
    if recon['warnings']:
        print(f"Reconciliation Warnings: {', '.join(recon['warnings'])}")
    print(f"Canonical Active Count: {recon['canonical_active_count']}")
    
    return 0

def cmd_verify(package_path, strict=False, use_json=False):
    """Executes verification on the given package."""
    assert_allowed(package_path, 'read')
    
    if not os.path.exists(package_path):
        if use_json:
            print(json.dumps({
                "status": "FAIL",
                "failures": [f"Package path does not exist: {package_path}"],
                "warnings": []
            }, indent=2))
        else:
            print(f"Error: Package path does not exist: {package_path}", file=sys.stderr)
        return 1

    # Call verify_package from verify_subagents
    is_passed, failures, warnings = verify_package(package_path, strict)
    
    if use_json:
        result = {
            "status": "PASS" if is_passed else "FAIL",
            "failures": failures,
            "warnings": warnings,
            "final_status_allowed": is_passed
        }
        print(json.dumps(result, indent=2))
    else:
        if is_passed:
            print("Verification PASSED!")
            for w in warnings:
                print(f"Warning: {w}")
        else:
            print("Verification FAILED!")
            for f in failures:
                print(f"Failure: {f}")
            for w in warnings:
                print(f"Warning: {w}")

    return 0 if is_passed else 1

def cmd_replay(manifest_path):
    """Validates the manifest run for output reproducibility."""
    assert_allowed(manifest_path, 'read')
    
    if not os.path.exists(manifest_path):
        print("REPLAY_FAIL: run_manifest does not exist")
        return 1

    try:
        with safe_open(manifest_path, 'r', encoding='utf-8') as f:
            manifest = json.load(f)
    except Exception as e:
        print(f"REPLAY_FAIL: Failed to parse manifest: {str(e)}")
        return 1

    from anti_agent_registry import reconcile_agents_against_manifest, validate_agent_registry, STRICT_CANONICAL_36_MODE, LEGACY_COMPAT_MODE
    run_id = manifest.get("run_id", "")
    is_m8 = "m8" in str(run_id).lower() or "m8" in str(manifest_path).lower()
    registry_mode = STRICT_CANONICAL_36_MODE if is_m8 else LEGACY_COMPAT_MODE
    
    recon = reconcile_agents_against_manifest(manifest, mode=registry_mode)
    if recon["status"] == "FAIL" and registry_mode == STRICT_CANONICAL_36_MODE:
        print("REPLAY_FAIL: agent reconciliation failed")
        return 1
        
    if registry_mode == STRICT_CANONICAL_36_MODE:
        reg_validation = validate_agent_registry()
        if reg_validation["status"] == "FAIL":
            print("REPLAY_FAIL: agent registry validation failed")
            return 1

    package_path = manifest.get("package_path", "")
    if not package_path:
        print("REPLAY_FAIL: package path is missing in manifest")
        return 1

    # Resolve package path relative to the manifest directory if it is not absolute or doesn't exist
    if not os.path.exists(package_path):
        # Fallback to local subdirectory relative to manifest folder
        manifest_dir = os.path.dirname(os.path.abspath(manifest_path))
        # E.g. package path basename
        base_name = os.path.basename(os.path.normpath(package_path))
        fallback_path = os.path.join(manifest_dir, base_name)
        if os.path.exists(fallback_path):
            package_path = fallback_path
        else:
            # Fallback to manifest_dir itself if files are in it
            package_path = manifest_dir

    # 1. Package path exists
    if not os.path.exists(package_path):
        print("REPLAY_FAIL: package path does not exist")
        return 1

    # 2. Check evidence ledger and package integrity exist
    evidence_ledger_path = os.path.join(package_path, "evidence_ledger.md")
    package_integrity_path = os.path.join(package_path, "package_integrity.md")
    
    if not os.path.exists(evidence_ledger_path):
        print("REPLAY_INCONCLUSIVE: evidence_ledger.md is missing (UNVERIFIED)")
        return 2
        
    if not os.path.exists(package_integrity_path):
        print("REPLAY_FAIL: package_integrity.md does not exist")
        return 1

    # 3. File hashes match manifest
    file_hashes = manifest.get("file_hashes", {})
    for filename, expected_hash in file_hashes.items():
        if filename in ["package_integrity.md", "run_manifest.json"]:
            continue
        file_path = os.path.join(package_path, filename)
        if not os.path.exists(file_path):
            print(f"REPLAY_FAIL: Package file is missing: {filename}")
            return 1
        
        try:
            computed_hash = calculate_normalized_hash(file_path)
        except Exception as e:
            print(f"REPLAY_FAIL: Failed to hash file {filename}: {str(e)}")
            return 1
            
        if computed_hash != expected_hash.upper():
            print(f"REPLAY_FAIL: Hash mismatch for file: {filename} (Expected {expected_hash.upper()}, got {computed_hash})")
            return 1

    # 4. Check that manifest hashes match package_integrity.md (Inconsistent Manifest Reference check)
    try:
        with safe_open(package_integrity_path, 'r', encoding='utf-8') as f:
            integrity_content = f.read()
        
        for filename, expected_hash in file_hashes.items():
            if filename in ["package_integrity.md", "run_manifest.json"]:
                continue
            pattern = re.compile(rf"\|\s*`?{re.escape(filename)}`?\s*\|\s*`?([A-Fa-f0-9]+)`?\s*\|")
            match = pattern.search(integrity_content)
            if not match:
                print(f"REPLAY_FAIL: inconsistent manifest reference (missing record in package_integrity.md for {filename})")
                return 1
            if match.group(1).upper() != expected_hash.upper():
                print(f"REPLAY_FAIL: inconsistent manifest reference (hash mismatch in package_integrity.md for {filename})")
                return 1
    except Exception as e:
        print(f"REPLAY_FAIL: Failed to check package integrity hashes: {str(e)}")
        return 1

    # 5. Check for fake verdicts, uninvoked agent verdicts, and placeholders in 00_executive_verdict.md
    final_status = manifest.get("final_status", "")
    failed_agents = manifest.get("failed_agents", [])
    verification_status = manifest.get("verification_status", "")
    
    if final_status in ["GO_INTERNAL", "GO_INTERNAL_VERIFIED"] and failed_agents:
        print("REPLAY_FAIL: fake verdict (GO status with failed agents)")
        return 1
        
    if verification_status == "PASS" and failed_agents:
        print("REPLAY_FAIL: inconsistent manifest reference (verification_status is PASS but failed_agents exist)")
        return 1

    verdict_file = None
    for f in os.listdir(package_path):
        if "verdict" in f.lower() and f.endswith(".md"):
            verdict_file = f
            break

    if verdict_file:
        verdict_path = os.path.join(package_path, verdict_file)
        try:
            with safe_open(verdict_path, 'r', encoding='utf-8') as f:
                verdict_content = f.read()
            
            # Check for placeholders
            for placeholder in [r"\[Placeholder.*?\]", r"\[insert.*?\]", r"\[TODO.*?\]", r"\[TBD.*?\]"]:
                if re.search(placeholder, verdict_content, re.IGNORECASE):
                    print("REPLAY_FAIL: placeholder verdict found in executive verdict")
                    return 1

            # Find vote matrix
            matrix_lines = []
            in_matrix = False
            for line in verdict_content.splitlines():
                if "Agent Vote Matrix" in line or "| Agent | Role |" in line:
                    in_matrix = True
                elif in_matrix and line.strip() == "":
                    in_matrix = False
                elif in_matrix:
                    matrix_lines.append(line)

            for line in matrix_lines:
                if "|" in line and "Agent" not in line and "---" not in line:
                    parts = [p.strip() for p in line.split("|")]
                    if len(parts) >= 7:
                        agent_name = parts[1]
                        invoked_val = parts[4]
                        verdict_val = parts[6]
                        # If not invoked, it must not have a real verdict
                        if invoked_val in ["No", "no", "False", "false"] and verdict_val not in ["NOT_INVOKED", "-", ""]:
                            print(f"REPLAY_FAIL: Non-invoked agent '{agent_name}' has a verdict in matrix")
                            return 1
        except Exception as e:
            print(f"REPLAY_FAIL: Failed to check executive verdict matrix: {str(e)}")
            return 1

    # 6. Check cached transcripts
    transcripts_dir = os.path.join(package_path, "transcripts")
    logs_dir = os.path.join(package_path, "logs")
    
    transcript_files = []
    resolved_dir = package_path
    if os.path.exists(transcripts_dir):
        transcript_files = [f for f in os.listdir(transcripts_dir) if f.endswith('.jsonl')]
        resolved_dir = transcripts_dir
    elif os.path.exists(logs_dir):
        transcript_files = [f for f in os.listdir(logs_dir) if f.endswith('.jsonl')]
        resolved_dir = logs_dir
    else:
        transcript_files = [f for f in os.listdir(package_path) if f.endswith('.jsonl')]
        resolved_dir = package_path
        
    if not transcript_files:
        print("REPLAY_INCONCLUSIVE: transcripts are missing (UNVERIFIED)")
        return 2

    for tf in transcript_files:
        tf_path = os.path.join(resolved_dir, tf)
        valid, err_msg = verify_transcript_hash_chain(tf_path)
        if not valid:
            print(f"REPLAY_FAIL: transcript hash mismatch for {tf}: {err_msg}")
            return 1

    # If everything passed
    print("REPLAY_PASS")
    return 0

def cmd_agents():
    """Prints the canonical 36 registry, count, and validation status."""
    from anti_agent_registry import load_canonical_agents, validate_agent_registry
    
    roster = load_canonical_agents()
    validation = validate_agent_registry()
    
    print("Canonical 36 Registry:")
    print("-" * 110)
    print(f"{'Agent ID':<35} | {'Name':<35} | {'Criticality':<12} | {'Required':<8}")
    print("-" * 110)
    for agent in roster:
        print(f"{agent.agent_id:<35} | {agent.name:<35} | {agent.criticality:<12} | {str(agent.required):<8}")
    print("-" * 110)
    print(f"Canonical Count: {len(roster)}")
    print(f"Registry Validation Status: {validation['status']}")
    if validation['status'] != "PASS":
        print("Validation Errors:")
        for err in validation['errors']:
            print(f"- {err}")
    return 0

def main():
    parser = argparse.ArgumentParser(description="/anti CLI tool for subagent orchestration status and validation")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # Status Parser
    status_parser = subparsers.add_parser("status", help="Show status of subagent run")
    status_parser.add_argument("--manifest", required=True, help="Path to run_manifest.json")

    # Verify Parser
    verify_parser = subparsers.add_parser("verify", help="Verify generated package integrity")
    verify_parser.add_argument("--package", required=True, help="Path to package directory")
    verify_parser.add_argument("--json", action="store_true", help="Output results in JSON format")
    verify_parser.add_argument("--strict", action="store_true", help="Escalate warnings to failures")

    # Replay Parser
    replay_parser = subparsers.add_parser("replay", help="Replay subagent verification run")
    replay_parser.add_argument("--manifest", required=True, help="Path to run_manifest.json")

    # Agents Parser
    agents_parser = subparsers.add_parser("agents", help="Print the canonical 36 registry")

    args = parser.parse_args()

    try:
        if args.command == "status":
            sys.exit(cmd_status(args.manifest))
        elif args.command == "verify":
            sys.exit(cmd_verify(args.package, args.strict, args.json))
        elif args.command == "replay":
            sys.exit(cmd_replay(args.manifest))
        elif args.command == "agents":
            sys.exit(cmd_agents())
    except AntiSandboxViolation as e:
        print(f"AntiSandboxViolation: {str(e)}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
