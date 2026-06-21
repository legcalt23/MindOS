# 14 Phase 07.5 Data Sanitization Gate

**Mandatory Blocker:** No data may pass from the core Arena Engine to Simulation, Dashboard, or Analytics without clearing this gate.

**Rules:**
- **Prohibited Raw Fields:** True UIDs, Email, Device IDs, IP Addresses, PII.
- **Allowed Synthetic Fields:** Hashed or randomized Simulation IDs, Aggregate Cohort IDs.
- **UID Removal:** All test harnesses must map true UIDs to one-way synthetic hashes before memory storage.
- **Crash Log Redaction:** Any exception within the Bayesian Core must have its message scrubbed before bubbling up to UI or Crashlytics.
- **No Cognitive Profiling Leakage:** Do not log terms like "Low Performance" or "High Intelligence" anywhere.

**Acceptance Criteria:**
1. Running a simulation outputs JSON/CSV files with exactly 0 occurrences of a true UID format.
2. Exceptions thrown in the engine produce stack traces but scrubbed custom messages.

**Claude Code Implementation Prompt:**
"Implement the DataSanitizer component. It must intercept all Arena outputs and replace UIDs with UUIDv4 synthetic IDs. Write tests that prove an exception thrown with a UID in its message has the UID scrubbed before reaching the caller."
