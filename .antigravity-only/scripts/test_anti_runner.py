import os
import sys
import unittest
import subprocess
import json
import shutil
import hashlib
from datetime import datetime

# Add script folder to python path to import modules directly for testing
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from verify_subagents import verify_transcript_hash_chain

class TestAntiRunner(unittest.TestCase):
    def setUp(self):
        self.repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        self.runner_path = os.path.join(self.repo_root, '.antigravity-only', 'scripts', 'anti_runner.py')
        
        # Temp dir for test outputs
        self.temp_dir = os.path.normpath(os.path.join(self.repo_root, 'scratch', 'runner_test_output'))
        os.makedirs(self.temp_dir, exist_ok=True)
        
    def tearDown(self):
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)

    def run_runner(self, args):
        cmd = [sys.executable, self.runner_path] + args
        env = os.environ.copy()
        env["PYTHONPATH"] = os.path.dirname(self.runner_path)
        result = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, env=env)
        return result

    def test_01_probe_does_not_crash(self):
        """1. probe command runs successfully and prints required fields."""
        res = self.run_runner(["probe"])
        self.assertEqual(res.returncode, 0)
        self.assertIn("Docker:", res.stdout)
        self.assertIn("WSL2:", res.stdout)
        self.assertIn("fallback_local_ephemeral_workspace:", res.stdout)
        self.assertIn("recommended_backend:", res.stdout)

    def test_02_run_missing_manifest_fails(self):
        """2. run fails closed on missing manifest file."""
        res = self.run_runner(["run", "--manifest", "nonexistent.json", "--mode", "poc"])
        self.assertEqual(res.returncode, 1)
        self.assertIn("Error: Manifest file does not exist", res.stderr)

    def test_03_run_invalid_agents_fails(self):
        """3. run fails when registry fails or manifest has non-reconciling agents."""
        manifest_path = os.path.join(self.temp_dir, "invalid_manifest.json")
        manifest_data = {
            "run_id": "test-run-invalid-agents",
            "package_path": self.temp_dir,
            "required_agents": ["unknown_agent_foo"],
            "invoked_agents": ["unknown_agent_foo"],
            "failed_agents": [],
            "final_status": "GO_INTERNAL_VERIFIED",
            "verification_status": "PASS",
            "file_hashes": {}
        }
        with open(manifest_path, 'w', encoding='utf-8') as f:
            json.dump(manifest_data, f, indent=2)
            
        res = self.run_runner(["run", "--manifest", manifest_path, "--mode", "poc"])
        self.assertEqual(res.returncode, 1)
        self.assertIn("Error: Manifest agent reconciliation failed", res.stderr)

    def test_04_run_success_creates_files_and_chains_hashes(self):
        """4. run command creates transcripts/evidence and validates hash chain."""
        from anti_agent_registry import load_canonical_agents
        canonical_agents = load_canonical_agents()
        agent_ids = [agent.agent_id for agent in canonical_agents[:3]]
        
        manifest_path = os.path.join(self.temp_dir, "valid_run_manifest.json")
        package_path = os.path.join(self.temp_dir, "m9b_package")
        os.makedirs(package_path, exist_ok=True)
        
        manifest_data = {
            "run_id": "test-run-m9b-poc",
            "package_path": package_path,
            "required_agents": agent_ids,
            "invoked_agents": agent_ids,
            "failed_agents": [],
            "final_status": "GO_INTERNAL_VERIFIED",
            "verification_status": "PASS",
            "file_hashes": {}
        }
        with open(manifest_path, 'w', encoding='utf-8') as f:
            json.dump(manifest_data, f, indent=2)

        res = self.run_runner(["run", "--manifest", manifest_path, "--mode", "poc"])
        self.assertEqual(res.returncode, 0)
        self.assertIn("POC execution completed successfully", res.stdout)
        
        ledger_path = os.path.join(package_path, "evidence_ledger.md")
        self.assertTrue(os.path.exists(ledger_path))
        with open(ledger_path, 'r', encoding='utf-8') as f:
            ledger_content = f.read()
            
        self.assertIn("LOCAL_EPHEMERAL_WORKSPACE_ONLY", ledger_content)
        self.assertIn("NOT_NATIVE_SANDBOX", ledger_content)
        self.assertIn("NO_GO_HIGH_STAKES", ledger_content)
        self.assertIn("CANONICAL_36_CONFIRMED", ledger_content)
        
        transcript_path = os.path.join(package_path, "transcripts", "transcript.jsonl")
        self.assertTrue(os.path.exists(transcript_path))
        
        valid, msg = verify_transcript_hash_chain(transcript_path)
        self.assertTrue(valid, f"Hash chain verification failed: {msg}")
        
        with open(transcript_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        self.assertEqual(len(lines), 2)
        
        for line in lines:
            event = json.loads(line.strip())
            self.assertEqual(event["backend"], "local_ephemeral")
            self.assertEqual(event["sandbox_mode"], "NOT_NATIVE_SANDBOX")
            self.assertIn("transcript_hash", event)
            self.assertIn("transcript_hash_prev", event)

    def test_05_tampered_transcript_fails_validation(self):
        """5. tampered transcript hash chain fails validation."""
        from anti_agent_registry import load_canonical_agents
        canonical_agents = load_canonical_agents()
        agent_ids = [agent.agent_id for agent in canonical_agents[:2]]
        
        manifest_path = os.path.join(self.temp_dir, "tamper_manifest.json")
        package_path = os.path.join(self.temp_dir, "tamper_package")
        os.makedirs(package_path, exist_ok=True)
        
        manifest_data = {
            "run_id": "test-run-tamper",
            "package_path": package_path,
            "required_agents": agent_ids,
            "invoked_agents": agent_ids,
            "failed_agents": [],
            "final_status": "GO_INTERNAL_VERIFIED",
            "verification_status": "PASS",
            "file_hashes": {}
        }
        with open(manifest_path, 'w', encoding='utf-8') as f:
            json.dump(manifest_data, f, indent=2)

        res = self.run_runner(["run", "--manifest", manifest_path, "--mode", "poc"])
        self.assertEqual(res.returncode, 0)
        
        transcript_path = os.path.join(package_path, "transcripts", "transcript.jsonl")
        self.assertTrue(os.path.exists(transcript_path))
        
        with open(transcript_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        event2 = json.loads(lines[1].strip())
        event2["command_summary"] = "tampered command execution"
        lines[1] = json.dumps(event2) + "\n"
        
        with open(transcript_path, 'w', encoding='utf-8') as f:
            f.writelines(lines)
            
        valid, msg = verify_transcript_hash_chain(transcript_path)
        self.assertFalse(valid)
        self.assertIn("Computed hash mismatch", msg)

    def test_06_run_denied_package_path_fails(self):
        """6. run fails if package_path is outside allowed write roots."""
        from anti_agent_registry import load_canonical_agents
        canonical_agents = load_canonical_agents()
        agent_ids = [agent.agent_id for agent in canonical_agents[:2]]
        
        manifest_path = os.path.join(self.temp_dir, "denied_manifest.json")
        denied_package_path = os.path.normpath(os.path.join(self.repo_root, "unauthorized_test_dir"))
        
        manifest_data = {
            "run_id": "test-run-denied",
            "package_path": denied_package_path,
            "required_agents": agent_ids,
            "invoked_agents": agent_ids,
            "failed_agents": [],
            "final_status": "GO_INTERNAL_VERIFIED",
            "verification_status": "PASS",
            "file_hashes": {}
        }
        with open(manifest_path, 'w', encoding='utf-8') as f:
            json.dump(manifest_data, f, indent=2)

        res = self.run_runner(["run", "--manifest", manifest_path, "--mode", "poc"])
        self.assertEqual(res.returncode, 1)
        self.assertIn("Error: Sandbox policy violation", res.stderr)

if __name__ == "__main__":
    unittest.main()
