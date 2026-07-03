from __future__ import annotations

import pathlib
import sys
import tomllib


ROOT = pathlib.Path(__file__).resolve().parents[2]
AGENTS_DIR = ROOT / ".codex" / "agents"
EXPECTED = {
    "security_orchestrator",
    "security_lead_ciso",
    "appsec_auditor",
    "secure_code_remediator",
    "threat_modeling_agent",
    "dependency_supply_chain",
    "security_qa_verifier",
    "documentation_sanitizer",
    "incident_response_commander",
    "red_team_lab_authorized",
    "cloud_security_auditor",
    "iam_secrets_crypto_auditor",
    "devsecops_cicd_auditor",
    "detection_engineering",
    "blue_team_soc",
    "forensics_agent",
    "compliance_privacy_agent",
}
ALLOWED_KEYS = {"name", "description", "developer_instructions"}
REQUIRED_DOCS = {
    "agentic-security-architecture.md",
    "agent-rbac-matrix.md",
    "workflows.md",
    "evidence-ledger-spec.md",
    "memory-policy.md",
    "dual-use-roe-template.md",
    "evals-agentic-security.md",
    "checklist-top-security.md",
    "legacy-agents-risk.md",
    "codex-user-config-recommended.toml",
}
REQUIRED_CONTROLS = {
    "writer-lock.ps1",
    "secret-path-guard.ps1",
    "egress-allowlist-check.ps1",
    "dlp-output-check.ps1",
    "evidence-ledger-append.ps1",
    "agent-receipt-validator.ps1",
}


def fail(errors: list[str]) -> None:
    for error in errors:
        print(f"ERROR {error}")
    raise SystemExit(1)


def main() -> None:
    errors: list[str] = []
    parsed: dict[str, dict[str, object]] = {}
    for path in sorted(AGENTS_DIR.glob("*.toml")):
        try:
            data = tomllib.loads(path.read_text(encoding="utf-8"))
        except Exception as exc:  # parser evidence, never silently ignored
            errors.append(f"{path.name}: invalid TOML: {exc}")
            continue
        extra = set(data) - ALLOWED_KEYS
        missing = ALLOWED_KEYS - set(data)
        if extra or missing:
            errors.append(f"{path.name}: keys extra={sorted(extra)} missing={sorted(missing)}")
        parsed[path.stem] = data

    missing_agents = EXPECTED - set(parsed)
    if missing_agents:
        errors.append(f"missing security agents: {sorted(missing_agents)}")

    for stem in EXPECTED & set(parsed):
        text = str(parsed[stem].get("developer_instructions", ""))
        lowered = text.lower()
        for marker in ("codex-only", "untrusted content", "secrets:", "fail closed", "receipt:"):
            if marker not in lowered:
                errors.append(f"{stem}: missing marker {marker!r}")
        if "recursive delegation" not in lowered and "spawn agents" not in lowered:
            errors.append(f"{stem}: recursive delegation is not denied")
        if stem != "secure_code_remediator" and "read-only" not in lowered:
            errors.append(f"{stem}: logical read-only role is not explicit")

    remediator = str(parsed.get("secure_code_remediator", {}).get("developer_instructions", "")).lower()
    for marker in ("autonomy: a2", "writer lock", "human-approved", "cannot do", "verify its own"):
        if marker not in remediator:
            errors.append(f"secure_code_remediator: missing segregation marker {marker!r}")

    config_path = ROOT / ".codex" / "config.toml"
    try:
        config = tomllib.loads(config_path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"config.toml invalid: {exc}")
        config = {}
    features = config.get("features", {})
    v2 = features.get("multi_agent_v2", {}) if isinstance(features, dict) else {}
    if v2.get("enabled") is not True or v2.get("max_concurrent_threads_per_session") != 6:
        errors.append("config.toml must enable V2 with concurrency cap 6")
    legacy = config.get("agents", {})
    if isinstance(legacy, dict) and ({"max_threads", "max_depth"} & set(legacy)):
        errors.append("config.toml mixes legacy V1 agent controls with V2")
    if config.get("sandbox_mode") != "read-only" or config.get("approval_policy") != "on-request":
        errors.append("config.toml must default to read-only/on-request")

    docs_dir = ROOT / "docs" / "security"
    missing_docs = [name for name in sorted(REQUIRED_DOCS) if not (docs_dir / name).is_file()]
    if missing_docs:
        errors.append(f"missing security docs: {missing_docs}")
    controls_dir = ROOT / "scripts" / "security"
    missing_controls = [name for name in sorted(REQUIRED_CONTROLS) if not (controls_dir / name).is_file()]
    if missing_controls:
        errors.append(f"missing controls: {missing_controls}")

    policy = (ROOT / "AGENTS.md").read_text(encoding="utf-8")
    if policy.count("<!-- BEGIN CODEX_SECURITY_AGENTIC_PACKAGE -->") != 1 or policy.count("<!-- END CODEX_SECURITY_AGENTIC_PACKAGE -->") != 1:
        errors.append("AGENTS.md must contain exactly one delimited public security block")
    if "secure_code_remediator` es el unico agente" not in policy.lower().replace("ún", "un"):
        errors.append("AGENTS.md does not declare the single logical writer")

    ignore = (ROOT / ".gitignore").read_text(encoding="utf-8")
    for stem in EXPECTED:
        if f"!.codex/agents/{stem}.toml" not in ignore:
            errors.append(f".gitignore does not expose public manifest {stem}")

    if errors:
        fail(errors)
    legacy_count = len(parsed) - len(EXPECTED)
    if legacy_count != 51:
        fail([f"legacy agent inventory drifted: expected 51, found {legacy_count}"])
    print(
        f"OK agent_package files={len(parsed)} new_agents={len(EXPECTED)} "
        f"legacy_agents={legacy_count} v2_cap=6 single_writer=secure_code_remediator"
    )


if __name__ == "__main__":
    main()
