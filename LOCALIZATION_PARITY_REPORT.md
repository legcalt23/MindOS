# LOCALIZATION_PARITY_REPORT.md

**Date:** 2026-05-27

---

## 1. Current state

### Language: Spanish (es) only

The app UI is entirely in Spanish. All strings are hardcoded in Kotlin source files.

### String centralization

| Area | Status |
|---|---|
| Auth strings | Hardcoded in AuthScreen.kt, AuthDiagnostics.kt |
| Profile strings | Hardcoded in ProfileScreen.kt, ProfileEntryScreen.kt |
| DM strings | Hardcoded in DMChatScreen.kt, DMListScreen.kt |
| Search strings | Hardcoded in SearchVisualTokens.kt, UsernameSearchScreen.kt |
| Feed strings | Hardcoded in GlobalFeedScreen.kt, GlobalFeedComposerScreen.kt |
| Error messages | Hardcoded in AuthDiagnostics.kt, various catch blocks |
| Settings strings | Hardcoded in SettingsScreen.kt |

### New centralization layer

`design/AppStrings.kt` created with 50+ constants covering common actions, auth, states, profile, DM, settings, feed, and accessibility labels.

---

## 2. Parity assessment

**Localization parity is 100%** because all strings come from the same Kotlin source in commonMain. There is no divergence between platforms.

**However:** Strings are not externalized for i18n. Future multi-language support would require migration to compose-resources or similar.

---

## 3. Format parity

| Format | Implementation | Shared? |
|---|---|---|
| Date/time | kotlinx-datetime + expect/actual GlobalFeedTime | YES |
| Numbers | Default Kotlin formatting | YES |
| Currency | Not used (wallet uses integer amounts) | N/A |
| Plurals | Not needed (Spanish hardcoded) | N/A |

---

## 4. Recommendations

| Priority | Action |
|---|---|
| P1 (done) | AppStrings.kt centralization layer |
| P2 | Migrate screen strings to AppStrings references |
| P3 | Evaluate compose-resources for proper i18n |
| P4 | Add RTL support if needed |
