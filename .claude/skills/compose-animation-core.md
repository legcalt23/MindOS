---
name: compose-animation-core
description: Compose Multiplatform animation skill — Animatable, animate*AsState, graphicsLayer transforms, easing, stagger, fade/slide/scale tweens. Use ONLY when the user explicitly asks for a specific screen design or UI with animations. Do NOT apply by default to every screen.
trigger: User explicitly requests animations, motion design, or a polished screen design with transitions/effects.
---

# Compose Animation Core (adapted from GSAP patterns)

## When to Use This Skill

Apply ONLY when the user explicitly asks for:
- A specific screen design with animations
- Motion design, entrance effects, or transitions
- Polished UI with slide/fade/scale/stagger effects
- Scroll-driven animations or parallax

Do NOT apply animations by default. Simple screens, forms, lists, and standard UI should use no custom animations unless requested.

## Core Concepts — GSAP → Compose Mapping

| GSAP | Compose Equivalent |
|------|-------------------|
| `gsap.to(target, { x: 100 })` | `Animatable.animateTo()` or `animate*AsState()` |
| `gsap.from(target, { opacity: 0 })` | `AnimatedVisibility` with enter/exit transitions |
| `gsap.fromTo()` | `Animatable` with `snapTo()` then `animateTo()` |
| `gsap.set()` | Direct state assignment (no animation) |
| `duration` | `durationMillis` in `tween()` / `spring()` |
| `delay` | `delay(ms)` in coroutine or `delayMillis` in `tween()` |
| `ease` | `Easing` functions or `spring()` |
| `stagger` | `forEachIndexed` with `delay(index * staggerMs)` |
| `overwrite` | `animationSpec` with `initialValue` or `Animatable.stop()` |
| `repeat / yoyo` | `infiniteRepeatable(repeatMode = RepeatMode.Reverse)` |
| `onComplete` | Coroutine code after `animateTo()` completes |
| transforms (`x`, `y`, `scale`, `rotation`) | `Modifier.graphicsLayer { }` |
| `autoAlpha` | `Modifier.graphicsLayer { alpha = value }` (GPU-composited) |
| `clearProps` | Reset state to initial values |

## Easing — GSAP → Compose

| GSAP Ease | Compose Equivalent |
|-----------|-------------------|
| `"power1.out"` (default) | `FastOutSlowInEasing` |
| `"power2.out"` | `FastOutSlowInEasing` |
| `"power3.inOut"` | `FastOutLinearInEasing` + custom |
| `"back.out(1.7)"` | `spring(dampingRatio = 0.6f, stiffness = 300f)` |
| `"elastic.out(1, 0.3)"` | `spring(dampingRatio = 0.3f, stiffness = 200f)` |
| `"none"` (linear) | `LinearEasing` |
| `"sine.inOut"` | `EaseInOutSine` (custom: `CubicBezierEasing(0.37f, 0f, 0.63f, 1f)`) |
| `"expo.out"` | `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)` |
| `"circ.out"` | `CubicBezierEasing(0f, 0.55f, 0.45f, 1f)` |
| `"bounce.out"` | Custom with `keyframes {}` or spring with low damping |

### Custom Easing (like CustomEase)

```kotlin
val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
```

## Tween Patterns

### Simple "to" animation (like gsap.to)

```kotlin
val offsetX = remember { Animatable(0f) }

LaunchedEffect(trigger) {
    offsetX.animateTo(
        targetValue = 100f,
        animationSpec = tween(
            durationMillis = AppAnimation.DurationMedium,
            easing = FastOutSlowInEasing
        )
    )
}

Box(Modifier.graphicsLayer { translationX = offsetX.value.dp.toPx() })
```

### "from" animation (entrance, like gsap.from)

```kotlin
val alpha = remember { Animatable(0f) }
val translationY = remember { Animatable(40f) }

LaunchedEffect(Unit) {
    launch { alpha.animateTo(1f, tween(AppAnimation.DurationMedium)) }
    launch { translationY.animateTo(0f, tween(AppAnimation.DurationMedium, easing = FastOutSlowInEasing)) }
}

Box(
    Modifier.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
)
```

### Stagger (like gsap stagger)

```kotlin
items.forEachIndexed { index, item ->
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        delay(index * AppAnimation.StaggerDelay)
        launch { alpha.animateTo(1f, tween(AppAnimation.DurationMedium)) }
        launch { offsetY.animateTo(0f, tween(AppAnimation.DurationMedium, easing = FastOutSlowInEasing)) }
    }

    ItemCard(
        item = item,
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        }
    )
}
```

### Infinite repeat + yoyo (like repeat: -1, yoyo: true)

```kotlin
val pulse = rememberInfiniteTransition()
val scale by pulse.animateFloat(
    initialValue = 1f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)

Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
```

## Transform Preferences (Performance)

ALWAYS prefer `Modifier.graphicsLayer { }` over layout-modifying properties:

- **translationX/Y** instead of `offset` or `padding` for movement
- **scaleX/Y** instead of changing `size`
- **rotationZ** (or rotationX/Y for 3D) instead of custom drawing
- **alpha** instead of `Modifier.alpha()` (graphicsLayer is GPU-composited)

```kotlin
Modifier.graphicsLayer {
    translationX = animatedX.value
    translationY = animatedY.value
    scaleX = animatedScale.value
    scaleY = animatedScale.value
    rotationZ = animatedRotation.value
    alpha = animatedAlpha.value
    transformOrigin = TransformOrigin(0.5f, 0.5f)
}
```

## AnimatedVisibility (GSAP-style enter/exit)

```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(tween(AppAnimation.DurationMedium)) +
            slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(AppAnimation.DurationMedium, easing = FastOutSlowInEasing)
            ),
    exit = fadeOut(tween(AppAnimation.DurationFast)) +
           slideOutVertically(targetOffsetY = { -it / 4 })
)
```

## Duration Tokens (use AppAnimation)

| Token | Value | Use for |
|-------|-------|---------|
| `AppAnimation.DurationFast` | 150ms | Micro-interactions, toggles, opacity |
| `AppAnimation.DurationMedium` | 300ms | Standard transitions, slides, fades |
| `AppAnimation.DurationSlow` | 500ms | Page enters, complex transforms |
| `AppAnimation.DurationDramatic` | 800ms | Intro sequences, full-screen reveals |
| `AppAnimation.StaggerDelay` | 50ms | Delay between staggered items |

## Best Practices

- Use `graphicsLayer` for all transform animations (GPU-composited, no relayout)
- Use `remember { Animatable() }` for imperative animations in coroutines
- Use `animate*AsState()` for simple state-driven animations
- Use `AnimatedVisibility` for enter/exit with `fadeIn`/`slideIn`
- Use `spring()` for natural motion; `tween()` for precise timing
- Clean up: animations in `LaunchedEffect` auto-cancel on recomposition
- Use `AppAnimation` tokens — never hardcode duration values
- Stagger: multiply index by `AppAnimation.StaggerDelay` in `delay()`

## Do Not

- Animate `size`, `padding`, `offset` when `graphicsLayer` transforms work
- Use `Modifier.alpha()` — use `graphicsLayer { alpha = }` instead
- Hardcode animation durations — use `AppAnimation` tokens
- Create animations without `remember` — they'll reset on recomposition
- Nest `AnimatedVisibility` inside `AnimatedVisibility` without good reason
- Apply animations to every screen by default — only when explicitly requested
