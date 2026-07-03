import unittest
from anti_agent_registry import (
    AntiAgent,
    CANONICAL_ROSTER,
    load_canonical_agents,
    validate_agent_registry,
    reconcile_agents_against_manifest,
    STRICT_CANONICAL_36_MODE,
    LEGACY_COMPAT_MODE
)

class TestAntiAgentRegistry(unittest.TestCase):
    def test_exact_36_registry_load(self):
        """Verify registry contains exactly 36 agents and loads correctly."""
        agents = load_canonical_agents()
        self.assertEqual(len(agents), 36)
        
    def test_canonical_count_validation(self):
        """Verify validation succeeds on canonical roster."""
        res = validate_agent_registry()
        self.assertEqual(res["status"], "PASS")
        self.assertEqual(res["agent_count"], 36)
        
    def test_duplicate_id_rejection(self):
        """Verify duplicate IDs are rejected."""
        dup = AntiAgent(
            agent_id="chief_orchestrator_agent",
            name="Duplicate Chief",
            role="Duplicate Role",
            required=False,
            criticality="standard",
            evidence_required=False,
            source_files=("some_file.md",)
        )
        CANONICAL_ROSTER.append(dup)
        try:
            res = validate_agent_registry()
            self.assertEqual(res["status"], "FAIL")
            self.assertTrue(any("Duplicate agent_id found" in err for err in res["errors"]))
        finally:
            CANONICAL_ROSTER.pop()
            
    def test_duplicate_name_rejection(self):
        """Verify duplicate names are rejected."""
        dup = AntiAgent(
            agent_id="some_unique_id",
            name="Chief Orchestrator Agent",
            role="Duplicate Role",
            required=False,
            criticality="standard",
            evidence_required=False,
            source_files=("some_file.md",)
        )
        CANONICAL_ROSTER.append(dup)
        try:
            res = validate_agent_registry()
            self.assertEqual(res["status"], "FAIL")
            self.assertTrue(any("Duplicate agent display name found" in err for err in res["errors"]))
        finally:
            CANONICAL_ROSTER.pop()

    def test_empty_field_rejection(self):
        """Verify empty fields are rejected."""
        dup = AntiAgent(
            agent_id="",
            name="Some Name",
            role="Some Role",
            required=False,
            criticality="standard",
            evidence_required=False,
            source_files=("some_file.md",)
        )
        CANONICAL_ROSTER.append(dup)
        try:
            res = validate_agent_registry()
            self.assertEqual(res["status"], "FAIL")
            self.assertTrue(any("empty agent_id" in err for err in res["errors"]))
        finally:
            CANONICAL_ROSTER.pop()

    def test_placeholder_rejection(self):
        """Verify placeholder values are rejected."""
        dup = AntiAgent(
            agent_id="placeholder_agent",
            name="[Placeholder Name]",
            role="Some Role",
            required=False,
            criticality="standard",
            evidence_required=False,
            source_files=("some_file.md",)
        )
        CANONICAL_ROSTER.append(dup)
        try:
            res = validate_agent_registry()
            self.assertEqual(res["status"], "FAIL")
            self.assertTrue(any("placeholder" in err for err in res["errors"]))
        finally:
            CANONICAL_ROSTER.pop()

    def test_missing_source_reference_rejection(self):
        """Verify missing source files are rejected."""
        dup = AntiAgent(
            agent_id="no_source_agent",
            name="No Source Agent",
            role="Some Role",
            required=False,
            criticality="standard",
            evidence_required=False,
            source_files=()
        )
        CANONICAL_ROSTER.append(dup)
        try:
            res = validate_agent_registry()
            self.assertEqual(res["status"], "FAIL")
            self.assertTrue(any("no source file references" in err for err in res["errors"]))
        finally:
            CANONICAL_ROSTER.pop()

    def test_legacy_manifest_compatibility_warning(self):
        """Verify legacy manifests with unknown agents report warnings, not failures."""
        manifest = {
            "required_agents": ["chief_orchestrator_agent", "unknown_legacy_agent"],
            "invoked_agents": ["chief_orchestrator_agent"],
            "failed_agents": []
        }
        res = reconcile_agents_against_manifest(manifest, mode=LEGACY_COMPAT_MODE)
        self.assertEqual(res["status"], "PASS")
        self.assertTrue(any("unknown_legacy_agent" in w for w in res["warnings"]))

    def test_strict_manifest_failure_on_unknown_agents(self):
        """Verify strict mode fails on unknown agents in manifest."""
        manifest = {
            "required_agents": ["chief_orchestrator_agent", "unknown_agent"],
            "invoked_agents": ["chief_orchestrator_agent"],
            "failed_agents": []
        }
        res = reconcile_agents_against_manifest(manifest, mode=STRICT_CANONICAL_36_MODE)
        self.assertEqual(res["status"], "FAIL")
        self.assertTrue(any("unknown_agent" in f for f in res["failures"]))

    def test_invoked_agent_not_in_registry(self):
        """Verify strict mode fails when invoked agent is not in registry."""
        manifest = {
            "required_agents": ["chief_orchestrator_agent"],
            "invoked_agents": ["chief_orchestrator_agent", "unknown_invoked_agent"],
            "failed_agents": []
        }
        res = reconcile_agents_against_manifest(manifest, mode=STRICT_CANONICAL_36_MODE)
        self.assertEqual(res["status"], "FAIL")
        self.assertTrue(any("unknown_invoked_agent" in f for f in res["failures"]))

    def test_failed_agent_not_invoked(self):
        """Verify logic check fails when a failed agent was not even invoked."""
        manifest = {
            "required_agents": ["chief_orchestrator_agent"],
            "invoked_agents": ["chief_orchestrator_agent"],
            "failed_agents": ["red_team_devil_advocate_agent"]
        }
        res = reconcile_agents_against_manifest(manifest, mode=STRICT_CANONICAL_36_MODE)
        self.assertEqual(res["status"], "FAIL")
        self.assertTrue(any("Logic Error: Failed agent 'red_team_devil_advocate_agent' was not even invoked" in f for f in res["failures"]))

if __name__ == "__main__":
    unittest.main()
