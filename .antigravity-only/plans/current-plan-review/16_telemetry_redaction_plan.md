# 16 Telemetry Redaction / Crash Safety

**Goal:** Zero leakage of cognitive ratings or PII into Crashlytics/Sentry.

**Rules:**
- **Safe Logging:** "Engine calculated rating" is allowed. "User A rating dropped to 1200" is PROHIBITED.
- **Exception Redaction:** Throwables must be wrapped or intercepted. Use a `SafeArenaException` that overrides `getMessage()` to strip payload details.
- **Prohibited Payloads:** `{"uid": "user123", "rating": 1450, "task": "focus"}`
- **Allowed Aggregate Payloads:** `{"hardwareClass": "iOS-120Hz", "averageMatchDurationMs": 45000}`

**Acceptance Criteria:**
1. `ArenaLogger` class created that swallows raw state and only emits safe enum-based event types.
2. Unit tests verify that passing a `PlayerRatingState` to the logger results in an obfuscated log string.

**Claude Code Prompt:**
"Create `ObservabilitySink.recordNonFatal` wrapper that enforces `NonFatalTelemetryEvent` types. Ensure raw Throwables are stripped of messages and causes before external reporting."
