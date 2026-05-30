# TESTING_STRATEGY.md

**Date:** 2026-05-27

---

## 1. Current test inventory

### commonTest (20 files, run on all platforms)

| Test | What it validates |
|---|---|
| SafeInputTest | Input sanitization and validation |
| AuthDiagnosticsTest | Auth error classification |
| DMMapperTest | DM data mapping |
| DMConversationIdsTest | DM ID generation |
| SendDMMessageUseCaseTest | DM message sending logic |
| DMListViewModelTest | DM list state management |
| DMMediaAdapterTest | DM media type handling |
| ElementMasterCatalogTest | Element catalog logic |
| ElementSelectionStateHolderTest | Element selection state |
| ProfileVisualConfigTest | Profile visual configuration |
| SocialGraphRulesTest | Social graph domain rules |
| GlobalFeedInputValidationTest | Feed input validation |
| PublishingPipelineTest | Publishing pipeline logic |
| PublisherDraftSecurityTest | Draft security checks |
| MediaContentResolverTest | Media content resolution |
| WalletModelsTest | Wallet domain models |
| WalletStateHolderTest | Wallet presentation state |
| WaveRequestServiceTest | Wave request business logic |
| ProfileInlineEditUtilsTest | Profile editing utilities |
| ProfileStateHolderSaveTest | Profile save flow |
| ErrorMapperTest | Error classification (NEW) |
| UiStateTest | UI state model (NEW) |

### desktopTest (5 files, Compose UI tests)

| Test | What it validates |
|---|---|
| MindOSAppNavigationSmokeTest | App navigation smoke test |
| AuthScreenSmokeTest | Auth screen renders |
| ElementSelectionScreenSmokeTest | Element selection renders |
| InMemoryDMRepository (fake) | Test double |
| NoOpRepositories (fakes) | Test doubles |

---

## 2. Test execution

| Command | Target | Platform |
|---|---|---|
| `./gradlew :shared:desktopTest` | commonTest + desktopTest | JVM/Desktop |
| `./gradlew :shared:allTests` | All source sets | All targets |
| `./gradlew test` | All module tests | JVM |

### Latest run result

```
BUILD SUCCESSFUL
22 test files, all passing
```

---

## 3. Test coverage assessment

| Layer | Coverage | Gap |
|---|---|---|
| Domain value objects | HIGH | -- |
| Domain policies | HIGH | -- |
| Domain use cases | MEDIUM | Some use cases untested |
| Data mappers | MEDIUM | DMMapper tested, others not |
| Data repositories | LOW | No repository tests with fakes (beyond DM) |
| Presentation state holders | MEDIUM | Wallet, DM, profile, element tested |
| UI components | LOW | Only smoke tests (3 screens) |
| Navigation | LOW | One smoke test |
| Error mapping | HIGH (NEW) | ErrorMapper fully tested |
| UiState | HIGH (NEW) | UiState fully tested |

---

## 4. Recommended test additions

| Priority | Test | Type |
|---|---|---|
| P1 | ErrorMapper for DomainError -> UserFacingError | Unit |
| P1 | SessionRepository with fake Firebase | Unit |
| P2 | ProfileStateHolder full lifecycle | Unit |
| P2 | GlobalFeedStateHolder | Unit |
| P2 | DMStateHolder | Unit |
| P3 | Navigation graph completeness test | UI (Desktop) |
| P3 | Visual regression baseline (Paparazzi or Desktop screenshots) | Visual |
| P4 | Accessibility semantics test | UI (Desktop) |

---

## 5. Test infrastructure

| Tool | Status |
|---|---|
| kotlin-test | Configured |
| kotlinx-coroutines-test | Configured |
| Compose UI Test (Desktop) | Configured |
| Android instrumented tests | NOT configured |
| iOS tests | NOT configured (no iosApp) |
| Visual regression | NOT configured |
| Code coverage | NOT configured |
