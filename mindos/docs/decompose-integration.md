# MindOS Decompose Integration (ZIP10)

The app-wiring layer that connects the engine to **Decompose** navigation. It is the only module
that depends on Decompose; everything upstream stays framework-free. No Compose/Android/UI here —
Decompose's component+navigation model only.

```
User input ─► matcher.understand ─► MindIntent + entities
           ─► IntentToActionMapper ─► MindAction
           ─► ActionExecutor (validate → resolve refs)
           ─► MindNavigationCommand
           ─► DecomposeNavigationCoordinator   ◄── the only navigation entry point
           ─► StackNavigation<Config>.pushNew
           ─► RootComponent.stack (ChildStack)
           ─► RootChild (Profile / Story / Chat / Messages / Settings)  ─► [app renders]

DeepLink / notification ─► DeepLinkBridge ─► MindAction ─► (same pipeline)
```

## Module `mind-decompose`

| Type | Role |
|------|------|
| `Config` (sealed, `@Serializable`) | Decompose child-stack state: `Settings`, `Messages`, `Profile(userId)`, `Story(storyId)`, `Chat(chatId)`. Resolved ids only. |
| `RootComponent` / `DefaultRootComponent` | Hosts the child stack over a real `StackNavigation<Config>`; exposes the single `coordinator`. `ComponentContext by` — a true Decompose component. |
| `ProfileComponent` / `StoryComponent` / `ChatComponent` / `MessagesComponent` / `SettingsComponent` | Per-screen component contracts (UI-agnostic), surfaced via `RootChild`. |
| `DecomposeNavigationCoordinator` | Implements the engine's `NavigationCoordinator`; maps `MindNavigationCommand → Config` and performs the child-stack transition (`pushNew`, idempotent on the top entry). |
| `DeepLinkBridge` | Converts `mindos://…` / `https://mindos.app/…` / notification paths into typed `MindAction`s. Deterministic, UI-free. |

### Dependencies

`mind-decompose` → `mind-navigation`, `mind-action-model`, `com.arkivanov.decompose:decompose`
(`3.3.0`), coroutines, serialization. The executor depends on the `NavigationCoordinator`
**interface**, so it never sees Decompose — the boundary holds: *router never navigates, executor
never knows Compose, coordinator owns navigation side-effects.*

## Boundary guarantees

- Decompose appears in **exactly one** module. `mind-action-execution` and below have zero Decompose
  on their classpath.
- `Config` (Decompose-internal) is distinct from `MindNavigationCommand` (engine vocabulary); the
  coordinator is the single mapping point.
- The coordinator is the **only** navigation entry point (ZIP10 COMPONENTS rule).

## Integration tests (headless, no UI)

Run with a real `LifecycleRegistry` + `DefaultComponentContext` — no Compose/Android:

- **NavigationCommand → Component**: `coordinator.navigate(OpenProfile("u_ana"))` ⇒ active child is
  `RootChild.Profile(userId="u_ana")`; same for Story/Chat.
- **DeepLink → Action**: `mindos://profile/ana` ⇒ `OpenProfile(userRef="ana")`; `notifications` ⇒
  `OpenMessages`; malformed/`mindos://profile` (no id) ⇒ `null`.
- **Resolver → Navigation (full pipeline)**: `"open ana"` ⇒ understand ⇒ map ⇒ execute (resolve
  `ana`→`u_ana`) ⇒ coordinator ⇒ child stack shows `Profile("u_ana")`.
- **DeepLink → Action → Navigation**: `mindos://profile/ana` through the executor reaches the same
  resolved navigation.

## App wiring (outside this repo)

The Android/Desktop app:
1. Builds `DefaultRootComponent(defaultComponentContext())` at the root.
2. Wires `DefaultActionExecutor(..., coordinator = root.coordinator)`.
3. Renders `root.stack` with Compose/SwiftUI, switching on `RootChild`.
4. Feeds OS deep links / FCM payloads through `DeepLinkBridge` → `executor.execute`.

The Compose rendering of `RootChild` is the only remaining UI-coupled piece and lives in the app,
not here.

## Prohibited (by design)

No Compose/Android imports in this module. The executor never depends on Decompose. The matcher
never navigates. Deep links go through the same validate→resolve→navigate pipeline as typed
commands — no shortcut that bypasses validation/resolution.

## Deferred (v1.5+)

Back-stack policies beyond `pushNew` (replace-all on tab switch, `popTo`), multi-stack (bottom-nav)
hosts, deep-link parameter typing (query args), and iOS/Android `actual` entry points for
`defaultComponentContext()`.
