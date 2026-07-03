import os
import unittest
import tempfile
import shutil
from pathlib import Path
from sandbox_enforcer import (
    AntiSandboxViolation,
    load_policy,
    normalize_path,
    resolve_real_path,
    is_denied,
    assert_allowed,
    safe_open,
    safe_read_text,
    safe_write_text
)

class TestSandboxEnforcement(unittest.TestCase):
    def setUp(self):
        # Load standard policy
        self.policy = load_policy()
        # Set up a temporary directory within approved root (e.g. inside .antigravity-only/plans/ or workspace scratch)
        self.repo_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
        self.scratch_dir = os.path.join(self.repo_root, 'scratch', 'test_sandbox_run')
        os.makedirs(self.scratch_dir, exist_ok=True)

    def tearDown(self):
        # Clean up temp files
        if os.path.exists(self.scratch_dir):
            shutil.rmtree(self.scratch_dir)

    def test_01_read_env_blocked(self):
        """1. Reading .env is blocked."""
        env_path = os.path.join(self.scratch_dir, '.env')
        with self.assertRaises(AntiSandboxViolation) as ctx:
            assert_allowed(env_path, 'read', self.policy)
        self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")

    def test_02_read_env_local_blocked(self):
        """2. Reading .env.local is blocked."""
        env_local_path = os.path.join(self.scratch_dir, '.env.local')
        with self.assertRaises(AntiSandboxViolation) as ctx:
            assert_allowed(env_local_path, 'read', self.policy)
        self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")

    def test_03_read_google_services_blocked(self):
        """3. Reading google-services.json is blocked."""
        gs_path = os.path.join(self.scratch_dir, 'google-services.json')
        with self.assertRaises(AntiSandboxViolation) as ctx:
            assert_allowed(gs_path, 'read', self.policy)
        self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")

    def test_04_read_pem_blocked(self):
        """4. Reading *.pem is blocked."""
        pem_path = os.path.join(self.scratch_dir, 'cert.pem')
        with self.assertRaises(AntiSandboxViolation) as ctx:
            assert_allowed(pem_path, 'read', self.policy)
        self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")

    def test_05_read_key_blocked(self):
        """5. Reading *.key is blocked."""
        key_path = os.path.join(self.scratch_dir, 'private.key')
        with self.assertRaises(AntiSandboxViolation) as ctx:
            assert_allowed(key_path, 'read', self.policy)
        self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")

    def test_06_relative_traversal_blocked(self):
        """6. Relative traversal to denied path is blocked."""
        # E.g. scratch/test_sandbox_run/../../.env -> resolved to repo_root/.env
        traversal_path = os.path.join(self.scratch_dir, '..', '..', '.env')
        with self.assertRaises(AntiSandboxViolation) as ctx:
            assert_allowed(traversal_path, 'read', self.policy)
        self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")

    def test_07_case_variant_blocked(self):
        """7. Case-variant denied path is blocked on Windows."""
        case_env_path = os.path.join(self.scratch_dir, '.ENV')
        with self.assertRaises(AntiSandboxViolation) as ctx:
            assert_allowed(case_env_path, 'read', self.policy)
        self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")
        
        case_gs_path = os.path.join(self.scratch_dir, 'GOOGLE-SERVICES.JSON')
        with self.assertRaises(AntiSandboxViolation) as ctx:
            assert_allowed(case_gs_path, 'read', self.policy)
        self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")

    def test_08_symlink_traversal_blocked(self):
        """8. Symlink/junction to denied path is blocked where supported."""
        # Create a mock target (.env) and symlink it
        target_path = os.path.join(self.scratch_dir, 'target_secret.key')
        link_path = os.path.join(self.scratch_dir, 'symlink_to_secret')
        
        # We write dummy data
        with open(target_path, 'w') as f:
            f.write("secret_data")
            
        try:
            os.symlink(target_path, link_path)
            # If symlink succeeds, assert read through symlink is blocked
            with self.assertRaises(AntiSandboxViolation) as ctx:
                assert_allowed(link_path, 'read', self.policy)
            self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")
        except (OSError, NotImplementedError):
            # Skip test if OS permissions block symlink creation
            pass

    def test_09_write_outside_package_root_blocked(self):
        """9. Writing outside package root is blocked."""
        # E.g. trying to write to build.gradle.kts at the repo root
        outside_path = os.path.join(self.repo_root, 'build.gradle.kts')
        with self.assertRaises(AntiSandboxViolation) as ctx:
            assert_allowed(outside_path, 'write', self.policy)
        self.assertEqual(str(ctx.exception), "SECURITY_BLOCKED: denied path access")

    def test_10_allowed_package_read_succeeds(self):
        """10. Allowed package read succeeds."""
        # Any file in .antigravity-only/ is allowed for reading (unless matches denypatterns)
        allowed_path = os.path.join(self.repo_root, '.antigravity-only', 'VERSION.md')
        if os.path.exists(allowed_path):
            try:
                assert_allowed(allowed_path, 'read', self.policy)
            except AntiSandboxViolation:
                self.fail("assert_allowed raised AntiSandboxViolation on an allowed path read")

    def test_11_allowed_package_write_succeeds(self):
        """11. Allowed package write succeeds."""
        # Writing to a path inside .antigravity-only/plans/ is allowed
        allowed_write_path = os.path.join(self.repo_root, '.antigravity-only', 'plans', 'temp_test_file.md')
        try:
            assert_allowed(allowed_write_path, 'write', self.policy)
        except AntiSandboxViolation:
            self.fail("assert_allowed raised AntiSandboxViolation on an allowed path write")

    def test_12_violation_error_sanitized(self):
        """12. Violation error message is sanitized."""
        # Error must contain "SECURITY_BLOCKED: denied path access" and NOT leak path
        sensitive_path = os.path.join(self.scratch_dir, 'very_sensitive_filename_secret.keystore')
        try:
            assert_allowed(sensitive_path, 'read', self.policy)
        except AntiSandboxViolation as e:
            msg = str(e)
            self.assertIn("SECURITY_BLOCKED", msg)
            self.assertNotIn("very_sensitive_filename_secret", msg)
            self.assertNotIn("scratch", msg)

    def test_13_subprocess_escape_not_blocked(self):
        """13. Subprocess execution is NOT blocked (Partial Sandbox constraint)."""
        import sys
        import subprocess
        # Run a simple echo command to verify that subprocess execution is not intercepted/blocked
        # This demonstrates the PARTIAL_ENFORCEMENT_BOUNDARY limitation.
        try:
            res = subprocess.run([sys.executable, "-c", "print('PARTIAL_ENFORCEMENT_BOUNDARY')"], capture_output=True, text=True)
            self.assertEqual(res.stdout.strip(), "PARTIAL_ENFORCEMENT_BOUNDARY")
        except Exception:
            self.fail("Subprocess execution failed, but it should not be blocked by the Python-level sandbox")

    def test_14_windows_separator_normalization(self):
        """14. Windows separator normalization handles slashes consistently."""
        # Mix slashes and backslashes in blocked path to ensure it is normcased & caught
        mixed_path = os.path.join(self.scratch_dir, "subdir/nested\\.env").replace('\\', '/')
        with self.assertRaises(AntiSandboxViolation):
            assert_allowed(mixed_path, 'read', self.policy)

if __name__ == '__main__':
    unittest.main()
