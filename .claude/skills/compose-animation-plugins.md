---
name: compose-animation-plugins
description: Compose Multiplatform advanced animation patterns — Flip/layout animations, draggable, text animation, morph/path, physics-based spring. Use ONLY when the user asks for advanced motion effects like layout transitions, drag interactions, text reveals, or spring physics in a specific screen design.
trigger: User explicitly requests flip animations, drag interactions, text reveal effects, morphing, path animation, or spring physics.
---

# Compose Advanced Animation Patterns (adapted from GSAP Plugins)

## When to Use This Skill

Apply ONLY when the user explicitly asks for:
- Layout transition animations (Flip-style)
- Drag interactions with momentum
- Text character/word animation
- Shape morphing or path-following animation
- Spring/physics-based motion
- Custom gesture-driven animations

## Flip / Layout Animation (like GSAP Flip)

Compose uses `animateItem()` and `AnimatedContent` for layout transitions.

### List reorder animation (like Flip in a list)

```kotlin
LazyColumn {
    items(sortedItems, key = { it.id }) { item ->
        ItemCard(
            item = item,
            modifier = Modifier.animateItem(
                fadeInSpec = tween(AppAnimation.DurationMedium),
                fadeOutSpec = tween(AppAnimation.DurationFast),
                placementSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        )
    }
}
```

### AnimatedContent for state transitions (like Flip between states)

```kotlin
AnimatedContent(
    targetState = currentState,
    transitionSpec = {
        (fadeIn(tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300)))
            .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 0.92f, animationSpec = tween(200)))
    }
) { state ->
    when (state) {
        ViewState.Grid -> GridLayout(items)
        ViewState.List -> ListLayout(items)
    }
}
```

### Size animation (expand/collapse like Flip)

```kotlin
Box(
    Modifier
        .animateContentSize(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
) {
    if (expanded) ExpandedContent() else CollapsedContent()
}
```

## Draggable (like GSAP Draggable)

### Basic drag with snap-back

```kotlin
val offsetX = remember { Animatable(0f) }
val offsetY = remember { Animatable(0f) }
val scope = rememberCoroutineScope()

Box(
    Modifier
        .graphicsLayer {
            translationX = offsetX.value
            translationY = offsetY.value
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDrag = { _, dragAmount ->
                    scope.launch {
                        offsetX.snapTo(offsetX.value + dragAmount.x)
                        offsetY.snapTo(offsetY.value + dragAmount.y)
                    }
                },
                onDragEnd = {
                    // Snap back with spring (like inertia: false)
                    scope.launch {
                        launch { offsetX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        launch { offsetY.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    }
                }
            )
        }
)
```

### Drag with velocity/fling (like Draggable + InertiaPlugin)

```kotlin
val offsetX = remember { Animatable(0f) }
val scope = rememberCoroutineScope()

Box(
    Modifier
        .graphicsLayer { translationX = offsetX.value }
        .pointerInput(Unit) {
            val decay = splineBasedDecay<Float>(this)
            detectHorizontalDragGestures(
                onDrag = { _, dragAmount ->
                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                },
                onDragEnd = { velocity ->
                    scope.launch {
                        offsetX.animateDecay(velocity, decay)
                    }
                }
            )
        }
)
```

### Drag with bounds (like Draggable bounds)

```kotlin
val offsetX = remember { Animatable(0f, Float.VectorConverter) }

LaunchedEffect(Unit) {
    offsetX.updateBounds(-200f, 200f)
}

// Same drag gesture but bounds are enforced automatically
```

### Swipe-to-dismiss (like Draggable type: "x")

```kotlin
val dismissState = rememberSwipeToDismissBoxState()
SwipeToDismissBox(
    state = dismissState,
    backgroundContent = { /* delete icon */ }
) {
    CardContent()
}
```

## Text Animation (like GSAP SplitText)

### Character-by-character reveal

```kotlin
@Composable
fun AnimatedText(
    text: String,
    staggerMs: Long = 30L,
    durationMs: Int = AppAnimation.DurationFast
) {
    Row {
        text.forEachIndexed { index, char ->
            val alpha = remember { Animatable(0f) }
            val offsetY = remember { Animatable(12f) }

            LaunchedEffect(Unit) {
                delay(index * staggerMs)
                launch { alpha.animateTo(1f, tween(durationMs)) }
                launch { offsetY.animateTo(0f, tween(durationMs, easing = FastOutSlowInEasing)) }
            }

            Text(
                text = char.toString(),
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha.value
                    translationY = offsetY.value
                },
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}
```

### Word-by-word reveal

```kotlin
@Composable
fun AnimatedWords(
    text: String,
    staggerMs: Long = 80L,
    durationMs: Int = AppAnimation.DurationMedium
) {
    val words = text.split(" ")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        words.forEachIndexed { index, word ->
            val alpha = remember { Animatable(0f) }
            val offsetY = remember { Animatable(20f) }

            LaunchedEffect(Unit) {
                delay(index * staggerMs)
                launch { alpha.animateTo(1f, tween(durationMs)) }
                launch { offsetY.animateTo(0f, tween(durationMs, easing = FastOutSlowInEasing)) }
            }

            Text(
                text = "$word ",
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha.value
                    translationY = offsetY.value
                }
            )
        }
    }
}
```

### Scramble/typewriter text (like ScrambleText)

```kotlin
@Composable
fun ScrambleText(
    targetText: String,
    scrambleChars: String = "01!@#$%",
    revealDurationMs: Long = 1500L
) {
    var displayText by remember { mutableStateOf("") }

    LaunchedEffect(targetText) {
        val totalSteps = targetText.length
        for (i in 0..totalSteps) {
            val revealed = targetText.substring(0, i)
            val scrambled = buildString {
                repeat(targetText.length - i) {
                    append(scrambleChars.random())
                }
            }
            displayText = revealed + scrambled
            delay(revealDurationMs / totalSteps)
        }
    }

    Text(text = displayText, fontFamily = FontFamily.Monospace)
}
```

## Spring Physics (like GSAP elastic/back/bounce)

```kotlin
// Back ease (overshoot like back.out(1.7))
spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy, // 0.5f
    stiffness = Spring.StiffnessMedium               // 1500f
)

// Elastic (like elastic.out)
spring<Float>(
    dampingRatio = 0.3f,  // low = more oscillation
    stiffness = 200f      // low = slower
)

// Bouncy entrance
spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,  // 0.2f
    stiffness = Spring.StiffnessLow                // 200f
)

// Stiff/snappy (no bounce)
spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,  // 1.0f
    stiffness = Spring.StiffnessHigh              // 10000f
)
```

### Spring reference table

| GSAP Feel | `dampingRatio` | `stiffness` | Use for |
|-----------|---------------|-------------|---------|
| `back.out(1.7)` | 0.5f | 1500f | Button press, card pop |
| `elastic.out(1, 0.3)` | 0.3f | 200f | Playful entrances |
| `bounce.out` | 0.2f | 200f | Notification badges |
| Smooth ease | 1.0f | 500f | Standard transitions |
| Snappy | 1.0f | 10000f | Instant feedback |

## Custom Gesture (like GSAP Observer)

```kotlin
Modifier.pointerInput(Unit) {
    detectVerticalDragGestures(
        onDragStart = { /* like Observer onDown */ },
        onVerticalDrag = { change, dragAmount ->
            if (dragAmount > 0) { /* swipe down */ }
            else { /* swipe up */ }
        },
        onDragEnd = { /* gesture complete */ }
    )
}
```

## Best Practices

- Use `spring()` for natural, interruptible motion (drag, gestures)
- Use `tween()` for precise, timed sequences
- Use `animateDecay()` for fling/momentum after drag
- Use `animateItem()` in LazyColumn for list reorder
- Use `AnimatedContent` for switching between layouts
- Split text animations only for headers/titles — not body text

## Do Not

- Animate every character in long paragraphs — only short titles/headings
- Use `spring()` with extremely low stiffness (< 50f) — it feels sluggish
- Forget bounds on draggable elements
- Use layout animations (animateContentSize) on rapidly changing content
- Apply these patterns unless the user specifically requests advanced effects
