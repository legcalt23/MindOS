import os
import sys
import unittest
import subprocess
import json
import shutil
import re

class TestAntiCli(unittest.TestCase):
    def setUp(self):
        self.repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        self.cli_path = os.path.join(self.repo_root, '.antigravity-only', 'scripts', 'anti_cli.py')
        self.valid_package = os.path.join(self.repo_root, '.antigravity-only', 'plans', 'anti-system-review_20260621_150701')
        self.valid_manifest = os.path.join(self.valid_package, 'run_manifest.json')
        
        # Fixtures paths
        self.fixtures_dir = os.path.join(self.repo_root, '.antigravity-only', 'scripts', 'fixtures')
        self.fake_package = os.path.join(self.fixtures_dir, 'fake_non_invoked_verdict_package')
        self.placeholder_package = os.path.join(self.fixtures_dir, 'placeholder_output_package')
        
        # Temp dir for hash tampering tests
        self.temp_package_dir = os.path.join(self.repo_root, 'scratch', 'tamper_package_test')
        os.makedirs(self.temp_package_dir, exist_ok=True)

    def tearDown(self):
        if os.path.exists(self.temp_package_dir):
            shutil.rmtree(self.temp_package_dir)

    def run_cli(self, args):
        cmd = [sys.executable, self.cli_path] + args
        # Run with current PYTHONPATH to ensure imports work
        env = os.environ.copy()
        env["PYTHONPATH"] = os.path.dirname(self.cli_path)
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, env=env)
        return result

    def test_01_status_valid_manifest(self):
        """1. status works on valid manifest."""
        res = self.run_cli(["status", "--manifest", self.valid_manifest])
        self.assertEqual(res.returncode, 0)
        self.assertIn("Run ID:", res.stdout)
        self.assertIn("Final Status:", res.stdout)
        self.assertIn("File Hash Count:", res.stdout)

    def test_02_status_missing_manifest(self):
        """2. status fails cleanly on missing manifest."""
        missing_path = os.path.join(self.repo_root, 'nonexistent_manifest.json')
        res = self.run_cli(["status", "--manifest", missing_path])
        self.assertEqual(res.returncode, 1)
        self.assertIn("Error: Manifest file does not exist", res.stderr)

    def test_03_status_refuses_denied_path(self):
        """3. status refuses denied manifest path."""
        # Reading a file named .env (even if it's missing) should be blocked by sandbox enforcer
        denied_path = os.path.join(self.repo_root, '.env')
        res = self.run_cli(["status", "--manifest", denied_path])
        self.assertEqual(res.returncode, 1)
        self.assertIn("AntiSandboxViolation", res.stderr)

    def test_04_verify_passes_valid_package(self):
        """4. verify passes valid package."""
        res = self.run_cli(["verify", "--package", self.valid_package])
        self.assertEqual(res.returncode, 0)
        self.assertIn("Verification PASSED!", res.stdout)

    def test_05_verify_fails_placeholder_package(self):
        """5. verify fails placeholder package."""
        res = self.run_cli(["verify", "--package", self.placeholder_package])
        self.assertEqual(res.returncode, 1)
        self.assertIn("Verification FAILED!", res.stdout)

    def test_06_verify_fails_fake_non_invoked_verdict_package(self):
        """6. verify fails fake non-invoked verdict package."""
        res = self.run_cli(["verify", "--package", self.fake_package])
        self.assertEqual(res.returncode, 1)
        self.assertIn("Verification FAILED!", res.stdout)


    def test_07_replay_passes_when_hashes_match(self):
        """7. replay passes when hashes match and transcripts exist."""
        # Create a temp copy of valid package, add a mock transcript.jsonl
        # to ensure it does not return REPLAY_INCONCLUSIVE
        temp_package = os.path.join(self.temp_package_dir, 'valid_run_replay')
        shutil.copytree(self.valid_package, temp_package)
        
        # Write dummy transcript
        with open(os.path.join(temp_package, 'transcript.jsonl'), 'w') as f:
            f.write('{"step": 1}\n')
            
        # Update manifest file path inside package's run_manifest.json to point to temp_package
        manifest_file = os.path.join(temp_package, 'run_manifest.json')
        with open(manifest_file, 'r', encoding='utf-8') as f:
            manifest_data = json.load(f)
        manifest_data["package_path"] = temp_package
        with open(manifest_file, 'w', encoding='utf-8') as f:
            json.dump(manifest_data, f, indent=2)

        res = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 0)
        self.assertIn("REPLAY_PASS", res.stdout)

    def test_08_replay_fails_when_file_hash_changes(self):
        """8. replay fails when a package file hash changes."""
        temp_package = os.path.join(self.temp_package_dir, 'tampered_run_replay')
        shutil.copytree(self.valid_package, temp_package)
        
        # Tamper a file
        verdict_file = os.path.join(temp_package, '00_executive_verdict.md')
        with open(verdict_file, 'a') as f:
            f.write("\nTAMPERED_CONTENT_STRETCH\n")

        manifest_file = os.path.join(temp_package, 'run_manifest.json')
        with open(manifest_file, 'r', encoding='utf-8') as f:
            manifest_data = json.load(f)
        manifest_data["package_path"] = temp_package
        with open(manifest_file, 'w', encoding='utf-8') as f:
            json.dump(manifest_data, f, indent=2)

        res = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 1)
        self.assertIn("REPLAY_FAIL", res.stdout)
        self.assertIn("Hash mismatch for file", res.stdout)

    def test_09_replay_becomes_inconclusive_when_transcripts_missing(self):
        """9. replay becomes REPLAY_INCONCLUSIVE when transcripts are missing."""
        # Create temp package, do NOT write any .jsonl transcript files
        temp_package = os.path.join(self.temp_package_dir, 'missing_transcripts_replay')
        shutil.copytree(self.valid_package, temp_package)
        
        # Remove any jsonl files that might exist
        for item in os.listdir(temp_package):
            if item.endswith('.jsonl'):
                os.remove(os.path.join(temp_package, item))

        manifest_file = os.path.join(temp_package, 'run_manifest.json')
        with open(manifest_file, 'r', encoding='utf-8') as f:
            manifest_data = json.load(f)
        manifest_data["package_path"] = temp_package
        with open(manifest_file, 'w', encoding='utf-8') as f:
            json.dump(manifest_data, f, indent=2)

        res = self.run_cli(["replay", "--manifest", manifest_file])
        # REPLAY_INCONCLUSIVE returns code 2
        self.assertEqual(res.returncode, 2)
        self.assertIn("REPLAY_INCONCLUSIVE", res.stdout)
        self.assertIn("transcripts are missing (UNVERIFIED)", res.stdout)

    def test_10_replay_does_not_invent_transcripts(self):
        """10. replay does not invent missing transcripts."""
        # Verify that if we run replay with missing transcripts, it fails/inconcludes
        # instead of creating any new .jsonl files in the package directory.
        temp_package = os.path.join(self.temp_package_dir, 'no_invent_replay')
        shutil.copytree(self.valid_package, temp_package)
        
        # Remove any jsonl files
        for item in os.listdir(temp_package):
            if item.endswith('.jsonl'):
                os.remove(os.path.join(temp_package, item))

        manifest_file = os.path.join(temp_package, 'run_manifest.json')
        with open(manifest_file, 'r', encoding='utf-8') as f:
            manifest_data = json.load(f)
        manifest_data["package_path"] = temp_package
        with open(manifest_file, 'w', encoding='utf-8') as f:
            json.dump(manifest_data, f, indent=2)

        # Before replay, check no jsonl exists
        jsonl_files_before = [f for f in os.listdir(temp_package) if f.endswith('.jsonl')]
        self.assertEqual(len(jsonl_files_before), 0)

        res = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 2)
        self.assertIn("REPLAY_INCONCLUSIVE", res.stdout)

        # After replay, check still no jsonl exists (none was created/invented)
        jsonl_files_after = [f for f in os.listdir(temp_package) if f.endswith('.jsonl')]
        self.assertEqual(len(jsonl_files_after), 0)

    def test_11_status_malformed_json(self):
        """11. status fails closed on malformed JSON."""
        malformed_file = os.path.join(self.temp_package_dir, "malformed_manifest.json")
        with open(malformed_file, "w") as f:
            f.write("{ invalid json")
        res = self.run_cli(["status", "--manifest", malformed_file])
        self.assertEqual(res.returncode, 1)
        self.assertIn("Error: Failed to parse manifest", res.stderr)

    def test_12_status_missing_required_fields(self):
        """12. status fails closed on missing required fields."""
        incomplete_file = os.path.join(self.temp_package_dir, "incomplete_manifest.json")
        with open(incomplete_file, "w") as f:
            json.dump({"run_id": "test-run"}, f)
        res = self.run_cli(["status", "--manifest", incomplete_file])
        self.assertEqual(res.returncode, 1)
        self.assertIn("Error: Manifest is missing required fields", res.stderr)

    def test_13_status_ignores_unknown_fields(self):
        """13. status succeeds and ignores unknown fields."""
        temp_package = os.path.join(self.temp_package_dir, "unknown_fields_status")
        shutil.copytree(self.valid_package, temp_package)
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        data["some_crazy_new_field"] = "hello_world"
        data["package_path"] = temp_package
        with open(manifest_file, "w") as f:
            json.dump(data, f)
        res = self.run_cli(["status", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 0)
        self.assertNotIn("some_crazy_new_field", res.stdout)

    def test_14_status_does_not_fabricate_state(self):
        """14. status does not fabricate live state."""
        # Verify it prints exactly what is in the manifest
        temp_package = os.path.join(self.temp_package_dir, "no_fabrication_status")
        shutil.copytree(self.valid_package, temp_package)
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        data["final_status"] = "SUPER_CUSTOM_VERDICT_STATE"
        data["package_path"] = temp_package
        with open(manifest_file, "w") as f:
            json.dump(data, f)
        res = self.run_cli(["status", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 0)
        self.assertIn("Final Status: SUPER_CUSTOM_VERDICT_STATE", res.stdout)

    def test_15_replay_inconclusive_missing_evidence_ledger(self):
        """15. replay returns inconclusive when evidence_ledger.md is missing."""
        temp_package = os.path.join(self.temp_package_dir, "missing_evidence_ledger")
        shutil.copytree(self.valid_package, temp_package)
        
        # Write dummy transcript to avoid missing transcript trigger
        with open(os.path.join(temp_package, "transcript.jsonl"), "w") as f:
            f.write('{"step": 1}\n')
            
        # Delete evidence_ledger.md
        ledger = os.path.join(temp_package, "evidence_ledger.md")
        if os.path.exists(ledger):
            os.remove(ledger)
            
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        data["package_path"] = temp_package
        with open(manifest_file, "w") as f:
            json.dump(data, f)
            
        res = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 2)
        self.assertIn("REPLAY_INCONCLUSIVE: evidence_ledger.md is missing", res.stdout)

    def test_16_replay_fails_on_fake_verdict(self):
        """16. replay fails on fake verdict (GO status with failed agents)."""
        temp_package = os.path.join(self.temp_package_dir, "fake_verdict")
        shutil.copytree(self.valid_package, temp_package)
        
        # Write transcript
        with open(os.path.join(temp_package, "transcript.jsonl"), "w") as f:
            f.write('{"step": 1}\n')
            
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        data["package_path"] = temp_package
        data["final_status"] = "GO_INTERNAL_VERIFIED"
        data["failed_agents"] = ["qa_evidence_agent"]
        with open(manifest_file, "w") as f:
            json.dump(data, f)
            
        res = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 1)
        self.assertIn("REPLAY_FAIL: fake verdict", res.stdout)

    def test_17_replay_fails_on_placeholder_verdict(self):
        """17. replay fails on placeholder verdict in executive verdict."""
        import hashlib
        temp_package = os.path.join(self.temp_package_dir, "placeholder_verdict")
        shutil.copytree(self.valid_package, temp_package)
        
        # Write transcript
        with open(os.path.join(temp_package, "transcript.jsonl"), "w") as f:
            f.write('{"step": 1}\n')
            
        # Tamper executive verdict with a placeholder
        verdict_file = os.path.join(temp_package, "00_executive_verdict.md")
        with open(verdict_file, "r", encoding="utf-8") as f:
            content = f.read()
        
        content += "\n[TODO: insert real verdict here]\n"
        with open(verdict_file, "w", encoding="utf-8") as f:
            f.write(content)
            
        normalized_content = content.replace("\r\n", "\n").encode("utf-8")
        h = hashlib.sha256(normalized_content).hexdigest().upper()
        
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        data["package_path"] = temp_package
        data["file_hashes"]["00_executive_verdict.md"] = h
        
        # Update package_integrity.md
        integrity_file = os.path.join(temp_package, "package_integrity.md")
        with open(integrity_file, "r", encoding="utf-8") as f:
            integrity_content = f.read()
        
        # Original hash from valid package run_manifest.json
        with open(self.valid_manifest, "r") as f:
            orig_data = json.load(f)
        orig_hash = orig_data["file_hashes"]["00_executive_verdict.md"]
        
        integrity_content = integrity_content.replace(orig_hash, h)
        with open(integrity_file, "w", encoding="utf-8") as f:
            f.write(integrity_content)
            
        normalized_integrity = integrity_content.replace("\r\n", "\n").encode("utf-8")
        h_integrity = hashlib.sha256(normalized_integrity).hexdigest().upper()
        data["file_hashes"]["package_integrity.md"] = h_integrity
        
        with open(manifest_file, "w") as f:
            json.dump(data, f)
            
        res = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 1)
        self.assertIn("REPLAY_FAIL: placeholder verdict", res.stdout)

    def test_18_replay_fails_on_uninvoked_agent_verdict(self):
        """18. replay fails when uninvoked agent has verdict in matrix."""
        import hashlib
        temp_package = os.path.join(self.temp_package_dir, "uninvoked_verdict")
        shutil.copytree(self.valid_package, temp_package)
        
        # Write transcript
        with open(os.path.join(temp_package, "transcript.jsonl"), "w") as f:
            f.write('{"step": 1}\n')
            
        verdict_file = os.path.join(temp_package, "00_executive_verdict.md")
        with open(verdict_file, "r", encoding="utf-8") as f:
            content = f.read()
            
        # Tamper it by changing "Yes" to "No" for DevOps / Ops
        old_row = "| **DevOps / Ops** | Operations Specialist | Critical | Yes | `af84b157-43fb-4a8c-9fc1-f7f873647836` | **COND. APPROVE** | 0.95 | No automated test runner; empty manifests; manual rollbacks. | No |"
        new_row = "| **DevOps / Ops** | Operations Specialist | Critical | No | `af84b157-43fb-4a8c-9fc1-f7f873647836` | **COND. APPROVE** | 0.95 | No automated test runner; empty manifests; manual rollbacks. | No |"
        self.assertIn(old_row, content)
        tampered_content = content.replace(old_row, new_row)
        
        with open(verdict_file, "w", encoding="utf-8") as f:
            f.write(tampered_content)
            
        normalized_content = tampered_content.replace("\r\n", "\n").encode("utf-8")
        h = hashlib.sha256(normalized_content).hexdigest().upper()
        
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        data["package_path"] = temp_package
        data["file_hashes"]["00_executive_verdict.md"] = h
        
        # Update package_integrity.md
        with open(self.valid_manifest, "r") as f:
            orig_data = json.load(f)
        orig_hash = orig_data["file_hashes"]["00_executive_verdict.md"]
        
        integrity_file = os.path.join(temp_package, "package_integrity.md")
        with open(integrity_file, "r", encoding="utf-8") as f:
            integrity_content = f.read()
        integrity_content = integrity_content.replace(orig_hash, h)
        with open(integrity_file, "w", encoding="utf-8") as f:
            f.write(integrity_content)
            
        normalized_integrity = integrity_content.replace("\r\n", "\n").encode("utf-8")
        h_integrity = hashlib.sha256(normalized_integrity).hexdigest().upper()
        data["file_hashes"]["package_integrity.md"] = h_integrity
        
        with open(manifest_file, "w") as f:
            json.dump(data, f)
            
        res = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 1)
        self.assertIn("REPLAY_FAIL: Non-invoked agent", res.stdout)

    def test_19_replay_fails_on_inconsistent_manifest_reference(self):
        """19. replay fails when manifest file hashes mismatch package_integrity.md."""
        temp_package = os.path.join(self.temp_package_dir, "inconsistent_manifest")
        shutil.copytree(self.valid_package, temp_package)
        
        # Write transcript
        with open(os.path.join(temp_package, "transcript.jsonl"), "w") as f:
            f.write('{"step": 1}\n')
            
        # Tamper package_integrity.md for 00_executive_verdict.md but keep manifest/disk hash matching
        with open(self.valid_manifest, "r") as f:
            orig_data = json.load(f)
        orig_hash = orig_data["file_hashes"]["00_executive_verdict.md"]
        
        integrity_file = os.path.join(temp_package, "package_integrity.md")
        with open(integrity_file, "r", encoding="utf-8") as f:
            integrity_content = f.read()
            
        # Replace actual hash of 00_executive_verdict.md with DEADBEEF... in package_integrity.md
        tampered_integrity = integrity_content.replace(orig_hash, "DEADBEEF1234567890ABCDEFDEADBEEF1234567890ABCDEFDEADBEEF12345678")
        with open(integrity_file, "w", encoding="utf-8") as f:
            f.write(tampered_integrity)
            
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        data["package_path"] = temp_package
        with open(manifest_file, "w") as f:
            json.dump(data, f)
            
        res = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 1)
        self.assertIn("REPLAY_FAIL: inconsistent manifest reference", res.stdout)

    def test_20_agents_command_prints_registry(self):
        """20. agents command prints registry, count, and validation status."""
        res = self.run_cli(["agents"])
        self.assertEqual(res.returncode, 0)
        self.assertIn("Canonical 36 Registry:", res.stdout)
        self.assertIn("Canonical Count: 36", res.stdout)
        self.assertIn("Registry Validation Status: PASS", res.stdout)
        # Ensure it doesn't mention any live run-time fabrication or fake active runs
        self.assertNotIn("active runs found", res.stdout.lower())

    def test_21_verify_fails_strict_m8_manifest_on_unknown_agents(self):
        """21. verify fails a strict M8 manifest containing unknown agents."""
        temp_package = os.path.join(self.temp_package_dir, "m8_unknown_agent")
        shutil.copytree(self.valid_package, temp_package)
        
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        
        data["run_id"] = "run-20260621-anti-m8-strict-run"
        data["package_path"] = temp_package
        data["required_agents"].append("unknown_anti_agent_x")
        data["invoked_agents"].append("unknown_anti_agent_x")
        
        # We need to re-sign package_integrity.md since manifest changed, but verify_package will load manifest first
        # Let's save manifest
        with open(manifest_file, "w") as f:
            json.dump(data, f)
            
        res = self.run_cli(["verify", "--package", temp_package])
        self.assertEqual(res.returncode, 1)
        self.assertIn("Verification FAILED!", res.stdout)
        self.assertIn("unknown_anti_agent_x", res.stdout)

    def test_22_verify_legacy_compat_mode_warnings_only(self):
        """22. verify treats unknown agents as warnings only in legacy mode."""
        temp_package = os.path.join(self.temp_package_dir, "legacy_unknown_agent")
        shutil.copytree(self.valid_package, temp_package)
        
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        
        # Ensure it is legacy
        data["run_id"] = "run-20260621-anti-m3-m7-legacy"
        data["package_path"] = temp_package
        data["required_agents"].append("unknown_legacy_agent")
        data["invoked_agents"].append("unknown_legacy_agent")
        
        # Re-sign run_manifest.json in package_integrity.md to keep integrity verification happy
        with open(manifest_file, "w") as f:
            json.dump(data, f)
            
        # We update the hashes of run_manifest.json in package_integrity.md
        import hashlib
        with open(manifest_file, "rb") as f:
            content = f.read().replace(b"\r\n", b"\n")
            h_manifest = hashlib.sha256(content).hexdigest().upper()
            
        integrity_file = os.path.join(temp_package, "package_integrity.md")
        with open(integrity_file, "r") as f:
            int_content = f.read()
            
        # Replace the manifest hash in integrity content
        pattern = re.compile(r"\|\s*`?run_manifest\.json`?\s*\|\s*`?([A-Fa-f0-9]+)`?\s*\|")
        match = pattern.search(int_content)
        if match:
            int_content = int_content.replace(match.group(1), h_manifest)
            
        with open(integrity_file, "w") as f:
            f.write(int_content)
            
        # Now update the hash of package_integrity.md in run_manifest.json
        with open(integrity_file, "rb") as f:
            content_int = f.read().replace(b"\r\n", b"\n")
            h_integrity = hashlib.sha256(content_int).hexdigest().upper()
            
        data["file_hashes"]["run_manifest.json"] = h_manifest
        data["file_hashes"]["package_integrity.md"] = h_integrity
        with open(manifest_file, "w") as f:
            json.dump(data, f)
            
        res = self.run_cli(["verify", "--package", temp_package])
        # In legacy mode, this should pass because it only produces warnings, not failures.
        self.assertEqual(res.returncode, 0)
        self.assertIn("Verification PASSED!", res.stdout)
        self.assertIn("Warning: Required agent 'unknown_legacy_agent' is not in the canonical registry", res.stdout)

    def test_23_replay_fails_on_tampered_transcript_hash_chain(self):
        """23. replay fails on tampered transcript hash chain."""
        import hashlib
        temp_package = os.path.join(self.temp_package_dir, "tampered_transcript_cli")
        shutil.copytree(self.valid_package, temp_package)
        
        # Create transcripts folder
        os.makedirs(os.path.join(temp_package, "transcripts"), exist_ok=True)
        transcript_path = os.path.join(temp_package, "transcripts", "transcript.jsonl")
        
        event1 = {
            "event_id": "evt-01",
            "run_id": "cli-tamper-run",
            "runner_component": "bootstrap",
            "timestamp": "2026-06-21T21:00:00Z",
            "backend": "local_ephemeral",
            "sandbox_mode": "NOT_NATIVE_SANDBOX",
            "command_summary": "bootstrap",
            "input_refs": [],
            "output_refs": [],
            "stdout_ref": "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
            "stderr_ref": "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
            "exit_code": 0,
            "file_deltas": {},
            "network_policy": "none",
            "forbidden_write_attempts": [],
            "registry_hash": "SOME_REGISTRY_HASH",
            "transcript_hash_prev": ""
        }
        str1 = json.dumps(event1, sort_keys=True)
        h1 = hashlib.sha256(str1.encode('utf-8')).hexdigest().upper()
        event1["transcript_hash"] = h1
        
        event2 = {
            "event_id": "evt-02",
            "run_id": "cli-tamper-run",
            "runner_component": "executor",
            "timestamp": "2026-06-21T21:01:00Z",
            "backend": "local_ephemeral",
            "sandbox_mode": "NOT_NATIVE_SANDBOX",
            "command_summary": "executor run",
            "input_refs": [],
            "output_refs": [],
            "stdout_ref": "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
            "stderr_ref": "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
            "exit_code": 0,
            "file_deltas": {},
            "network_policy": "none",
            "forbidden_write_attempts": [],
            "registry_hash": "SOME_REGISTRY_HASH",
            "transcript_hash_prev": h1
        }
        str2 = json.dumps(event2, sort_keys=True)
        h2 = hashlib.sha256(str2.encode('utf-8') + h1.encode('utf-8')).hexdigest().upper()
        event2["transcript_hash"] = h2
        
        with open(transcript_path, 'w', encoding='utf-8') as f:
            f.write(json.dumps(event1) + "\n")
            f.write(json.dumps(event2) + "\n")
            
        manifest_file = os.path.join(temp_package, "run_manifest.json")
        with open(manifest_file, "r") as f:
            data = json.load(f)
        data["package_path"] = temp_package
        with open(manifest_file, "w") as f:
            json.dump(data, f)
            
        # First verify it passes replay
        res = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res.returncode, 0)
        self.assertIn("REPLAY_PASS", res.stdout)
        
        # Now tamper the second event
        event2["command_summary"] = "tampered command execution details"
        with open(transcript_path, 'w', encoding='utf-8') as f:
            f.write(json.dumps(event1) + "\n")
            f.write(json.dumps(event2) + "\n")
            
        res2 = self.run_cli(["replay", "--manifest", manifest_file])
        self.assertEqual(res2.returncode, 1)
        self.assertIn("REPLAY_FAIL: transcript hash mismatch", res2.stdout)

if __name__ == '__main__':
    unittest.main()
