import os
import re
import json
import hashlib
import argparse
import sys

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
    sha256 = hashlib.sha256()
    with open(filepath, 'rb') as f:
        while chunk := f.read(8192):
            sha256.update(chunk)
    return sha256.hexdigest().upper()

def verify_package(package_dir, strict_mode):
    failures = []
    warnings = []
    
    # 1. File Completeness
    for req_file in REQUIRED_FILES:
        filepath = os.path.join(package_dir, req_file)
        if not os.path.exists(filepath):
            failures.append(f"Missing required file: {req_file}")
            
    if failures:
        return False, failures, warnings

    # 2. Check each file for placeholders, empty sections, headers, and forbidden claims
    for req_file in REQUIRED_FILES:
        filepath = os.path.join(package_dir, req_file)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            lines = content.splitlines()

        # Placeholders
        for placeholder in PLACEHOLDERS:
            matches = re.findall(placeholder, content, re.IGNORECASE)
            # Filter out mock templates inside 08_updated_output_templates.md
            if matches and req_file != "08_updated_output_templates.md":
                failures.append(f"File {req_file} contains placeholder: {matches[0]}")

        # Forbidden claims checklist (cautious wording)
        for claim in FORBIDDEN_CLAIMS:
            pattern = re.compile(r'\b' + re.escape(claim) + r'\b', re.IGNORECASE)
            for idx, line in enumerate(lines):
                if pattern.search(line):
                    if not re.search(r'CLM-\d+', line, re.IGNORECASE) and not re.search(r'evidence', line, re.IGNORECASE):
                        if req_file not in ["08_updated_output_templates.md", "09_evidence_ledger_and_audit_spec.md", "10_verification_script_spec.md", "12_final_upgraded_anti_system_plan.md"]:
                            # Treat claims as warnings first, then escalate to failures if strict
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

    # 3. Blocker / High findings validation in 00_executive_verdict.md
    verdict_path = os.path.join(package_dir, "00_executive_verdict.md")
    with open(verdict_path, 'r', encoding='utf-8') as f:
        verdict_content = f.read()

    # Verify run_manifest.json exists and obeys rules
    manifest_path = os.path.join(package_dir, "run_manifest.json")
    try:
        with open(manifest_path, 'r', encoding='utf-8') as f:
            manifest = json.load(f)
    except Exception as e:
        failures.append(f"Failed to parse run_manifest.json: {str(e)}")
        return False, failures, warnings

    final_status = manifest.get("final_status", "IN_PROGRESS")
    file_hashes = manifest.get("file_hashes", {})
    failed_agents = manifest.get("failed_agents", [])

    if final_status == "GO_INTERNAL" and (failed_agents or "IN_PROGRESS" in verdict_content):
        failures.append("Final status GO_INTERNAL is invalid when there are failed agents or unresolved items.")

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
                if invoked_val == "No" and verdict_val not in ["NOT_INVOKED", "-", ""]:
                    failures.append(f"Non-invoked agent '{agent_name}' has a real verdict '{verdict_val}' in matrix.")

    # 5. Package integrity hash validation
    integrity_path = os.path.join(package_dir, "package_integrity.md")
    with open(integrity_path, 'r', encoding='utf-8') as f:
        integrity_content = f.read()

    for req_file in REQUIRED_FILES:
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

    # 6. Check that every claim in the evidence ledger has a verification status
    evidence_path = os.path.join(package_dir, "evidence_ledger.md")
    with open(evidence_path, 'r', encoding='utf-8') as f:
        evidence_content = f.read()

    for line in evidence_content.splitlines():
        if "|" in line and "Claim ID" not in line and "---" not in line:
            parts = [p.strip() for p in line.split("|")]
            if len(parts) >= 6:
                claim_id = parts[1]
                status_val = parts[5]
                if status_val not in ["VERIFIED", "UNVERIFIED", "INCONCLUSIVE", "CONTRADICTED", "NOT_APPLICABLE"]:
                    failures.append(f"Invalid verification status '{status_val}' for claim '{claim_id}' in evidence ledger.")

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
        with open(failure_filepath, 'w', encoding='utf-8') as f:
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
