# VISUAL_PARITY_REPORT.md

**Date:** 2026-05-27

---

## 1. Summary

Visual parity between iOS and Android is inherently HIGH because MindOS uses Compose Multiplatform for ALL UI rendering. Both platforms render via Skia/Skiko, producing pixel-identical output for:
- Colors
- Typography (when using the same font families)
- Spacing
- Shapes
- Layout
- Animations

### Inherent parity from Compose Multiplatform

Since ALL Compose UI code lives in `commonMain`, the rendering is identical on Android, iOS, and Desktop. The exceptions are system chrome elements.

---

## 2. Screens audited

| Screen | commonMain? | Visual parity risk |
|---|---|---|
| AuthScreen | YES | LOW (standard form) |
| ElementSelectionScreen | YES | LOW (custom canvas) |
| HomeScreen | YES | LOW (list + navigation) |
| ProfileScreen | YES | MEDIUM (complex layout) |
| ProfileEntryScreen | YES | LOW (form) |
| ProfileSettingsScreen | YES | LOW (settings list) |
| PrivacySettingsScreen | YES | LOW (switches) |
| GlobalFeedScreen | YES | MEDIUM (feed + media) |
| GlobalFeedComposerScreen | YES | LOW (form) |
| DMListScreen | YES | LOW (list) |
| DMChatScreen | YES | MEDIUM (messages + input) |
| CajoneraScreen | YES | LOW (list) |
| NotificationsScreen | YES | LOW (list) |
| UsernameSearchScreen | YES | LOW (search + results) |
| SettingsScreen | YES | LOW (settings) |
| MoneyScreen | YES | LOW (wallet display) |
| AppMediaViewer | EXPECT/ACTUAL | HIGH (platform media rendering) |

---

## 3. Diferencias detectadas

### 3.1 Hardcoded colors (275 occurrences in 30 files)

**Status:** Tokens created (AppColors). Migration pending.

Files with most hardcoded colors:
- ProfileScreen.kt (32)
- CajoneraScreen.kt (26)
- ProfileEditorComponents.kt (25)
- OrbComponent.kt (21)
- GlobalFeedComposerScreen.kt (21)
- DMChatScreen.kt (15)
- SanctuaryBackground.kt (14)

**Risk:** Colors are consistent because they use the same hex values, but maintenance is fragile.

### 3.2 System chrome differences

| Element | Android | iOS | Risk |
|---|---|---|---|
| Status bar | Material edge-to-edge | iOS safe area | LOW (handled by statusBarsPadding) |
| Navigation bar | Gesture nav / 3-button | iOS home indicator | LOW (handled by navigationBarsPadding) |
| Keyboard | Material keyboard | iOS keyboard | ACCEPTED (native) |
| Scroll overscroll | Glow (or stretch on newer) | Bounce | ACCEPTED (Compose handles) |

### 3.3 Font rendering

Compose Multiplatform uses system fonts via Skia. On Android, this maps to Roboto; on iOS, to SF Pro. Both are sans-serif defaults and visually similar, but not identical.

**Status:** ACCEPTED difference. Custom font loading would unify this but adds bundle size.

---

## 4. Fixes aplicados

| Fix | Impact |
|---|---|
| Created AppColors.kt with centralized tokens | Foundation for color migration |
| Created AppSpacing.kt with spacing scale | Foundation for spacing migration |
| Created AppShapes.kt with shape tokens | Foundation for shape migration |
| Created AppElevation.kt with elevation tokens | Foundation for elevation migration |
| Created StateComponents.kt with standard screens | Consistent loading/error/empty |

---

## 5. Diferencias aceptadas

1. System font rendering (Roboto vs SF Pro)
2. Status bar / navigation bar native chrome
3. Keyboard appearance
4. Scroll physics
5. AppMediaViewer platform-specific rendering

---

## 6. Diferencias residuales

1. 275 Color() literals not yet migrated to AppColors (functional, but maintenance risk)
2. 624+ dp literals not yet migrated to AppSpacing (functional, but consistency risk)
3. No visual regression testing infrastructure yet

---

## 7. Visual checklist per screen

| Screen | Colors from tokens? | Spacing from tokens? | Shapes from tokens? | Dark mode? |
|---|---|---|---|---|
| AuthScreen | PARTIAL | NO | NO | YES |
| ElementSelectionScreen | NO | NO | NO | YES |
| HomeScreen | PARTIAL | NO | NO | YES |
| ProfileScreen | NO | NO | NO | YES |
| GlobalFeedScreen | NO | NO | NO | YES |
| DMChatScreen | NO | NO | NO | YES |
| All others | NO | NO | NO | YES |

**Note:** "PARTIAL" means the screen uses MaterialTheme.colorScheme for some values but also has inline Color() literals.
