#!/usr/bin/env python3
"""
G10 - Pre-Gate AI Inactivity verification (evidence harness).

Static check that the offline Creativity MVP keeps every AI / network / telemetry capability
disabled by default and that no AI runtime is wired in. Prints PASS/FAIL per check and exits
non-zero on any failure.

This is the command named for gate G10 in
`.antigravity-only/plans/current-plan-review-evidence-based_20260621_050307/14_evidence_manifest.md`.
It PRODUCES evidence; it does NOT itself flip the gate to PASSED — that needs human sign-off
(no self-certification, per 12_final_signoff.md / 15_package_integrity.md).
"""
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
CREATIVITY = REPO / "shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/creativity"
POLICY = CREATIVITY / "config/CreativityRuntimePolicy.kt"
GATES = CREATIVITY / "config/CreativityEvidenceGate.kt"

FORBIDDEN_IMPORT = re.compile(
    r"^\s*import\s+.*("
    r"okhttp|retrofit|io\.ktor|firebase|firestore|openai|anthropic|gemini|"
    r"java\.net|\bllm\b|embedding|inference"
    r")",
    re.IGNORECASE,
)

failures = []


def check(name, ok, detail=""):
    print(f"[{'PASS' if ok else 'FAIL'}] {name}" + (f" -- {detail}" if detail else ""))
    if not ok:
        failures.append(name)


def main():
    if not CREATIVITY.exists():
        print(f"[FAIL] creativity package not found at {CREATIVITY}")
        return 1

    # 1. Runtime capability flags are all hard-blocked (false).
    policy = POLICY.read_text(encoding="utf-8")
    for flag in (
        "AI_RUNTIME_ENABLED",
        "NETWORK_SYNC_ENABLED",
        "REMOTE_TELEMETRY_ENABLED",
        "EVIDENCE_GATES_PASSED",
    ):
        m = re.search(rf"const val {flag}\s*:\s*Boolean\s*=\s*(true|false)", policy)
        check(f"{flag} == false", bool(m) and m.group(1) == "false",
              "flag not found" if not m else "")

    # 2. No network / AI / inference imports anywhere in the creativity package.
    offending = []
    for path in sorted(CREATIVITY.rglob("*.kt")):
        for i, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if FORBIDDEN_IMPORT.match(line):
                offending.append(f"{path.relative_to(REPO)}:{i}")
    check("no network/AI/inference imports in creativity package",
          not offending, "; ".join(offending))

    # 3. No evidence gate is constructed as PASSED in source (no self-certification).
    gates = GATES.read_text(encoding="utf-8")
    no_passed = re.search(r"state\s*=\s*CreativityGateState\.PASSED", gates) is None
    check("no gate constructed as PASSED in source", no_passed)

    print()
    if failures:
        print(f"G10 PRE-GATE AI INACTIVITY: FAIL ({len(failures)} check(s) failed)")
        return 1
    print("G10 PRE-GATE AI INACTIVITY: PASS "
          "(evidence generated; gate stays DESIGNED pending human sign-off)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
