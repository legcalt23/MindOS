# Founder Council Usage Guide

This guide explains how to request a review and interpret the output of the Founder Council Extended.

---

## 1. How to Request a Review

Copy the template below, populate it with your feature spec or implementation plan, and paste it into the prompt:

```text
Review Mode: [FAST_FEATURE_REVIEW | SCIENCE_REVIEW | PSYCHOMETRICS_SCORING_REVIEW | TECHNICAL_REVIEW | ANDROID_TIMING_REVIEW | LEGAL_CLAIMS_REVIEW | PRIVACY_TRUST_REVIEW | BUSINESS_REVIEW | UX_RETENTION_REVIEW | FULL_FEATURE_REVIEW | WAR_ROOM_REVIEW]
Feature / Plan: [Describe feature/plan or paste design]
Stage: [concept | specification | alpha | beta | production]
Decision Needed: [e.g. approve baseline metrics, check timing latency]
Target User: [e.g. developers, gamers]
Affected Modules: [e.g. shared/src, local database]
Claims Involved: [Yes/No]
Data Involved: [Yes/No]
Scoring Involved: [Yes/No]
Timing/Latency Involved: [Yes/No]
Evidence Available: [e.g. Choreographer output logs, research papers]
Known Risks: [e.g. baseline skew, frame rate drops]
Founder Question: [What is your specific question for the Council?]
```

---

## 2. Review Modes Reference

- **FAST_FEATURE_REVIEW:** Minor UI flows, copy tweaks, simple buttons. (6 agents)
- **SCIENCE_REVIEW:** Cognitive task designs, stimulus timings, distractor loops. (9 agents)
- **PSYCHOMETRICS_SCORING_REVIEW:** Baseline calculations, SRI math, outlier rules. (7 agents)
- **TECHNICAL_REVIEW:** Room databases, SQL queries, KMP module boundaries. (9 agents)
- **ANDROID_TIMING_REVIEW:** Input touch latency, Choreographer frame synchronization checks. (7 agents)
- **LEGAL_CLAIMS_REVIEW:** App store text, health marketing copy, compliance check. (7 agents)
- **PRIVACY_TRUST_REVIEW:** Permission requests, sensitive logging boundaries. (7 agents)
- **BUSINESS_REVIEW:** Subscription models, paywall popups timing. (8 agents)
- **UX_RETENTION_REVIEW:** Streaks, daily challenges, user friction loops. (8 agents)
- **FULL_FEATURE_REVIEW:** Core E1/E6 exercise releases. (18 agents)
- **WAR_ROOM_REVIEW:** Strategic pivots, roadmap changes. (36 agents, requires explicit request)
