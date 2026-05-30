# ACCESSIBILITY_AUDIT.md

**Date:** 2026-05-27

---

## 1. Current state

### Semantics usage

Compose Multiplatform provides accessibility via `Modifier.semantics {}`. The codebase has:

- Some `contentDescription` on icons (via Material Icons which provide defaults)
- `Role.Button` semantics on some clickable elements
- `statusBarsPadding()` and `navigationBarsPadding()` used consistently

### Gaps identified

| Issue | Severity | Location | Platform |
|---|---|---|---|
| Icon-only buttons without explicit contentDescription | HIGH | Multiple screens | Both |
| Custom Canvas elements without semantics | HIGH | OrbComponent, SanctuaryBackground, ProfileStoriesCarousel3D | Both |
| Loading states not announced to screen reader | MEDIUM | Various | Both |
| Form error messages not linked to fields | MEDIUM | AuthScreen, profile edit | Both |
| Touch targets potentially < 48dp | MEDIUM | Some icon buttons, chips | Both |
| No heading semantics on screen titles | LOW | Most screens | Both |
| Dynamic content changes not announced | LOW | Feed updates, chat messages | Both |

---

## 2. Fixes applied

| Fix | File |
|---|---|
| Added a11y string constants | `design/AppStrings.kt` (A11y* constants) |
| LoadingScreen has contentDescription | `design/components/StateComponents.kt` |

---

## 3. Recommended fixes

### P1 (High severity)

1. Add `contentDescription` to all icon-only buttons (back, settings, search, edit, close)
2. Add `clearAndSetSemantics` to custom Canvas components that convey information
3. Ensure all interactive elements have minimum 48dp touch target

### P2 (Medium severity)

4. Add `liveRegion = LiveRegionMode.Polite` to error messages so screen readers announce them
5. Link form error messages to their fields via `semantics { error = "..." }`
6. Add loading state announcements

### P3 (Low severity)

7. Add `heading()` semantics to screen titles
8. Add traversal order hints where needed
9. Test with TalkBack (Android) and VoiceOver (iOS)

---

## 4. Accessibility parity

Since all UI is in commonMain Compose, accessibility semantics apply equally to both platforms. The Compose framework translates semantics to:
- **Android:** AccessibilityNodeInfo (TalkBack)
- **iOS:** UIAccessibility (VoiceOver)

**Parity: 100%** — any accessibility fix in Compose applies to both platforms automatically.

---

## 5. Risk assessment

| Risk | Impact | Mitigation |
|---|---|---|
| Screen reader unusable on some screens | HIGH | Add semantics to interactive elements |
| Touch targets too small | MEDIUM | Use AppSpacing.MinTouchTarget (48dp) |
| Dynamic content not announced | LOW | Add liveRegion where needed |
