# NAVIGATION_PARITY_REPORT.md

**Date:** 2026-05-27

---

## 1. Navigation architecture

### Type: Custom state-based routing

- **Route definition:** `AppRoute` sealed interface in `core/navigation/AppRoute.kt`
- **Route resolution:** `LaunchDestinationResolver` in `core/navigation/LaunchDestinationResolver.kt`
- **Route rendering:** `MindOSApp` composable in `App.kt`
- **Back handling:** `FireBackGestureHandler` in `core/ui/gesture/`

### All navigation is in commonMain — 100% shared

---

## 2. Route inventory

| Route | Requires Auth | Back Target | Deep Link |
|---|---|---|---|
| Loading | No | N/A | No |
| Auth | No | N/A (root) | No |
| ElementSelection | No | N/A | No |
| Main(user) | Yes | N/A (root) | No |
| GlobalFeed(user) | Yes | Main | No |
| Settings(user) | Yes | Main | No |
| MediaViewer(user, content) | Yes | Previous route | No |
| DMList(user) | Yes | Main | No |
| DMChat(user, chatId, otherUserId) | Yes | DMList | No |
| DMChatUserInfo(user, chatId, otherUserId) | Yes | DMChat | No |
| DMCajonera(user, chatId?, otherUserId?) | Yes | DMChatUserInfo or DMList | No |
| Moneda(user) | Yes | Main | No |

---

## 3. Back behavior audit

| From | Back navigates to | Correct? |
|---|---|---|
| GlobalFeed | Main | YES |
| Settings | Main | YES |
| MediaViewer | Previous route (stored) | YES |
| DMList | Main | YES |
| DMChat | DMList | YES |
| DMChatUserInfo | DMChat | YES |
| DMCajonera (with chat context) | DMChatUserInfo | YES |
| DMCajonera (without chat context) | DMList | YES |
| Moneda | Main | YES |

**Result: Back behavior is consistent and well-defined.**

---

## 4. Auth flow

1. App starts -> `LaunchDestinationResolver.resolveInitialRoute()`
2. If user session exists -> `Main(user)`
3. If no session -> `Auth`
4. User signs in -> `resolvePostLoginRoute()` -> `Main(user)`
5. `authState` Flow monitors session changes
6. If session lost (sign out / token expired) -> `Auth`

**Auth flow is centralized and shared. No platform-specific auth UI.**

---

## 5. Navigation gaps

| Gap | Severity | Recommendation |
|---|---|---|
| No deep links | LOW | Add deep link support when needed for sharing |
| No animated transitions | LOW | Compose navigation transitions could enhance UX |
| No state restoration | MEDIUM | App kill -> restart loses navigation stack |
| Route strings not type-safe beyond sealed class | LOW | Current implementation is sufficient |
| No onboarding flow connected | LOW | ElementSelection route exists but not wired |

---

## 6. Orphan screen check

All screens are reachable via AppRoute. No orphan screens detected.

---

## 7. Platform differences

| Aspect | Android | iOS | Desktop |
|---|---|---|---|
| System back | Back button/gesture | Swipe from edge | N/A |
| Back handling | FireBackGestureHandler | FireBackGestureHandler | FireBackGestureHandler |
| Result | Identical behavior | Identical behavior | Identical behavior |

**Compose Multiplatform's `FireBackGestureHandler` (custom) normalizes back behavior across platforms.**
