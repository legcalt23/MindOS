---
name: compose-animation-utils
description: Compose Multiplatform animation utility functions — lerp, clamp, mapRange, snap, easing presets, common animation helpers. Use ONLY when building animated screens that need mathematical interpolation, value mapping, or custom easing beyond standard Compose APIs.
trigger: User asks for custom easing, value interpolation, or animation utility functions.
---

# Compose Animation Utilities (adapted from GSAP Utils)

## When to Use This Skill

Apply when building animated screens that need:
- Value interpolation (lerp)
- Range mapping (scroll position to opacity)
- Snapping to grid values
- Custom easing curves
- Reusable animation helpers

## Utility Functions

### lerp (linear interpolation)

Compose provides `lerp` for `Float`, `Dp`, `Color`, etc:

```kotlin
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.lerp as dpLerp

// Float interpolation
val value = lerp(start = 0f, stop = 100f, fraction = 0.5f) // 50f

// Dp interpolation
val size = dpLerp(start = 48.dp, stop = 200.dp, fraction = progress)

// Color interpolation
val color = lerp(Color.Red, Color.Blue, fraction = progress)
```

### clamp (like gsap.utils.clamp)

```kotlin
fun clamp(min: Float, max: Float, value: Float): Float = value.coerceIn(min, max)

// Usage
val opacity = (scrollOffset / 300f).coerceIn(0f, 1f)
```

### mapRange (like gsap.utils.mapRange)

```kotlin
fun mapRange(
    inMin: Float, inMax: Float,
    outMin: Float, outMax: Float,
    value: Float
): Float {
    val normalized = (value - inMin) / (inMax - inMin)
    return outMin + normalized * (outMax - outMin)
}

// Usage: map scroll 0-500 to opacity 0-1
val alpha = mapRange(0f, 500f, 0f, 1f, scrollOffset.toFloat()).coerceIn(0f, 1f)

// Map scroll 0-300 to rotation 0-360
val rotation = mapRange(0f, 300f, 0f, 360f, scrollOffset.toFloat())
```

### normalize (like gsap.utils.normalize)

```kotlin
fun normalize(min: Float, max: Float, value: Float): Float =
    ((value - min) / (max - min)).coerceIn(0f, 1f)

// Usage
val progress = normalize(100f, 500f, scrollOffset.toFloat())
```

### snap (like gsap.utils.snap)

```kotlin
fun snap(increment: Float, value: Float): Float =
    (value / increment).roundToInt() * increment

fun snap(values: List<Float>, value: Float): Float =
    values.minByOrNull { abs(it - value) } ?: value

// Usage
val snappedX = snap(50f, dragOffset) // snaps to multiples of 50
val snappedProgress = snap(listOf(0f, 0.25f, 0.5f, 0.75f, 1f), rawProgress)
```

### wrap (like gsap.utils.wrap)

```kotlin
fun wrap(min: Float, max: Float, value: Float): Float {
    val range = max - min
    return min + ((value - min) % range + range) % range
}

// Usage: infinite rotation
val rotation = wrap(0f, 360f, rawRotation)
```

## Custom Easing Presets

```kotlin
object AppEasing {
    // Standard (like power1-2)
    val Standard = FastOutSlowInEasing
    val Decelerate = LinearOutSlowInEasing
    val Accelerate = FastOutLinearInEasing

    // Expressive (like power3-4)
    val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val EaseInOutExpo = CubicBezierEasing(0.87f, 0f, 0.13f, 1f)
    val EaseOutCirc = CubicBezierEasing(0f, 0.55f, 0.45f, 1f)
    val EaseInOutCirc = CubicBezierEasing(0.85f, 0f, 0.15f, 1f)

    // Overshoot (like back)
    val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val EaseInOutBack = CubicBezierEasing(0.68f, -0.6f, 0.32f, 1.6f)

    // Smooth sine (like sine)
    val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
    val EaseOutSine = CubicBezierEasing(0.61f, 1f, 0.88f, 1f)
}
```

## Spring Presets (like elastic/bounce eases)

```kotlin
object AppSprings {
    // Gentle (standard UI motion)
    val Gentle = spring<Float>(dampingRatio = 0.8f, stiffness = 400f)

    // Bouncy (like back.out for buttons, badges)
    val Bouncy = spring<Float>(dampingRatio = 0.5f, stiffness = 300f)

    // Wobbly (like elastic.out for playful entrances)
    val Wobbly = spring<Float>(dampingRatio = 0.3f, stiffness = 200f)

    // Stiff (like snappy feedback)
    val Stiff = spring<Float>(dampingRatio = 1.0f, stiffness = 1500f)

    // Slow (like dramatic entrances)
    val Slow = spring<Float>(dampingRatio = 0.7f, stiffness = 100f)
}
```

## Tween Presets

```kotlin
object AppTweens {
    // Quick micro-interaction
    fun fast(easing: Easing = FastOutSlowInEasing) =
        tween<Float>(AppAnimation.DurationFast, easing = easing)

    // Standard transition
    fun medium(easing: Easing = FastOutSlowInEasing) =
        tween<Float>(AppAnimation.DurationMedium, easing = easing)

    // Slow emphasis
    fun slow(easing: Easing = AppEasing.EaseOutExpo) =
        tween<Float>(AppAnimation.DurationSlow, easing = easing)

    // Dramatic reveal
    fun dramatic(easing: Easing = AppEasing.EaseOutExpo) =
        tween<Float>(AppAnimation.DurationDramatic, easing = easing)

    // Fade in
    val fadeIn = tween<Float>(AppAnimation.DurationMedium, easing = LinearEasing)

    // Fade out (faster)
    val fadeOut = tween<Float>(AppAnimation.DurationFast, easing = LinearEasing)
}
```

## Reusable Modifier Extensions

```kotlin
fun Modifier.animatedEntrance(
    alpha: Float,
    offsetY: Float = 0f,
    offsetX: Float = 0f,
    scale: Float = 1f,
    rotation: Float = 0f
): Modifier = this.graphicsLayer {
    this.alpha = alpha
    this.translationY = offsetY
    this.translationX = offsetX
    this.scaleX = scale
    this.scaleY = scale
    this.rotationZ = rotation
}
```

## Best Practices

- Define easing/spring presets once, reference everywhere
- Use `coerceIn` on all mapped/normalized values
- Use `lerp` from `androidx.compose.ui.util` — don't roll your own
- Keep utility functions pure (no side effects)
- Combine utils with `graphicsLayer` for GPU-composited results

## Do Not

- Create custom lerp/clamp when Compose provides them
- Use these utilities outside of animation contexts (they're for motion math)
- Define easing presets in individual composables — centralize them
- Forget to coerce values to valid ranges after mapping
