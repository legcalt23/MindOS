# MindOS KMP Contracts (ZIP08)

ZIP08 defines the public Kotlin Multiplatform contracts that can be shared by Android, iOS,
desktop, tests, and backend adapters without depending on internal router modules.

The implementation lives in `mind-kmp-contracts` under package `com.mindos.contracts`.

## Design Rules

- Contracts are KMP-pure Kotlin.
- DTOs use `kotlinx.serialization`.
- Top-level wire DTOs carry `schemaVersion`.
- Result and failure types are sealed and typed, not stringly-typed.
- JSON uses explicit serializers and `ignoreUnknownKeys = true` for backward compatibility.
- The module has no project dependencies. Domain models map into/out of these contracts at app
  boundaries.

## Contracts

| ZIP file | Implemented contract |
|----------|----------------------|
| `ACTIONS/ACTION_CONTRACTS.kt` | `RouterActionDto` sealed hierarchy: profile, story, chat, messages, settings. |
| `RESULTS/ROUTER_RESULTS.kt` | `RouterResultDto`, `RouterSuccessDto`, `RouterFailureResultDto`, typed `RouterFailureDto`. |
| `ENTITIES/ENTITY_MODELS.kt` | `EntityDto`, `UserEntityDto`, `StoryEntityDto`, `ChatEntityDto`. |
| `VALIDATION/VALIDATION_CONTRACTS.kt` | `ActionContractValidator`, `ValidationReportDto`, `ValidationIssueDto`. |
| `API/ROUTER_API.md` | `KmpCommandRouter.route(RouterRequestDto): RouterResultDto`. |
| `SERIALIZATION/SERIALIZATION_POLICY.md` | `ContractJson` with explicit serializers and stable class discriminator. |

## Serialization Policy

Use:

```kotlin
ContractJson.encodeAction(action)
ContractJson.decodeAction(payload)
ContractJson.encodeResult(result)
ContractJson.decodeResult(payload)
```

Do not expose raw internal domain objects as wire contracts. Keep compatibility changes additive:
new fields need defaults, renamed fields need a migration path, and removed fields require an RFC.
