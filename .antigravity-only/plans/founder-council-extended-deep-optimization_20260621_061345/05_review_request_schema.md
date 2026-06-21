# Round 5: Review Request Schema

This document defines the strict input metadata schema that the founder must provide to initiate a structured review.

## Request Input Schema

Every request must be classified using the following structure:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "FounderReviewRequest",
  "type": "object",
  "properties": {
    "review_mode": {
      "type": "string",
      "enum": [
        "FAST_FEATURE_REVIEW",
        "SCIENCE_REVIEW",
        "PSYCHOMETRICS_SCORING_REVIEW",
        "TECHNICAL_REVIEW",
        "ANDROID_TIMING_REVIEW",
        "LEGAL_CLAIMS_REVIEW",
        "PRIVACY_TRUST_REVIEW",
        "BUSINESS_REVIEW",
        "UX_RETENTION_REVIEW",
        "FULL_FEATURE_REVIEW",
        "WAR_ROOM_REVIEW"
      ]
    },
    "feature_plan": {
      "type": "string"
    },
    "stage": {
      "type": "string",
      "enum": ["concept", "specification", "alpha", "beta", "production"]
    },
    "decision_needed": {
      "type": "string"
    },
    "target_user": {
      "type": "string"
    },
    "affected_modules": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "claims_involved": {
      "type": "boolean"
    },
    "data_involved": {
      "type": "boolean"
    },
    "scoring_involved": {
      "type": "boolean"
    },
    "timing_latency_involved": {
      "type": "boolean"
    },
    "evidence_available": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "known_risks": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "founder_question": {
      "type": "string"
    }
  },
  "required": [
    "review_mode",
    "feature_plan",
    "stage",
    "decision_needed",
    "target_user",
    "claims_involved",
    "data_involved",
    "scoring_involved",
    "timing_latency_involved",
    "evidence_available"
  ]
}
```

---

## Founder-Friendly Copy-Paste Template

To initiate a review, copy the template below and paste it into the prompt:

```text
Review Mode: [FAST_FEATURE_REVIEW | SCIENCE_REVIEW | PSYCHOMETRICS_SCORING_REVIEW | TECHNICAL_REVIEW | ANDROID_TIMING_REVIEW | LEGAL_CLAIMS_REVIEW | PRIVACY_TRUST_REVIEW | BUSINESS_REVIEW | UX_RETENTION_REVIEW | FULL_FEATURE_REVIEW | WAR_ROOM_REVIEW]
Feature / Plan: [Provide feature plan details or paste implementation plan]
Stage: [concept | specification | alpha | beta | production]
Decision Needed: [e.g. approve baseline logic, approve pricing tiers]
Target User: [e.g. traders, programmers, executives]
Affected Modules: [e.g. shared/src/androidMain, firestore.rules]
Claims Involved: [Yes/No]
Data Involved: [Yes/No]
Scoring Involved: [Yes/No]
Timing/Latency Involved: [Yes/No]
Evidence Available: [e.g. Choreographer logs, N-Back literature citations]
Known Risks: [e.g. potential learning effects, touch event delay]
Founder Question: [What is your specific question for the Council?]
```
