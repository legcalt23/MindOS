# Evidence Ledger and Audit Spec

This document defines the strict quality standards, validation protocols, and auditability requirements for claims recorded in the `/anti` subagent evidence ledger.

---

## 1. What Counts as Evidence
To be classified as `VERIFIED`, a claim must trace back to one of the following concrete sources:
1.  **Code/Configuration Files:** Existing lines of code, shell scripts, or structured YAML/JSON configurations inside the current repository scope.
2.  **API Execution Logs:** Raw JSON responses, HTTP return codes, or console outputs retrieved directly from verified execution runs.
3.  **Active Session Transcripts:** Actual text outputs and message events stored within the active conversation logs (`.system_generated/logs/transcript.jsonl`) with matching step indices.
4.  **Verified Strategic Artifacts:** Explicitly signed plans or historical packages with matching SHA-256 integrity hashes.

---

## 2. What Does Not Count as Evidence
The following sources are classified as `UNVERIFIED` or `NOT_APPLICABLE` and cannot be used to verify strategic claims:
1.  **Model Assumptions:** Assertions made by an LLM without direct file system checks or console command feedback.
2.  **Mock or Simulated Responses:** Output blocks generated under `FALLBACK_PROMPT_SIMULATION` without a valid conversation thread.
3.  **Unimplemented Plans:** Draft roadmap items or hypothetical configurations described in text but not yet deployed.
4.  **Out-of-Scope Files:** Configuration parameters or code files belonging to out-of-scope modules (e.g. Kotlin code inside product modules).

---

## 3. Citation Standards

All claims in the ledger must follow these citation formats:

### Transcript Citations
When citing a transcript, the citation must include:
- The absolute URI to the transcript file.
- The specific Conversation ID.
- The `step_index` and the sender/recipient type.
- *Format:* `[Transcript: UUID#Step-12](file:///C:/path/to/transcript.jsonl#L123-145)`

### File Citations
When citing a file in the workspace, the citation must include:
- The absolute file path.
- The exact line range.
- The specific text matches.
- *Format:* `[REALITY_STATUS.md:L7-9](file:///c:/Users/curro/AndroidStudioProjects/MindOS/.antigravity-only/agents/REALITY_STATUS.md#L7-L9)`

---

## 4. Gaps and Unverified Claims
If a claim lacks direct, verifiable evidence:
1.  **Flag Status:** The verification status must be set to `UNVERIFIED` or `INCONCLUSIVE`.
2.  **Confidence Adjustment:** The agent confidence score for that claim must be capped at a maximum of `0.50`.
3.  **Audit Mitigation:** A required fix and owner must be assigned in the findings block to establish the missing evidence.

## 5. Prevention of Fake Auditability
To prevent models from fabricating evidence or claiming validation without checking:
- **No Self-Audit Approval:** No agent may mark its own execution log or prompt as `VERIFIED` without a cross-check from the `qa_evidence_agent`.
- **Integrity Cross-Check:** The verification script checks if the files cited in `evidence_ledger.md` exist and contain the target text at the specified line numbers.
- **Strict Veto Gates:** If the verification script detects a fake citation, it sets the status to `INCONCLUSIVE_BLOCKED` and halts the release process.
- **Mapping Claims:** Every blocker, high risk, and verdict status described in the Executive Verdict must map directly to at least one entry in `evidence_ledger.md` containing a valid claim ID.
