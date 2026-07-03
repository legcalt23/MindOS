import os
import re
import json
import hashlib
import argparse
import sys

from sandbox_enforcer import safe_open


REQUIRED_FILES = [
    "00_executive_verdict.md",
    "01_current_anti_system_audit.md",
    "02_agent_registry_audit.md",
    "03_review_modes_audit.md",
    "04_invocation_and_bootstrap_audit.md",
    "05_failure_modes_and_risk_register.md",
    "06_updated_anti_command_contract.md",
    "07_updated_agent_registry_spec.md",
    "08_updated_output_templates.md",
    "09_evidence_ledger_and_audit_spec.md",
    "10_verification_script_spec.md",
    "11_operational_runbook.md",
    "12_final_upgraded_anti_system_plan.md",
    "run_manifest.json",
    "evidence_ledger.md",
    "package_integrity.md"
]

PLACEHOLDERS = [
    r"\[Placeholder.*?\]",
    r"\[insert.*?\]",
    r"\[TODO.*?\]",
    r"\[TBD.*?\]"
]

FORBIDDEN_CLAIMS = [
    "fully audited",
    "100% reliable",
    "production-ready",
    "validated",
    "proved",
    "unbreakable"
]

def calculate_sha256(filepath):
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
    # Normalize Windows line endings to LF for consistent cross-platform hashes
    normalized_content = content.replace(b'\r\n', b'\n')
    sha256.update(normalized_content)
    return sha256.hexdigest().upper()


from anti_agent_registry import (
    validate_agent_registry,
    reconcile_agents_against_manifest,
    STRICT_CANONICAL_36_MODE,
    LEGACY_COMPAT_MODE
)

def verify_transcript_hash_chain(transcript_path):
    if not os.path.exists(transcript_path):
        return False, "Transcript file does not exist"
        
    try:
        prev_hash = ""
        with open(transcript_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        if not lines:
            return True, "Empty transcript"
            
        for line_num, line in enumerate(lines):
            line = line.strip()
            if not line:
                continue
            try:
                data = json.loads(line)
            except Exception as e:
                return False, f"Line {line_num+1}: Invalid JSON: {str(e)}"
            
            # Check keys. If no chaining keys exist, skip validation (legacy compat)
            if "transcript_hash" not in data or "transcript_hash_prev" not in data:
                return True, "Legacy format (no hash chaining)"
                
            expected_hash = data["transcript_hash"]
            expected_prev = data["transcript_hash_prev"]
            
            if expected_prev != prev_hash:
                return False, f"Line {line_num+1}: Hash chain broken. Expected prev hash '{prev_hash}', got '{expected_prev}'"
                
            # Recompute hash
            item = data.copy()
            item.pop("transcript_hash", None)
            serialized = json.dumps(item, sort_keys=True)
            
            hasher = hashlib.sha256()
            if prev_hash:
                hasher.update(serialized.encode('utf-8') + prev_hash.encode('utf-8'))
            else:
                hasher.update(serialized.encode('utf-8'))
            computed_hash = hasher.hexdigest().upper()
            
            if computed_hash != expected_hash.upper():
                return False, f"Line {line_num+1}: Computed hash mismatch. Expected '{expected_hash}', got '{computed_hash}'"
                
            prev_hash = expected_hash
            
        return True, "Valid transcript hash chain"
    except Exception as e:
        return False, f"Failed to parse transcript: {str(e)}"

def verify_package(package_dir, strict_mode):
    failures = []
    warnings = []
    
    # Verify run_manifest.json exists
    manifest_path = os.path.join(package_dir, "run_manifest.json")
    if not os.path.exists(manifest_path):
        failures.append("Missing required file: run_manifest.json")
        return False, failures, warnings

    try:
        with safe_open(manifest_path, 'r', encoding='utf-8') as f:
            manifest = json.load(f)
    except Exception as e:
        failures.append(f"Failed to parse run_manifest.json: {str(e)}")
        return False, failures, warnings

    # Determine validation mode
    run_id = manifest.get("run_id", "")
    is_m8 = "m8" in str(run_id).lower() or "m8" in str(package_dir).lower()
    
    if is_m8 or strict_mode:
        registry_mode = STRICT_CANONICAL_36_MODE
    else:
        registry_mode = LEGACY_COMPAT_MODE

    # Run agent registry reconciliation
    recon = reconcile_agents_against_manifest(manifest, mode=registry_mode)
    if recon["status"] == "FAIL":
        if registry_mode == STRICT_CANONICAL_36_MODE:
            failures.extend(recon["failures"])
        else:
            warnings.extend(recon["failures"])
    warnings.extend(recon["warnings"])

    # Strict registry checks
    if registry_mode == STRICT_CANONICAL_36_MODE:
        reg_validation = validate_agent_registry()
        if reg_validation["status"] == "FAIL":
            failures.extend(reg_validation["errors"])

    # 1. File Completeness
    file_hashes = manifest.get("file_hashes", {})
    if file_hashes:
        files_to_check = list(file_hashes.keys())
        if "package_integrity.md" not in files_to_check:
            files_to_check.append("package_integrity.md")
        if "run_manifest.json" not in files_to_check:
            files_to_check.append("run_manifest.json")
    else:
        files_to_check = REQUIRED_FILES

    # For M8, evidence_ledger.md is strictly required
    if registry_mode == STRICT_CANONICAL_36_MODE:
        if "evidence_ledger.md" not in files_to_check:
            files_to_check.append("evidence_ledger.md")

    for req_file in files_to_check:
        filepath = os.path.join(package_dir, req_file)
        if not os.path.exists(filepath):
            failures.append(f"Missing required file: {req_file}")
            
    if failures:
        return False, failures, warnings

    # 2. Check each file for placeholders, empty sections, headings, and forbidden claims
    for req_file in files_to_check:
        filepath = os.path.join(package_dir, req_file)
        with safe_open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            lines = content.splitlines()

        # Placeholders
        for placeholder in PLACEHOLDERS:
            matches = re.findall(placeholder, content, re.IGNORECASE)
            # Filter out templates
            if matches and req_file != "08_updated_output_templates.md":
                failures.append(f"File {req_file} contains placeholder: {matches[0]}")

        # Forbidden claims checklist (cautious wording)
        for claim in FORBIDDEN_CLAIMS:
            pattern = re.compile(r'\b' + re.escape(claim) + r'\b', re.IGNORECASE)
            for idx, line in enumerate(lines):
                if pattern.search(line):
                    if not re.search(r'CLM-\d+', line, re.IGNORECASE) and not re.search(r'evidence', line, re.IGNORECASE):
                        if req_file not in ["08_updated_output_templates.md", "09_evidence_ledger_and_audit_spec.md", "10_verification_script_spec.md", "12_final_upgraded_anti_system_plan.md"]:
                            warn_msg = f"File {req_file}:{idx+1} contains unsupported claim '{claim}' without evidence citation."
                            if strict_mode:
                                failures.append(warn_msg)
                            else:
                                warnings.append(warn_msg)

        # Empty sections and headings
        current_header = None
        header_content_count = 0
        for idx, line in enumerate(lines):
            if line.startswith('#'):
                if current_header and header_content_count == 0:
                    if req_file not in ["08_updated_output_templates.md", "07_updated_agent_registry_spec.md"]:
                        failures.append(f"File {req_file} has empty section under heading: {current_header}")
                current_header = line.strip()
                header_content_count = 0
            elif line.strip() and not line.startswith('---'):
                header_content_count += 1
        if current_header and header_content_count == 0 and req_file not in ["08_updated_output_templates.md", "07_updated_agent_registry_spec.md"]:
            failures.append(f"File {req_file} has empty final section under heading: {current_header}")

    # 3. Dynamic Verdict Validation
    verdict_file = None
    for f in os.listdir(package_dir):
        if "verdict" in f.lower() and f.endswith(".md"):
            verdict_file = f
            break

    final_status = manifest.get("final_status", "IN_PROGRESS")
    failed_agents = manifest.get("failed_agents", [])
    verification_status = manifest.get("verification_status", "")

    if final_status in ["GO_INTERNAL", "GO_INTERNAL_VERIFIED"] and failed_agents:
        failures.append("Final status GO_INTERNAL is invalid when there are failed agents.")

    if verification_status == "PASS" and failed_agents:
        failures.append("verification_status is PASS but failed_agents exist.")

    if verdict_file:
        verdict_path = os.path.join(package_dir, verdict_file)
        with safe_open(verdict_path, 'r', encoding='utf-8') as f:
            verdict_content = f.read()

        if "IN_PROGRESS" in verdict_content and final_status in ["GO_INTERNAL", "GO_INTERNAL_VERIFIED"]:
            failures.append("Final status is GO but verdict contains IN_PROGRESS placeholder.")

        # Check for placeholders in verdict
        for placeholder in PLACEHOLDERS:
            if re.search(placeholder, verdict_content, re.IGNORECASE):
                failures.append(f"Verdit file {verdict_file} contains placeholder.")

        # 4. Check for fake non-invoked verdicts
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
                    if invoked_val in ["No", "False", "no"] and verdict_val not in ["NOT_INVOKED", "-", ""]:
                        failures.append(f"Non-invoked agent '{agent_name}' has a real verdict '{verdict_val}' in matrix.")

    # 5. Package integrity hash validation
    integrity_path = os.path.join(package_dir, "package_integrity.md")
    if os.path.exists(integrity_path):
        with safe_open(integrity_path, 'r', encoding='utf-8') as f:
            integrity_content = f.read()

        for req_file in files_to_check:
            filepath = os.path.join(package_dir, req_file)
            if req_file in ["package_integrity.md", "run_manifest.json"]:
                continue
            
            computed_hash = calculate_sha256(filepath)
            
            manifest_hash = file_hashes.get(req_file)
            if not manifest_hash:
                failures.append(f"Missing hash in run_manifest.json for: {req_file}")
            elif manifest_hash.upper() != computed_hash:
                failures.append(f"Hash mismatch in run_manifest.json for {req_file}. Expected {computed_hash}, got {manifest_hash}")

            pattern = re.compile(rf"\|\s*`?{re.escape(req_file)}`?\s*\|\s*`?([A-Fa-f0-9]+)`?\s*\|")
            match = pattern.search(integrity_content)
            if not match:
                failures.append(f"Missing integrity record or hash in package_integrity.md for: {req_file}")
            elif match.group(1).upper() != computed_hash:
                failures.append(f"Hash mismatch in package_integrity.md for {req_file}. Expected {computed_hash}, got {match.group(1).upper()}")
    elif registry_mode == STRICT_CANONICAL_36_MODE:
        failures.append("Missing required file: package_integrity.md")

    # 6. Check that every claim in the evidence ledger has a verification status
    evidence_path = os.path.join(package_dir, "evidence_ledger.md")
    if os.path.exists(evidence_path):
        with safe_open(evidence_path, 'r', encoding='utf-8') as f:
            evidence_content = f.read()

        for line in evidence_content.splitlines():
            if "|" in line and "Claim ID" not in line and "---" not in line:
                parts = [p.strip() for p in line.split("|")]
                if len(parts) >= 6:
                    claim_id = parts[1]
                    status_val = parts[5]
                    if status_val not in ["VERIFIED", "UNVERIFIED", "INCONCLUSIVE", "CONTRADICTED", "NOT_APPLICABLE"]:
                        failures.append(f"Invalid verification status '{status_val}' for claim '{claim_id}' in evidence ledger.")

        if registry_mode == STRICT_CANONICAL_36_MODE:
            required_statements = [
                ("CANONICAL_36_CONFIRMED", "CANONICAL_36_NOT_CONFIRMED"),
                "GO_INTERNAL_VERIFIED",
                "NO_GO_HIGH_STAKES",
                "SANDBOX_ENFORCEMENT_PARTIAL"
            ]
            for stmt in required_statements:
                if isinstance(stmt, tuple):
                    if not any(s in evidence_content for s in stmt):
                        failures.append(f"evidence_ledger.md is missing statement: {' or '.join(stmt)}")
                else:
                    if stmt not in evidence_content:
                        failures.append(f"evidence_ledger.md is missing required statement: {stmt}")

    # 7. Verify transcript hash chaining if transcripts exist
    transcripts_dir = os.path.join(package_dir, "transcripts")
    transcript_files = []
    if os.path.exists(transcripts_dir):
        transcript_files = [f for f in os.listdir(transcripts_dir) if f.endswith('.jsonl')]
    elif os.path.exists(package_dir):
        # Fallback to package_dir itself
        transcript_files = [f for f in os.listdir(package_dir) if f.endswith('.jsonl')]
        transcripts_dir = package_dir

    for tf in transcript_files:
        tf_path = os.path.join(transcripts_dir, tf)
        valid, err_msg = verify_transcript_hash_chain(tf_path)
        if not valid:
            failures.append(f"Transcript verification failed for {tf}: {err_msg}")

    is_passed = len(failures) == 0
    return is_passed, failures, warnings

def main():
    parser = argparse.ArgumentParser(description="Hardened Package Verification Script")
    parser.add_argument("--package", required=True, help="Path to the review package folder")
    parser.add_argument("--json", action="store_true", help="Output results in JSON format")
    parser.add_argument("--strict", action="store_true", help="Escalate warnings to failures")
    parser.add_argument("--write-failures", action="store_true", help="Write 13_verification_failures.md on error")
    args = parser.parse_args()

    package_dir = args.package
    if not os.path.exists(package_dir):
        err_result = {
            "status": "FAIL",
            "failures": [f"Package directory does not exist: {package_dir}"],
            "warnings": [],
            "final_status_allowed": False
        }
        if args.json:
            print(json.dumps(err_result, indent=2))
        else:
            print(f"Error: {err_result['failures'][0]}")
        sys.exit(1)

    is_passed, failures, warnings = verify_package(package_dir, args.strict)
    
    output_status = "PASS" if is_passed else "FAIL"
    result = {
        "status": output_status,
        "failures": failures,
        "warnings": warnings,
        "final_status_allowed": is_passed
    }

    # Write failures report if requested and failed
    if not is_passed and args.write_failures:
        failure_filepath = os.path.join(package_dir, "13_verification_failures.md")
        with safe_open(failure_filepath, 'w', encoding='utf-8') as f:
            f.write("# Verification Failures Report\n\n")
            f.write(f"**Verification Status:** FAIL\n")
            f.write(f"**Target Package:** `{package_dir}`\n\n")
            f.write("## Failures Detected\n\n")
            for idx, failure in enumerate(failures):
                f.write(f"{idx+1}. {failure}\n")
            f.write("\n## Remediation Guidelines\n")
            f.write("Review the errors listed above, correct the files, and run this script again.\n")

    if args.json:
        print(json.dumps(result, indent=2))
    else:
        if is_passed:
            print("Verification PASSED!")
            if warnings:
                print("\nWarnings:")
                for warn in warnings:
                    print(f"- {warn}")
        else:
            print("Verification FAILED!")
            print("\nFailures:")
            for fail in failures:
                print(f"- {fail}")
            if warnings:
                print("\nWarnings:")
                for warn in warnings:
                    print(f"- {warn}")

    sys.exit(0 if is_passed else 1)

if __name__ == "__main__":
    main()
