---
name: compose-animation-scroll
description: Compose Multiplatform scroll-driven animation skill — scroll-linked effects, parallax, reveal-on-scroll, sticky/pinned sections. Use ONLY when the user asks for scroll-based animations, parallax, or reveal-on-scroll effects in a specific screen design.
trigger: User explicitly requests scroll animations, parallax, reveal-on-scroll, or sticky sections.
---

# Compose Scroll-Driven Animations (adapted from GSAP ScrollTrigger)

## When to Use This Skill

Apply ONLY when the user explicitly asks for:
- Scroll-linked animations (elements animate as you scroll)
- Parallax effects
- Reveal-on-scroll (elements fade/slide in when they enter viewport)
- Sticky/pinned sections during scroll
- Scrub-style animations tied to scroll progress

## Core Concept: ScrollTrigger → LazyListState + derivedStateOf

| GSAP ScrollTrigger | Compose Equivalent |
|---|---|
| `scrollTrigger: { trigger: ".box" }` | Observe item visibility via `LazyListState` |
| `scrub: true` | `derivedStateOf` mapping scroll offset to animation progress |
| `pin: true` | `stickyHeader { }` in `LazyColumn` or `Modifier.zIndex()` |
| `toggleActions: "play reverse"` | State toggle based on scroll position |
| `start / end` | First/last visible item index or pixel offset |
| `ScrollTrigger.batch()` | `snapshotFlow { }` on visible items |
| `markers: true` | Debug with `Log.d` or overlay text |

## Scroll Progress (like scrub)

```kotlin
val scrollState = rememberLazyListState()

val scrollProgress by remember {
    derivedStateOf {
        val firstVisible = scrollState.firstVisibleItemIndex
        val offset = scrollState.firstVisibleItemScrollOffset
        val totalItems = 20 // known total
        ((firstVisible.toFloat() + offset / 500f) / totalItems).coerceIn(0f, 1f)
    }
}

// Use scrollProgress (0f..1f) to drive any animation
Box(
    Modifier.graphicsLayer {
        alpha = scrollProgress
        translationX = lerp(0f, 200f, scrollProgress)
    }
)
```

## Reveal-on-Scroll (like ScrollTrigger toggle)

```kotlin
@Composable
fun RevealOnScroll(
    listState: LazyListState,
    itemIndex: Int,
    content: @Composable (alpha: Float, offsetY: Float) -> Unit
) {
    val isVisible by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            visibleItems.any { it.index == itemIndex }
        }
    }

    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(30f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            launch { alpha.animateTo(1f, tween(AppAnimation.DurationMedium)) }
            launch {
                offsetY.animateTo(
                    0f,
                    tween(AppAnimation.DurationMedium, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    content(alpha.value, offsetY.value)
}
```

Usage in LazyColumn:

```kotlin
val listState = rememberLazyListState()

LazyColumn(state = listState) {
    itemsIndexed(data) { index, item ->
        RevealOnScroll(listState, index) { alpha, offsetY ->
            Card(
                modifier = Modifier.graphicsLayer {
                    this.alpha = alpha
                    translationY = offsetY
                }
            ) {
                ItemContent(item)
            }
        }
    }
}
```

## Batch Reveal with Stagger (like ScrollTrigger.batch)

```kotlin
val listState = rememberLazyListState()

val newlyVisibleItems by remember {
    derivedStateOf {
        listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
    }
}

LazyColumn(state = listState) {
    itemsIndexed(items) { index, item ->
        val hasAppeared = remember { mutableStateOf(false) }
        val alpha = remember { Animatable(0f) }
        val offsetY = remember { Animatable(24f) }

        LaunchedEffect(index in newlyVisibleItems) {
            if (index in newlyVisibleItems && !hasAppeared.value) {
                hasAppeared.value = true
                val staggerDelay = (index % 5) * AppAnimation.StaggerDelay
                delay(staggerDelay)
                launch { alpha.animateTo(1f, tween(AppAnimation.DurationMedium)) }
                launch { offsetY.animateTo(0f, tween(AppAnimation.DurationMedium, easing = FastOutSlowInEasing)) }
            }
        }

        ItemCard(
            item = item,
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetY.value
            }
        )
    }
}
```

## Parallax Effect

```kotlin
val scrollState = rememberLazyListState()

val parallaxOffset by remember {
    derivedStateOf {
        val firstVisible = scrollState.firstVisibleItemIndex
        val offset = scrollState.firstVisibleItemScrollOffset.toFloat()
        -(firstVisible * 100f + offset * 0.3f) // slower movement = parallax
    }
}

Box {
    // Background image with parallax
    Image(
        painter = painterResource(Res.drawable.bg),
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = parallaxOffset }
    )
    // Foreground scrollable content
    LazyColumn(state = scrollState) { /* ... */ }
}
```

## Parallax per-item

```kotlin
LazyColumn(state = listState) {
    itemsIndexed(items) { index, item ->
        val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
        val parallaxFactor = if (itemInfo != null) {
            val viewportHeight = listState.layoutInfo.viewportEndOffset.toFloat()
            val itemCenter = itemInfo.offset + itemInfo.size / 2f
            (itemCenter / viewportHeight - 0.5f) * 2f // -1..1 range
        } else 0f

        Card(
            modifier = Modifier.graphicsLayer {
                translationY = parallaxFactor * -20f // subtle parallax
                scaleX = 1f - (kotlin.math.abs(parallaxFactor) * 0.05f)
                scaleY = 1f - (kotlin.math.abs(parallaxFactor) * 0.05f)
            }
        )
    }
}
```

## Sticky/Pinned Section (like pin: true)

```kotlin
LazyColumn {
    // Regular items
    items(headerItems) { Header(it) }

    // Pinned section (like ScrollTrigger pin)
    stickyHeader {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(/* ... */)
        }
    }

    // Content below pinned section
    items(contentItems) { ContentCard(it) }
}
```

## Scroll-driven header collapse

```kotlin
val scrollState = rememberLazyListState()

val headerProgress by remember {
    derivedStateOf {
        val offset = scrollState.firstVisibleItemScrollOffset.toFloat()
        val index = scrollState.firstVisibleItemIndex
        if (index == 0) (offset / 300f).coerceIn(0f, 1f) else 1f
    }
}

Column {
    // Collapsing header
    Box(
        Modifier
            .height(lerp(200.dp, 56.dp, headerProgress))
            .graphicsLayer {
                alpha = lerp(1f, 0.8f, headerProgress)
            }
    ) {
        HeaderContent()
    }

    LazyColumn(state = scrollState) { /* items */ }
}
```

## Best Practices

- Use `derivedStateOf` to avoid unnecessary recompositions from scroll state
- Use `snapshotFlow` for one-time triggers (like ScrollTrigger `once: true`)
- Use `graphicsLayer` for all scroll-driven visual changes (GPU)
- Track "has appeared" state to avoid re-triggering entrance animations
- Use `Modifier.zIndex()` for elevation during scroll
- Debounce scroll-driven calculations for complex computations

## Do Not

- Read `scrollState` directly in composition — always use `derivedStateOf`
- Trigger heavy recompositions on every scroll pixel
- Animate layout properties (`size`, `padding`) based on scroll — use `graphicsLayer`
- Forget to `coerceIn` progress values to 0f..1f
- Use scroll animations on simple static lists
