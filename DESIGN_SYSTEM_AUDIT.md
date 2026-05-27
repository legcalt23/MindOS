# DESIGN_SYSTEM_AUDIT.md

**Date:** 2026-05-27

---

## 1. Estado actual

### Theme: MindOSTheme.kt

- **Color scheme:** `darkColorScheme` only (no light mode)
- **Typography:** 6 text styles defined (headlineLarge, headlineMedium, titleMedium, bodyLarge, bodyMedium, labelLarge)
- **Shapes:** Material3 defaults (no custom shapes)
- **Location:** `shared/src/commonMain/kotlin/com/example/mindos/design/MindOSTheme.kt`

### Existing tokenization

| Area | Status | Location |
|---|---|---|
| Colors (Material3) | Defined in MindOSTheme | 10 color slots |
| Typography | 6 styles defined | MindOSTheme |
| Spacing | NOT tokenized | 624+ scattered dp literals |
| Shapes/Radius | NOT tokenized | Scattered RoundedCornerShape literals |
| Elevation | NOT tokenized | Scattered dp elevation values |
| Animation | NOT tokenized | Scattered duration literals |
| Strings | NOT centralized | Hardcoded in Compose functions |
| Icons | NOT tokenized | Material Icons Extended used directly |

### Hardcoded values audit

| Type | Occurrences | Files affected |
|---|---|---|
| `Color(0xFF...)` | 275 | 30 files |
| `N.dp` literals | 624+ | 30+ files |
| Hardcoded strings | ~200+ | Most screen files |
| `RoundedCornerShape(N.dp)` | ~80+ | 20+ files |

### Feature-local token patterns found

- `SearchVisualTokens.kt` — NeuBlack, NeuWhite, NeuGreen, NeuMist, TargetGold
- `ViewerDesign.kt` — Black, White, Green, Mist, Accent + Spacing + Typography objects

These prove the team recognizes the need for tokens but implemented them per-feature instead of globally.

---

## 2. New design system created (this session)

### Files created

| File | Purpose |
|---|---|
| `design/AppColors.kt` | Brand + neutral + semantic + state color tokens |
| `design/AppSpacing.kt` | Spacing scale (4dp grid) + screen/component sizes |
| `design/AppShapes.kt` | Radius scale + pre-built shapes |
| `design/AppElevation.kt` | Elevation scale |
| `design/AppAnimation.kt` | Duration + delay tokens |
| `design/AppStrings.kt` | Centralized UI string constants |
| `design/components/StateComponents.kt` | Loading, Empty, Error, Offline composables |

### Token inventory

| Category | Count | Examples |
|---|---|---|
| Brand colors | 4 | NeuGreen, NeuViolet, NeuGold, TargetGold |
| Neutral colors | 6 | DeepBlack, Background, Surface, SurfaceVariant, OnBackground, Mist |
| Semantic colors | 4 | Error, Warning, Success, Info |
| State colors | 2 | Disabled, DisabledBackground |
| Alpha values | 3 | PressedAlpha, FocusedAlpha, ScrimAlpha |
| Spacing | 9 base + 8 semantic | XXS(2) through XXXXL(40) + screen/component sizes |
| Radius | 7 + 10 shapes | RadiusXS(4) through RadiusFull(999) |
| Elevation | 5 | None(0) through Highest(16) |
| Animation | 5 | DurationFast(150) through StaggerDelay(50) |
| Strings | 50+ | Actions, auth, states, profile, DM, settings, a11y |

---

## 3. Component inventory

### Components that exist (in various screens, not centralized)

| Component | Location | Centralized? |
|---|---|---|
| Loading spinner | Various screens | NO (now: StateComponents) |
| Error display | Various screens | NO (now: StateComponents) |
| Empty state | Various screens | NO (now: StateComponents) |
| Text fields | AuthScreen, ProfileScreen | NO (Material3 OutlinedTextField) |
| Buttons | Various | NO (Material3 Button/OutlinedButton) |
| Icon buttons | ProfileScreen, various | NO (Material3 IconButton) |
| Cards | HomeScreen, various | NO (custom Box with styling) |
| Dialogs | ProfileScreen | NO (Material3 Dialog) |
| Avatars | ProfileScreen, DMList | NO (custom Clip/Image) |
| Top bar | Custom per screen | NO |

### Components needed (recommended future)

| Component | Priority | Notes |
|---|---|---|
| AppTopBar | P2 | Standardize back button, title, actions |
| AppButton (primary/secondary/destructive) | P2 | Wrap Material3 with token presets |
| AppTextField | P2 | Wrap OutlinedTextField with theme |
| AppCard | P3 | Standardize card styling |
| AppAvatar | P3 | Standardize avatar rendering |
| AppDialog | P3 | Standardize dialog styling |
| AppBottomSheet | P3 | If used in app |
| AppDivider | P4 | Standardize with tokens |
| AppChip | P4 | If used in app |

---

## 4. Recommendations

### Immediate (done)

1. Token files created
2. State components created
3. Tests for UiState and ErrorMapper

### Short term

1. Replace Color() literals with AppColors references (275 occurrences)
2. Replace dp literals with AppSpacing references (critical paths)
3. Replace hardcoded strings with AppStrings references (screen titles, buttons)
4. Existing feature-local tokens (SearchVisualTokens, ViewerDesign) should reference AppColors

### Medium term

1. Create AppTopBar, AppButton, AppTextField components
2. Create AppAvatar component
3. Add light theme variant to MindOSTheme
4. Add missing typography styles (bodySmall, labelMedium, labelSmall)
