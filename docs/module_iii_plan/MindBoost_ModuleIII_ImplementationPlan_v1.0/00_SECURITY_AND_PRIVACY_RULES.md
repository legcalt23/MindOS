# Security and Privacy Rules

1. **No real user data.** All simulation uses synthetic pseudonymized IDs.
2. **No production connections.** Feature flags prevent any live backend calls.
3. **Audit log isolation.** Simulation audit logs stored in a separate synthetic-only namespace.
4. **Anti-cheat secrecy.** No user-facing copy may reveal specific detection methods (AGRS thresholds, canary triggers, entropy algorithms, collusion graph structure).
5. **Session Validity copy.** Must use neutral language: "revisión de calidad competitiva", never "sistema de seguridad" or accusatory language.
6. **Data retention.** Synthetic simulation data has no retention policy (it's not real). Real data policies apply only after live Shadow Ranked (out of scope).
7. **DPIA.** Not required for synthetic-only simulation. Required before any live deployment (out of scope).
