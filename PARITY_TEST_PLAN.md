# PARITY_TEST_PLAN.md

**Date:** 2026-05-27

---

## Test matrix for iOS/Android parity

### Unit tests (commonTest — platform-agnostic)

| Test | Validates | Parity dimension |
|---|---|---|
| SafeInputTest | Input validation identical | Validation parity |
| AuthDiagnosticsTest | Auth error messages identical | Error parity |
| ErrorMapperTest | Error classification identical | Error parity |
| UiStateTest | State model behavior identical | State parity |
| SocialGraphRulesTest | Social rules identical | Functional parity |
| DMMapperTest | Data mapping identical | Functional parity |
| SendDMMessageUseCaseTest | DM logic identical | Functional parity |
| WalletModelsTest | Wallet logic identical | Functional parity |
| WaveRequestServiceTest | Wave logic identical | Functional parity |
| ProfileInlineEditUtilsTest | Edit logic identical | Functional parity |
| ProfileStateHolderSaveTest | Save flow identical | Functional parity |
| GlobalFeedInputValidationTest | Feed validation identical | Validation parity |
| PublishingPipelineTest | Publishing logic identical | Functional parity |

### UI smoke tests (desktopTest — verifies render)

| Test | Validates | Parity dimension |
|---|---|---|
| MindOSAppNavigationSmokeTest | Navigation renders | Navigation parity |
| AuthScreenSmokeTest | Auth screen renders | Visual parity |
| ElementSelectionScreenSmokeTest | Selection renders | Visual parity |

### Manual parity tests

See `MANUAL_QA_PARITY_SCRIPT.md` for per-screen manual test script.

---

## Execution

```bash
# Run all shared tests (commonTest on desktop target)
./gradlew :shared:desktopTest

# Run all tests across all targets
./gradlew :shared:allTests

# Run specific test class
./gradlew :shared:desktopTest --tests "com.example.mindos.core.ui.ErrorMapperTest"
```
