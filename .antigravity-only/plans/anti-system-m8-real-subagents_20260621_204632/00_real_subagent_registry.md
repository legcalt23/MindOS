# Real Subagent Registry M8 Audit

This document details the registry implementation for the canonical set of subagents.

## 1. Registry Invariants
This document details the registry implementation for the canonical set of subagents. The registry is defined in `anti_agent_registry.py`.

* **Total Canonical Count:** Exactly 36 subagents.
* **Registry Provenance:** Verified against repository surgery log `03_agent_specialization_surgery.md` and the operational plan `11_final_upgraded_founder_council_plan.md`.
* **Registry Hashes:**
  * `03_agent_specialization_surgery.md`: `FA595289AC43498FFCEC899FBB82F69D6B4CF8C3A000467AFAE8CEC1D35A17C8`
  * `11_final_upgraded_founder_council_plan.md`: `1BF8EFFD0F3CC50AC81F38F2E7D5EAED959CED4867BD9839E4A736517FEA3D66`

## 2. Canonical Set Verification
The validation checks run against the registry confirm the following:
* **Duplicates:** None. Every subagent has a unique `agent_id` and `name`.
* **Placeholders:** None. No placeholder fields, missing entries, or mockup tags.
* **Provenance:** Every subagent links directly to the source files listed above.
* **Count Verification:** Confirmed 36 active registrations.

## 3. Ambiguity Check
The roles and specialties were imported verbatim from repository logs. 
* Ambiguity on standard vs critical classifications was resolved by strictly prioritizing the classifications documented in `03_agent_specialization_surgery.md`.
* All 36 identities are preserved without normalization, merging, or splits.
