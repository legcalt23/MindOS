# PERFORMANCE_AUDIT.md

**Date:** 2026-05-27

---

## 1. Architecture performance characteristics

### Strengths

| Aspect | Assessment |
|---|---|
| Compose Multiplatform rendering | Skia-based, hardware accelerated |
| Coroutines for async work | Proper usage of rememberCoroutineScope |
| Firebase offline caching | Built-in Firestore persistence |
| LazyColumn for lists | Used in feed, DM list, search results |
| State scoping | State holders scoped to screen lifecycle |

### Concerns

| Concern | Severity | Location | Impact |
|---|---|---|---|
| Repository instances created in `remember {}` | MEDIUM | HomeScreen, various | May create multiple Firebase instances |
| No image caching strategy in shared code | MEDIUM | Profile images, feed attachments | Network re-fetches |
| Large Compose functions (100+ lines) | LOW | ProfileScreen, HomeScreen | Recomposition scope |
| Canvas-heavy custom components | LOW | Orb, Sanctuary, Chroma | CPU usage |
| No pagination in feed/lists | MEDIUM | GlobalFeedScreen, DMList | Memory growth |

---

## 2. Startup performance

| Phase | Implementation | Assessment |
|---|---|---|
| App init | MindOSApplication.onCreate() | Firebase init + App Check install |
| First route | LaunchDestinationResolver | Synchronous session check (fast) |
| First screen | Auth or Main | Single Compose tree |

**Startup path is lean.** Firebase initialization is the main cost.

---

## 3. Recomposition analysis

### High-risk areas

1. **MindOSApp composable** — Large `when` block with many route cases. Route changes recompose the entire tree.
2. **HomeScreen** — Many `remember {}` blocks creating Firebase repositories. State changes may trigger unnecessary recomposition.
3. **ProfileScreen** — Complex layout with multiple state variables. Some state reads in the root function could cause broad recomposition.

### Mitigation

- Compose compiler plugin (enabled) handles stable class detection
- `remember {}` prevents re-creation on recomposition
- Most state is hoisted to state holders

---

## 4. Memory

| Aspect | Assessment |
|---|---|
| Android | Standard JVM GC, Compose manages render tree |
| iOS | ARC + Kotlin/Native memory model |
| Image loading | Coil (Android only) provides caching; iOS/Desktop pending |
| Firebase listeners | Properly scoped to screen lifecycle via LaunchedEffect |

---

## 5. Network

| Aspect | Assessment |
|---|---|
| Firebase Firestore | Query limit enforcement (per CLAUDE.md rules) |
| No duplicate calls | Most screens fetch once on composition |
| No retry/backoff | Gap — errors shown but no automatic retry |
| Timeout handling | Firebase SDK defaults |

---

## 6. Recommendations

| Priority | Action | Impact |
|---|---|---|
| P1 | Add pagination to feed and DM lists | Memory + latency |
| P2 | Add image caching abstraction (shared) | Network + UX |
| P2 | Add retry with exponential backoff for failed network calls | Resilience |
| P3 | Profile recomposition — extract sub-composables with stable params | CPU |
| P3 | Audit `remember {}` scoping of Firebase repository instances | Memory |
| P4 | Enable Compose compiler metrics for recomposition analysis | Diagnostic |

---

## 7. Performance parity

Since all rendering is Compose Multiplatform (Skia), performance characteristics are nearly identical across platforms.

| Platform | Renderer | Expected Performance |
|---|---|---|
| Android | Skia (hardware) | Good |
| iOS | Skia (Metal) | Good |
| Desktop | Skia (OpenGL/Metal) | Good |

**Main parity risk:** Image loading uses Coil on Android but has no equivalent on iOS/Desktop. This could cause different perceived loading speed.
