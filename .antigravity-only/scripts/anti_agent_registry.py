from dataclasses import dataclass
from typing import List, Tuple, Dict, Any, Optional
import re

# Provenance metadata for registry source materials
SOURCE_FILES = (
    "03_agent_specialization_surgery.md",
    "11_final_upgraded_founder_council_plan.md"
)
SOURCE_HASHES = {
    "03_agent_specialization_surgery.md": "FA595289AC43498FFCEC899FBB82F69D6B4CF8C3A000467AFAE8CEC1D35A17C8",
    "11_final_upgraded_founder_council_plan.md": "1BF8EFFD0F3CC50AC81F38F2E7D5EAED959CED4867BD9839E4A736517FEA3D66"
}

# Reconciliation and validation modes
LEGACY_COMPAT_MODE = "LEGACY_COMPAT_MODE"
STRICT_CANONICAL_36_MODE = "STRICT_CANONICAL_36_MODE"

@dataclass(frozen=True)
class AntiAgent:
    agent_id: str
    name: str
    role: str
    required: bool
    criticality: str  # "critical" or "standard"
    evidence_required: bool
    source_files: Tuple[str, ...]

# Roster of the 36 canonical subagents
CANONICAL_ROSTER = [
    # Tier 0: Command & Governance
    AntiAgent(
        agent_id="chief_orchestrator_agent",
        name="Chief Orchestrator Agent",
        role="Orchestration, review mode coordinator, and report synthesizer",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="founder_strategy_agent",
        name="Founder Strategy Agent",
        role="Strategic focus, ROI assessment, and opportunity cost audit",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="red_team_devil_advocate_agent",
        name="Red Team Devil's Advocate Agent",
        role="Threat modeling, friction auditing, and risk analysis",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),

    # Tier 1: Science of the Mind
    AntiAgent(
        agent_id="cognitive_science_agent",
        name="Cognitive Science Agent",
        role="Attentional transfer and construct task validity validation",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="attention_control_agent",
        name="Attention Control Agent",
        role="Selective, sustained, and divided attention presentation loops review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="working_memory_agent",
        name="Working Memory Agent",
        role="Memory span, dual-task n-back capacity review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="executive_function_agent",
        name="Executive Function Agent",
        role="Stroop conflict and task set-shifting review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="stress_performance_agent",
        name="Stress Performance Agent",
        role="Time pressure calibration and stress recovery curves review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="fatigue_recovery_agent",
        name="Fatigue Recovery Agent",
        role="Session length limits and mental overtraining safety review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="metacognition_agent",
        name="Metacognition Agent",
        role="Confidence self-efficacy scaling and score explainability review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="psychometrics_measurement_agent",
        name="Psychometrics/Measurement Agent",
        role="Baseline formulas and statistical outlier filters audit",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="human_learning_transfer_agent",
        name="Human Learning/Transfer Agent",
        role="Practice effects and near-transfer mapping review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),

    # Tier 2: Product, Behavior & Experience
    AntiAgent(
        agent_id="product_strategy_agent",
        name="Product Strategy Agent",
        role="Moat differentiation and target user segment alignment audit",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="user_psychology_agent",
        name="User Psychology Agent",
        role="Behavioral feedback and motivational microcopy review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="behavior_design_agent",
        name="Behavior Design Agent",
        role="Cue-action habit triggers and daily retention loops review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="ux_ui_experience_agent",
        name="UX/UI Experience Agent",
        role="Layout interaction friction and onboarding visual guides review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="premium_design_agent",
        name="Premium Design Agent",
        role="Typography standards and color palette token enforcement audit",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="creative_narrative_agent",
        name="Creative Narrative Agent",
        role="Storytelling vocabulary boundaries and taglines review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="gamification_integrity_agent",
        name="Gamification Integrity Agent",
        role="Streak reward limits and scoring logic integrity review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),

    # Tier 3: Technology & Implementation
    AntiAgent(
        agent_id="technical_architecture_agent",
        name="Technical Architecture Agent",
        role="KMP offline-first modular boundaries audit",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="android_performance_agent",
        name="Android Performance Agent",
        role="Choreographer frame timing and rendering latency limits audit",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="exercise_engine_agent",
        name="Exercise Engine Agent",
        role="Trial state transitions and timing engine lifecycle review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="scoring_engine_agent",
        name="Scoring Engine Agent",
        role="SRI log-normal reaction time transformation formulas review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="local_storage_agent",
        name="Local Storage Agent",
        role="Room migration validation and SQLite query optimization review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="data_analytics_agent",
        name="Data/Analytics Agent",
        role="Telemetry normalization and local hourly bucket obfuscation review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="qa_evidence_agent",
        name="QA/Evidence Agent",
        role="Manual validation checklists and test coverage criteria audit",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="security_privacy_agent",
        name="Security/Privacy Agent",
        role="Android permission whitelist and SQLite DB encryption audit",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),

    # Tier 4: Business, Market & Growth
    AntiAgent(
        agent_id="business_monetization_agent",
        name="Business/Monetization Agent",
        role="Subscription willingness to pay and baseline timing gating review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="financial_analyst_agent",
        name="Financial Analyst Agent",
        role="Runway monthly resource burn prioritisations review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="growth_market_agent",
        name="Growth/Market Agent",
        role="Organic loop acquisition and viral sharing hooks review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="competitive_intelligence_agent",
        name="Competitive Intelligence Agent",
        role="White-space gap mapping against Peak/BrainHQ review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="positioning_category_agent",
        name="Positioning/Category Agent",
        role="Attentional readiness category vocabulary and positioning audit",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="customer_segment_agent",
        name="Customer Segment Agent",
        role="High-performer developer segment load tolerances review",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),

    # Tier 5: Legal, Ethics & Trust
    AntiAgent(
        agent_id="legal_claims_ethics_agent",
        name="Legal/Claims/Ethics Agent",
        role="FDA/FTC copy safety audit and diagnostic claims veto control",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="privacy_ethics_agent",
        name="Privacy Ethics Agent",
        role="Local opt-out consent toggles and telemetry tracking audit",
        required=False,
        criticality="standard",
        evidence_required=True,
        source_files=SOURCE_FILES
    ),
    AntiAgent(
        agent_id="stress_safety_agent",
        name="Stress Safety Agent",
        role="Photo-sensitivity and time-pressure safe opt-out review",
        required=True,
        criticality="critical",
        evidence_required=True,
        source_files=SOURCE_FILES
    )
]

def load_canonical_agents() -> List[AntiAgent]:
    """Returns the list of 36 canonical subagents."""
    return CANONICAL_ROSTER

def get_agent_by_id(agent_id: str) -> Optional[AntiAgent]:
    """Retrieves an agent from the roster by its unique ID."""
    for agent in CANONICAL_ROSTER:
        if agent.agent_id == agent_id:
            return agent
    return None

def validate_agent_registry() -> Dict[str, Any]:
    """
    Validates agent registry invariants:
    - Exactly 36 agents.
    - No duplicate IDs.
    - No duplicate names.
    - No empty fields.
    - No placeholder strings.
    - Every agent has at least one source file reference.
    """
    errors = []
    
    # 1. Size constraint
    if len(CANONICAL_ROSTER) != 36:
        errors.append(f"Registry does not contain exactly 36 agents (Current count: {len(CANONICAL_ROSTER)})")
        
    ids = set()
    names = set()
    placeholders = [r"\[Placeholder.*?\]", r"\[insert.*?\]", r"\[TODO.*?\]", r"\[TBD.*?\]", r"test_agent", r"mock_agent"]
    
    for agent in CANONICAL_ROSTER:
        # Check empty fields
        if not agent.agent_id:
            errors.append("Found agent with empty agent_id")
        if not agent.name:
            errors.append(f"Found agent {agent.agent_id} with empty name")
        if not agent.role:
            errors.append(f"Found agent {agent.agent_id} with empty role")
            
        # Check duplicates
        if agent.agent_id in ids:
            errors.append(f"Duplicate agent_id found: {agent.agent_id}")
        ids.add(agent.agent_id)
        
        if agent.name in names:
            errors.append(f"Duplicate agent display name found: {agent.name}")
        names.add(agent.name)
        
        # Check source files presence
        if not agent.source_files:
            errors.append(f"Agent {agent.agent_id} has no source file references")
            
        # Check placeholder regexes
        for pattern in placeholders:
            if re.search(pattern, agent.agent_id, re.IGNORECASE) or \
               re.search(pattern, agent.name, re.IGNORECASE) or \
               re.search(pattern, agent.role, re.IGNORECASE):
                errors.append(f"Agent {agent.agent_id} contains placeholder characters or values")
                
    is_valid = len(errors) == 0
    return {
        "status": "PASS" if is_valid else "FAIL",
        "errors": errors,
        "agent_count": len(CANONICAL_ROSTER)
    }

def reconcile_agents_against_manifest(manifest: dict, mode: str = STRICT_CANONICAL_36_MODE) -> Dict[str, Any]:
    """
    Reconciles manifest lists against the canonical registry.
    Detects required agents missing, invoked agents missing, legacy agents,
    duplicate allocations, and failed agents logic conflicts.
    """
    failures = []
    warnings = []
    
    required_agents = manifest.get("required_agents", [])
    invoked_agents = manifest.get("invoked_agents", [])
    failed_agents = manifest.get("failed_agents", [])
    
    registry_ids = {a.agent_id for a in CANONICAL_ROSTER}
    
    # 1. Duplicates check
    if len(required_agents) != len(set(required_agents)):
        failures.append("Duplicate entries found in 'required_agents'")
    if len(invoked_agents) != len(set(invoked_agents)):
        failures.append("Duplicate entries found in 'invoked_agents'")
    if len(failed_agents) != len(set(failed_agents)):
        failures.append("Duplicate entries found in 'failed_agents'")

    # 2. Logic: failed agents must be a subset of invoked agents
    for fa in failed_agents:
        if fa not in invoked_agents:
            failures.append(f"Logic Error: Failed agent '{fa}' was not even invoked")

    # 3. Check compatibility and registry mismatches
    for req in required_agents:
        if req not in registry_ids:
            msg = f"Required agent '{req}' is not in the canonical registry"
            if mode == STRICT_CANONICAL_36_MODE:
                failures.append(msg)
            else:
                warnings.append(msg)
                
    for inv in invoked_agents:
        if inv not in registry_ids:
            msg = f"Invoked agent '{inv}' is not in the canonical registry"
            if mode == STRICT_CANONICAL_36_MODE:
                failures.append(msg)
            else:
                warnings.append(msg)
                
    is_reconciled = len(failures) == 0
    
    return {
        "status": "PASS" if is_reconciled else "FAIL",
        "failures": failures,
        "warnings": warnings,
        "canonical_active_count": len(registry_ids),
        "manifest_required_count": len(required_agents),
        "manifest_invoked_count": len(invoked_agents)
    }
