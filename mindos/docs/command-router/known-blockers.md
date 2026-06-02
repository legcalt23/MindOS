# Command Router Known Blockers

Status after applying `mindos-command-router-codex-implementation-plan.zip`.

| ID | Blocker | Status | Evidence |
| --- | --- | --- | --- |
| B1 | Secure mode and mandatory safe wiring | Closed | `CommandRouterFactory.forProduction`, `ActionExecutorFactory.forProduction`, tests in `mind-kmp-core` |
| B2 | Unified security gate for text, deep-links, DTO/API | Closed | `CommandInputSecurityGate`, `CommandRouterImpl`, `DeepLinkBridge`, security/decompose tests |
| B3 | Thread-safe caches/abuse guard | Closed | atomic `InMemoryAbuseGuard`, TTL/max `CachingReferenceResolver`, concurrency/TTL tests |
| B4 | Replay/idempotency by requestId | Closed | `IdempotencyStore`, `DefaultActionExecutor` check before navigation, replay/conflict tests |
| B5 | Bounded navigation stack and explicit policy | Closed | `NavigationPolicy`, `BoundedNavigationCoordinator`, Decompose wiring, navigation tests |
| B6 | Real resolver/clocks/sinks cableable for production | Closed | production runtime factories reject missing/no-op critical dependencies |
| B7 | Dates and chained story-of-user resolution | Closed | `RelativeDateParser`, `MindAction.OpenStoryOfUser`, executor username -> userId -> storyId tests |

## Remaining Non-Blocker Work

- Replace process-local in-memory idempotency/cache/abuse implementations with distributed stores in
  multi-instance production.
- Add a real line/branch coverage tool such as Kover when CI policy is finalized. Current
  `verifyCommandRouterHardening` ensures the hardening regression suites ran and produced reports.
- Expand the declarative action registry as new actions are added; do not reintroduce ad-hoc mappers.
