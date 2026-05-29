---
name: compose-animation-performance
description: Compose Multiplatform animation performance skill — graphicsLayer optimization, avoiding recomposition, batching, scroll performance. Use when optimizing animations for smooth 60fps or when animations stutter/jank.
trigger: User reports animation jank, asks for performance optimization, or when building complex animated screens.
---

# Compose Animation Performance (adapted from GSAP Performance)

## When to Use This Skill

Apply when:
- Animations stutter or jank on device
- Building screens with many simultaneous animations
- Optimizing scroll-driven animations
- User asks for smooth 60fps performance

## Rule 1: graphicsLayer for All Visual Animation

`Modifier.graphicsLayer { }` runs on the GPU compositor — no layout pass, no measure pass. ALWAYS use it for:

| Animation | Correct (graphicsLayer) | Wrong (triggers relayout) |
|-----------|------------------------|--------------------------|
| Move | `translationX/Y` | `offset()`, `padding()` |
| Fade | `alpha` | `Modifier.alpha()` (separate layer) |
| Scale | `scaleX/Y` | Changing `size()` |
| Rotate | `rotationZ/X/Y` | Custom draw |
| 3D perspective | `cameraDistance` | N/A |

```kotlin
// GOOD: all in one graphicsLayer call
Modifier.graphicsLayer {
    translationX = animX.value
    translationY = animY.value
    scaleX = animScale.value
    scaleY = animScale.value
    alpha = animAlpha.value
    rotationZ = animRotation.value
}

// BAD: separate modifiers trigger multiple passes
Modifier
    .offset(x = animX.value.dp) // triggers relayout
    .alpha(animAlpha.value)      // separate layer
    .scale(animScale.value)      // another layer
```

## Rule 2: derivedStateOf for Scroll-Driven Values

Never read raw scroll state in composition. Always derive:

```kotlin
// GOOD: recomposes only when the derived value changes
val headerAlpha by remember {
    derivedStateOf {
        (1f - scrollState.firstVisibleItemScrollOffset / 300f).coerceIn(0f, 1f)
    }
}

// BAD: recomposes on every scroll pixel
val headerAlpha = 1f - scrollState.firstVisibleItemScrollOffset / 300f
```

## Rule 3: Lambda-Based Modifiers Where Available

When Compose offers a lambda-based modifier, use it to skip recomposition:

```kotlin
// GOOD: graphicsLayer lambda reads state without recomposition
Modifier.graphicsLayer {
    alpha = animAlpha.value // reads inside draw phase
}

// Also GOOD: drawBehind for custom drawing
Modifier.drawBehind {
    drawCircle(color, radius = animRadius.value)
}
```

## Rule 4: Stagger Instead of Simultaneous

Creating many animations at once is expensive. Stagger them:

```kotlin
// GOOD: staggered, fewer concurrent animations
items.forEachIndexed { i, item ->
    LaunchedEffect(Unit) {
        delay(i * AppAnimation.StaggerDelay)
        alpha.animateTo(1f, tween(AppAnimation.DurationMedium))
    }
}

// BAD: all 50 items animate simultaneously
items.forEach { item ->
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(AppAnimation.DurationMedium))
    }
}
```

## Rule 5: Limit Animated Items in Lists

Only animate items that are actually visible:

```kotlin
val visibleIndices by remember {
    derivedStateOf {
        listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
    }
}

// Only run animation for visible items
if (index in visibleIndices && !hasAppeared.value) {
    LaunchedEffect(Unit) { /* animate entrance */ }
}
```

## Rule 6: Reuse Animatables

Don't create new Animatables on recomposition:

```kotlin
// GOOD: remembered, survives recomposition
val scale = remember { Animatable(1f) }

// BAD: new instance every recomposition
val scale = Animatable(1f)
```

## Rule 7: spring() is Interruptible, tween() is Not

Use `spring()` for gestures and interactive animations — it handles interruption gracefully. Use `tween()` for fire-and-forget sequences:

```kotlin
// Gesture response: spring (interruptible)
offsetX.animateTo(target, spring(stiffness = Spring.StiffnessMedium))

// Entrance sequence: tween (predictable timing)
alpha.animateTo(1f, tween(AppAnimation.DurationMedium))
```

## Rule 8: Minimize State Reads in Animated Composables

```kotlin
// GOOD: pass only what's needed
@Composable
fun AnimatedCard(title: String, alpha: Float, offsetY: Float) {
    Card(Modifier.graphicsLayer { this.alpha = alpha; translationY = offsetY }) {
        Text(title)
    }
}

// BAD: pass entire data model — any field change recomposes
@Composable
fun AnimatedCard(item: FullDataModel, alpha: Float, offsetY: Float) { ... }
```

## Performance Checklist

Before shipping animated screens:

1. All movement/fade/scale uses `graphicsLayer { }` — no `offset()` or `Modifier.alpha()`
2. Scroll-driven values use `derivedStateOf`
3. Stagger is applied (not all items animate simultaneously)
4. Only visible items are animated
5. All `Animatable` instances are in `remember { }`
6. No unnecessary recomposition from state reads
7. `spring()` for interactive, `tween()` for choreographed
8. Complex calculations use `snapshotFlow` + `collect` instead of direct reads

## Do Not

- Use `Modifier.offset()` for animated movement — use `graphicsLayer { translationX/Y }`
- Use `Modifier.alpha()` — use `graphicsLayer { alpha = }`
- Read `LazyListState` directly in composition body
- Create hundreds of simultaneous `Animatable` instances
- Use `animate*AsState` where you need imperative control (coroutine-based)
- Animate layout properties (`height`, `width`, `padding`) unless absolutely necessary
- Forget `remember { }` on `Animatable` instances
