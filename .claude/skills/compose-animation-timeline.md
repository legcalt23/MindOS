---
name: compose-animation-timeline
description: Compose Multiplatform timeline/sequencing skill — orchestrate multi-step animations with coroutines, parallel/sequential choreography, labels, playback control. Use ONLY when the user asks for sequenced, multi-step, or choreographed animations in a screen design.
trigger: User explicitly requests sequenced animations, choreographed entrances, or multi-step motion design.
---

# Compose Animation Timeline (adapted from GSAP Timeline)

## When to Use This Skill

Apply ONLY when the user explicitly asks for:
- Multi-step animation sequences
- Choreographed entrance/exit animations
- Timeline-like orchestration of multiple elements
- Complex animation coordination

## Core Concept: Coroutine-based Timelines

GSAP uses `gsap.timeline()` with position parameters. In Compose, use **coroutines** with `launch` (parallel) and sequential `animateTo()` calls.

### Sequential (like gsap.timeline default)

```kotlin
LaunchedEffect(Unit) {
    // Step 1: fade in header
    headerAlpha.animateTo(1f, tween(300))
    // Step 2: slide in content (starts after step 1)
    contentOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
    // Step 3: fade in buttons (starts after step 2)
    buttonsAlpha.animateTo(1f, tween(250))
}
```

### Parallel (like position "< " in GSAP)

```kotlin
LaunchedEffect(Unit) {
    // Both run at the same time
    launch { alpha.animateTo(1f, tween(300)) }
    launch { translationY.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
}
```

### Mixed: Sequential groups with parallel items

```kotlin
LaunchedEffect(Unit) {
    // Group 1: header enters (parallel fade + slide)
    coroutineScope {
        launch { headerAlpha.animateTo(1f, tween(300)) }
        launch { headerOffsetY.animateTo(0f, tween(300, easing = FastOutSlowInEasing)) }
    }
    // Group 2: starts AFTER group 1 completes — content enters
    delay(100) // gap between groups (like "+=0.1" in GSAP)
    coroutineScope {
        launch { contentAlpha.animateTo(1f, tween(400)) }
        launch { contentOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing)) }
    }
    // Group 3: buttons stagger in
    buttonItems.forEachIndexed { i, _ ->
        launch {
            delay(i * AppAnimation.StaggerDelay)
            buttonAlphas[i].animateTo(1f, tween(250))
        }
    }
}
```

### Position Parameter Equivalents

| GSAP Position | Compose Equivalent |
|--------------|-------------------|
| Default (sequential) | Sequential `animateTo()` calls |
| `"<"` (same start as previous) | `launch { }` inside same `coroutineScope` |
| `"<0.2"` (0.2s after previous start) | `launch { delay(200); animateTo() }` |
| `"+=0.5"` (0.5s after end) | `delay(500)` between sequential calls |
| `"-=0.2"` (overlap 0.2s) | Start next `launch` with negative-equivalent early start |
| `0` (absolute time) | `launch { animateTo() }` at scope start |

### Timeline Defaults (like gsap.timeline({ defaults: {...} }))

```kotlin
// Define shared specs as vals
val enterSpec = tween<Float>(
    durationMillis = AppAnimation.DurationMedium,
    easing = FastOutSlowInEasing
)
val fadeSpec = tween<Float>(durationMillis = AppAnimation.DurationFast)

LaunchedEffect(Unit) {
    launch { headerAlpha.animateTo(1f, fadeSpec) }
    launch { headerOffsetY.animateTo(0f, enterSpec) }
    delay(100)
    launch { contentAlpha.animateTo(1f, fadeSpec) }
    launch { contentOffsetY.animateTo(0f, enterSpec) }
}
```

### Labels (like tl.addLabel)

```kotlin
// Use suspend functions as named "labels"
suspend fun introSequence() {
    coroutineScope {
        launch { logoScale.animateTo(1f, spring(dampingRatio = 0.6f)) }
        launch { logoAlpha.animateTo(1f, tween(400)) }
    }
}

suspend fun contentSequence() {
    titleAlpha.animateTo(1f, tween(300))
    delay(50)
    subtitleAlpha.animateTo(1f, tween(300))
}

suspend fun ctaSequence() {
    buttonScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
}

LaunchedEffect(Unit) {
    introSequence()
    delay(200)
    contentSequence()
    delay(100)
    ctaSequence()
}
```

### Playback Control (like tl.play/pause/reverse/restart)

```kotlin
class AnimationTimeline {
    private var job: Job? = null
    var progress by mutableFloatStateOf(0f)
        private set

    fun play(scope: CoroutineScope, block: suspend () -> Unit) {
        job = scope.launch { block() }
    }

    fun pause() { job?.cancel() }

    fun restart(scope: CoroutineScope, block: suspend () -> Unit) {
        job?.cancel()
        // Reset all animatables to initial values, then play
        play(scope, block)
    }
}
```

### Reverse animation

```kotlin
val expanded = remember { mutableStateOf(false) }
val height = remember { Animatable(0f) }

LaunchedEffect(expanded.value) {
    if (expanded.value) {
        height.animateTo(200f, tween(300, easing = FastOutSlowInEasing))
    } else {
        height.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
    }
}
```

## Screen Entrance Pattern (common timeline)

```kotlin
@Composable
fun AnimatedScreen(content: @Composable (ScreenAnimState) -> Unit) {
    val bgAlpha = remember { Animatable(0f) }
    val headerOffsetY = remember { Animatable(-30f) }
    val headerAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(40f) }
    val contentAlpha = remember { Animatable(0f) }

    val enterSpec = tween<Float>(AppAnimation.DurationMedium, easing = FastOutSlowInEasing)

    LaunchedEffect(Unit) {
        // Background fade
        launch { bgAlpha.animateTo(1f, tween(AppAnimation.DurationSlow)) }
        delay(150)
        // Header slides down
        coroutineScope {
            launch { headerAlpha.animateTo(1f, tween(AppAnimation.DurationFast)) }
            launch { headerOffsetY.animateTo(0f, enterSpec) }
        }
        delay(100)
        // Content slides up
        coroutineScope {
            launch { contentAlpha.animateTo(1f, tween(AppAnimation.DurationFast)) }
            launch { contentOffsetY.animateTo(0f, enterSpec) }
        }
    }

    // Pass animated values to content
    content(ScreenAnimState(bgAlpha, headerOffsetY, headerAlpha, contentOffsetY, contentAlpha))
}
```

## Best Practices

- Use `coroutineScope { }` to group parallel animations that must finish before the next step
- Use `launch { }` for fire-and-forget parallel animations
- Use `delay()` for gaps between steps
- Extract named `suspend fun` for reusable sequences (like GSAP labels)
- Define shared `AnimationSpec` vals (like GSAP timeline defaults)
- All Animatables must be `remember { }`'d

## Do Not

- Nest `LaunchedEffect` inside `LaunchedEffect`
- Forget to use `coroutineScope` when you need to wait for parallel animations
- Use `Thread.sleep()` — always use `delay()`
- Create Animatables without `remember`
- Apply timeline patterns to simple single-property animations — use `animate*AsState` instead
