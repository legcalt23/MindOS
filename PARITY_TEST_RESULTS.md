# PARITY_TEST_RESULTS.md

**Date:** 2026-05-27
**Executed on:** Windows (Desktop/JVM target)

---

## Test execution summary

### Command: `./gradlew :shared:desktopTest`

**Result: BUILD SUCCESSFUL**

### Test results

| Test class | Tests | Passed | Failed |
|---|---|---|---|
| SafeInputTest | ALL | ALL | 0 |
| AuthDiagnosticsTest | ALL | ALL | 0 |
| DMMapperTest | ALL | ALL | 0 |
| DMConversationIdsTest | ALL | ALL | 0 |
| SendDMMessageUseCaseTest | ALL | ALL | 0 |
| DMListViewModelTest | ALL | ALL | 0 |
| DMMediaAdapterTest | ALL | ALL | 0 |
| ElementMasterCatalogTest | ALL | ALL | 0 |
| ElementSelectionStateHolderTest | ALL | ALL | 0 |
| ProfileVisualConfigTest | ALL | ALL | 0 |
| SocialGraphRulesTest | ALL | ALL | 0 |
| GlobalFeedInputValidationTest | ALL | ALL | 0 |
| PublishingPipelineTest | ALL | ALL | 0 |
| PublisherDraftSecurityTest | ALL | ALL | 0 |
| MediaContentResolverTest | ALL | ALL | 0 |
| WalletModelsTest | ALL | ALL | 0 |
| WalletStateHolderTest | ALL | ALL | 0 |
| WaveRequestServiceTest | ALL | ALL | 0 |
| ProfileInlineEditUtilsTest | ALL | ALL | 0 |
| ProfileStateHolderSaveTest | ALL | ALL | 0 |
| **ErrorMapperTest (NEW)** | 10 | 10 | 0 |
| **UiStateTest (NEW)** | 10 | 10 | 0 |
| MindOSAppNavigationSmokeTest | ALL | ALL | 0 |
| AuthScreenSmokeTest | ALL | ALL | 0 |
| ElementSelectionScreenSmokeTest | ALL | ALL | 0 |

### Warnings

- WalletStateHolderTest: 24 ExperimentalCoroutinesApi opt-in warnings (non-blocking)
- Gradle deprecation warnings (compatible with Gradle 9.x, not 10)

---

## Limitations

| Limitation | Impact |
|---|---|
| Tests only run on Desktop/JVM target | Cannot verify iOS-specific behavior |
| No Android instrumented tests | Cannot verify Android-specific rendering |
| No visual regression tests | Cannot detect pixel-level differences |
| commonTest code runs on JVM, not on actual iOS/Android runtimes | Platform-specific bugs may be missed |

---

## Parity confidence

| Dimension | Confidence | Basis |
|---|---|---|
| Validation parity | HIGH | All validators tested in commonTest |
| Error parity | HIGH | Auth + ErrorMapper tested |
| State model parity | HIGH | UiState tested |
| Domain logic parity | HIGH | Use cases + policies tested |
| Navigation parity | MEDIUM | Smoke test only |
| Visual parity | LOW | No visual regression tests |
| Platform-specific parity | LOW | Cannot run iOS tests on Windows |
