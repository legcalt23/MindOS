# DESIGN_SYSTEM_SPEC.md

**Date:** 2026-05-27

---

## MindOS Design System Specification

### 1. Color System

**Source of truth:** `design/AppColors.kt` + `design/MindOSTheme.kt`

| Token | Hex | Usage |
|---|---|---|
| NeuGreen | #13ECA4 | Primary actions, success, brand accent |
| NeuViolet | #8F5CFF | Secondary actions, badges, highlights |
| NeuGold | #E7E0CF | Premium indicators, warm accent |
| TargetGold | #E9FF9B | Search highlights, radar targets |
| DeepBlack | #020504 | Deepest background, scrim base |
| Background | #050505 | App background |
| Surface | #0B0B0B | Cards, elevated containers |
| SurfaceVariant | #101815 | Subtle differentiation |
| OnBackground | #F4FFF9 | Primary text |
| Mist | #A7CFC1 | Secondary text |
| OnSurfaceVariant | #9BFFE0 | Accent text |
| Error | #FF4D6A | Error states, destructive |
| Warning | #FFB74D | Attention required |
| Info | #64B5F6 | Informational |

### 2. Typography System

**Source of truth:** `design/MindOSTheme.kt` (NeuTypography)

| Style | Weight | Size | Line Height |
|---|---|---|---|
| headlineLarge | Black (900) | 36sp | 42sp |
| headlineMedium | Black (900) | 28sp | 34sp |
| titleMedium | SemiBold (600) | 16sp | 22sp |
| bodyLarge | Normal (400) | 16sp | 24sp |
| bodyMedium | Normal (400) | 14sp | 20sp |
| labelLarge | Bold (700) | 14sp | 18sp |

### 3. Spacing System

**Source of truth:** `design/AppSpacing.kt`

4dp base grid: 2, 4, 8, 12, 16, 20, 24, 32, 40

### 4. Shape System

**Source of truth:** `design/AppShapes.kt`

Radius scale: 4, 8, 12, 16, 20, 28, 999(pill)

### 5. Elevation System

**Source of truth:** `design/AppElevation.kt`

Scale: 0, 2, 4, 8, 16dp

### 6. State Components

**Source of truth:** `design/components/StateComponents.kt`

| Component | Props | Accessibility |
|---|---|---|
| LoadingScreen | message | contentDescription = message |
| EmptyScreen | title, message | -- |
| ErrorScreen | error, onRetry | -- |
| OfflineScreen | onRetry | -- |

### 7. Rules

1. All colors from AppColors or MaterialTheme.colorScheme
2. All spacing from AppSpacing
3. All shapes from AppShapes
4. All elevations from AppElevation
5. All animation durations from AppAnimation
6. All user-facing text from AppStrings
7. All state rendering via StateComponents when applicable
