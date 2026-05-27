# PRODUCTION_PARITY_CHECKLIST.md

**Date:** 2026-05-27

---

## Production readiness checklist

### Build

- [x] Android debug builds
- [x] Android release configured (R8 enabled)
- [x] Android release signing configured (env-var based, falls back to debug)
- [ ] iOS simulator builds (requires macOS)
- [ ] iOS device builds (requires macOS + signing)
- [x] Desktop builds
- [x] applicationId changed to com.mindos.app

### Tests

- [x] Shared unit tests pass (22 test files, all green)
- [x] Desktop UI smoke tests pass
- [ ] Android instrumented tests
- [ ] iOS tests
- [ ] Visual regression baseline

### Architecture

- [x] KMP architecture audited
- [x] >90% code in commonMain
- [x] expect/actual pairs complete (18 declarations, all platforms)
- [x] Domain logic shared (no duplication)
- [x] Navigation shared (AppRoute sealed interface)

### Design system

- [x] Color tokens centralized (AppColors.kt)
- [x] Spacing tokens centralized (AppSpacing.kt)
- [x] Shape tokens centralized (AppShapes.kt)
- [x] Elevation tokens centralized (AppElevation.kt)
- [x] Animation tokens centralized (AppAnimation.kt)
- [x] String constants centralized (AppStrings.kt)
- [~] Screens migrated to use tokens (P1 critical screens done: ProfileScreen, HomeScreen, DMChatScreen — ~215 literals remain)
- [ ] Screens migrated to use AppStrings

### State management

- [x] UiState model created
- [x] ErrorMapper created
- [x] State components created (Loading, Error, Empty, Offline)
- [ ] Screens adopt UiState model
- [ ] Screens adopt ErrorScreen component

### Navigation

- [x] Route graph audited
- [x] Back behavior audited
- [x] No orphan screens
- [ ] Deep links (not needed yet)

### Error handling

- [x] DomainError hierarchy (social domain)
- [x] AuthDiagnostics (auth errors)
- [x] ErrorMapper (general errors)
- [ ] All screens use standardized error display

### Localization

- [x] Single language (Spanish) consistent
- [x] AppStrings centralization layer
- [ ] Screens migrated to AppStrings
- [ ] i18n framework (compose-resources)

### Accessibility

- [x] Audited
- [x] A11y string constants created
- [ ] All icon buttons have contentDescription
- [ ] All Canvas elements have semantics
- [ ] Touch target audit (48dp minimum)

### Performance

- [x] Audited
- [x] Lazy layouts used for lists
- [ ] Image caching strategy (shared)
- [ ] Pagination for large collections
- [ ] Recomposition optimization

### Resilience

- [x] Audited
- [x] ErrorMapper for exception classification
- [ ] Retry mechanism
- [ ] Offline detection
- [ ] Loading timeout

### CI/CD

- [x] Existing CI for Cloud Functions
- [x] CI for shared Kotlin tests (kotlin-ci.yml)
- [x] CI for Android build (kotlin-ci.yml)
- [x] CI for detekt (kotlin-ci.yml)
- [ ] CI for iOS framework (macOS runner)

### Observability

- [x] Debug logging (SafeDebugLogging)
- [ ] Crash reporting (Crashlytics)
- [ ] Analytics
- [ ] Performance monitoring

### Security

- [x] Input sanitization (SafeInput)
- [x] Firebase App Check (Android)
- [x] Debug logging disabled in release
- [x] Comprehensive security documentation
- [ ] PII removed from logs
- [ ] google-services.json in CI only

### Documentation

- [x] PARITY_AUDIT_INITIAL.md
- [x] PARITY_TARGET_SPEC.md
- [x] KMP_ARCHITECTURE_REVIEW.md
- [x] DESIGN_SYSTEM_AUDIT.md
- [x] DESIGN_SYSTEM_SPEC.md
- [x] VISUAL_PARITY_REPORT.md
- [x] FUNCTIONAL_PARITY_REPORT.md
- [x] NAVIGATION_PARITY_REPORT.md
- [x] BUILD_CONFIGURATION_AUDIT.md
- [x] LOCALIZATION_PARITY_REPORT.md
- [x] ACCESSIBILITY_AUDIT.md
- [x] PERFORMANCE_AUDIT.md
- [x] RESILIENCE_AUDIT.md
- [x] TESTING_STRATEGY.md
- [x] PARITY_TEST_PLAN.md
- [x] PARITY_TEST_RESULTS.md
- [x] RELEASE_READINESS_AUDIT.md
- [x] CI_CD_RECOMMENDATIONS.md
- [x] OBSERVABILITY_AUDIT.md
- [x] CLIENT_SECURITY_AUDIT.md
- [x] PRODUCTION_PARITY_CHECKLIST.md
- [x] MANUAL_QA_PARITY_SCRIPT.md
- [x] FINAL_PARITY_SELF_AUDIT.md

### Risks documented

- [x] No iosApp module
- [x] No KMP CI
- [x] Hardcoded values (275 colors, 624+ dp)
- [x] No crash reporting
- [x] No analytics
- [x] Camera Android-only
