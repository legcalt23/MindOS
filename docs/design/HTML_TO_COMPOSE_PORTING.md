# HTML/CSS → Compose Multiplatform — Referencia de portado (comando `/ui`)

Fuente de verdad del sistema `/ui`: coger un componente **HTML/CSS** (Uiverse, CodePen, o diseño
propio) y convertirlo en un **componente Compose Multiplatform idiomático** del design system de
Molvex, **fiel al original**, con **tokens del repo** (no literales), tematizado, accesible,
`commonMain`-safe y limpio de Detekt. El objetivo es que la UI **deje de notarse "hecha por IA"**:
craft fiel + disciplina de tokens.

> **Naming:** "Molvex" es marca; el código NO se renombra — paquete `com.mindos.app`, design system
> en `com.mindos.app.design`.

## Anti-goal (no negociable)

**NO** se renderiza HTML en runtime (nada de `WebView`/motor HTML). Un cognitive-trainer no dibuja
su UI con WebView (perf, consistencia, seguridad — las reglas Kotlin del repo advierten explícito).
`/ui` es un **portado en tiempo de desarrollo**: HTML/CSS entra, un `.kt` Compose sale.

## Pipeline (lo que hace `/ui` por componente)

1. **Deconstruir** el HTML/CSS en una *component-spec* normalizada (abajo). No portar aún.
2. **Portar** la spec a Compose usando la **tabla de mapeo** → tokens.
3. **Auto-revisión** de craft (fidelidad + carácter) y de a11y/cross-platform.
4. **Gate estático** + entregar los comandos Gradle (si el IDE posee el daemon, NO ejecutar CLI).
5. **Samples**: un hermano `<Nombre>Samples.kt` con preview de todos los estados.

## Formato de component-spec (el intermedio)

```
Nombre: <PascalCase, p.ej. GlowButton>
Uso/pantalla destino: <dónde vive>
Estructura: <jerarquía de contenedores>
Box model: <tamaños, padding, gap, alturas> → token objetivo
Colores: <fondo, texto, borde, acento> → AppColors.* / MaterialTheme / favoriteAccentColor
Tipografía: <size, weight, tracking, jerarquía> → MaterialTheme.typography / token
Radios: <border-radius> → AppShapes.*
Sombra: <box-shadow> → AppElevation.* / Modifier.shadow
Estados: default / hover / focus / active-pressed / disabled → qué cambia en cada uno
Motion: <propiedad, duración, easing> → AppAnimation.Duration* + Easing
Accesibilidad: <rol, label, foco, touch target>
Variantes: <tamaños/colores/modos>
Tokens faltantes: <valor CSS sin token → PROPUESTA de token nuevo en su archivo, nunca literal>
```

## Tabla de mapeo CSS → Compose + token (usa SIEMPRE el token, nunca el literal)

| CSS | Compose | Token del repo |
|---|---|---|
| `padding` / `margin` / `gap` | `Modifier.padding` / `Arrangement.spacedBy` | `AppSpacing.{XXS,XS,S,M,L,XL,XXL,XXXL,XXXXL}` (2/4/8/12/16/20/24/32/40 dp); pantalla `ScreenHorizontal`, card `CardPadding` |
| `width`/`height` fijos | `Modifier.size/width/height` | `AppSpacing.{ButtonHeight=48,InputHeight=56,IconSize=24,MinTouchTarget=48,Avatar*}` |
| `border-radius` | `RoundedCornerShape` vía token | `AppShapes.{RadiusXS…RadiusFull}` o formas `Small/Medium/Large/ExtraLarge/Pill/Circle`; semánticas `Button=Pill`, `Input=Small`, `Card=Medium`, `Dialog=Large`, `Chip=Pill` |
| `color` / `background` | `Color` / `Modifier.background` | `AppColors.*` (marca: `NeuGreen #13ECA4`, `NeuGreenSoft`, `NeuViolet`, `NeuCyan`, `CyanFocus`, `TargetGold`, `Composer*`; neutros `DeepBlack #020504`…) o `MaterialTheme.colorScheme.*` para slots Material3 |
| color de **acento** (el que "es del usuario") | `Color` resuelto | `favoriteAccentColor(LocalFavoriteAccent.current, fallback = <colorMarcaPantalla>)` o `favoriteAccentColorOrThemePrimary(LocalFavoriteAccent.current)` — NUNCA un azul/verde hardcoded |
| `box-shadow` / elevación | `Modifier.shadow` / `Surface(tonalElevation=)` | `AppElevation.{None,Low=2,Medium=4,High=8,Highest=16}` |
| `font-size`/`weight`/`letter-spacing` | `TextStyle` | `MaterialTheme.typography.*` (ajusta `fontWeight`/`letterSpacing`); no inventes tamaños sueltos |
| `transition` / `animation` | `animate*AsState` + `tween` | duración `AppAnimation.Duration{Fast=150,Medium=300,Slow=500,Dramatic=800}`; `StaggerDelay=50L` |
| `ease-in-out` | `CubicBezierEasing(0.42f, 0f, 0.58f, 1f)` | **no** `FastOutSlowInEasing` (asimétrico). `ease`→`FastOutSlowInEasing` ok; `linear`→`LinearEasing` |
| `:hover`/`:focus`/`:active` | `InteractionSource` (`collectIsFocusedAsState`, `collectIsPressedAsState`, `hoverable`) | anima color/escala/borde entre estados con los tokens de duración |
| `display:flex` (row/column) | `Row`/`Column` + `Arrangement`/`Alignment` | gaps con `AppSpacing.*` |
| `display:grid` | `Layout` custom / `Row`+`Column` / `LazyVerticalGrid` | — |
| `linear-gradient`/`radial-gradient` | `Brush.linearGradient/radialGradient` + `Modifier.background(brush)` | colores desde `AppColors.*` |
| SVG / icono | `ImageVector` (Material Icons) o `Canvas`/`drawWithCache` | — |
| `backdrop-filter: blur` | `Modifier.blur` (draw-only; no altera medida) | — |
| `opacity` | `Modifier.alpha` / `graphicsLayer{alpha=}` | — |
| `transform: scale/rotate` | `Modifier.graphicsLayer{scaleX/Y, rotationZ}` | — |

### Regla para valores CSS que no casan un token
Redondea al token más cercano si la diferencia es imperceptible; si es un valor de diseño
significativo y recurrente, **añade el token a su archivo** (`AppSpacing`/`AppShapes`/`AppColors`/
`AppElevation`) marcándolo, y úsalo. **Nunca** dejes un `dp`/`Color(0xFF…)`/`RoundedCornerShape(N.dp)`
literal fuera de los archivos de token.

## Reglas de Detekt / naming (ratchet one-way — no regenerar baseline)

- `@Composable` PascalCase → `@file:Suppress("FunctionNaming")` a nivel de archivo (con comentario).
- `MagicNumber` en Composables/estética → `@file:Suppress("MagicNumber")` con justificación, **o**
  mejor: mueve el número a un token.
- **`const val` TOP-LEVEL** (fuera de un `object`) → `SCREAMING_SNAKE_CASE` (`TopLevelPropertyNaming`).
  `@file:Suppress("FunctionNaming")` **no** cubre esto. Miembros de un `object` (p.ej.
  `AppAnimation.DurationFast`) y un `val` no-`const` (`val Foo = 48.dp`) pueden ir PascalCase.
- `CompositionLocal` top-level → conserva `LocalXxx` con `@Suppress("TopLevelPropertyNaming")`.
- Firma amplia estilo Material (>6 params) → `@file:Suppress("LongParameterList")` justificado
  (como `AppTextField`), o agrupa en un `@Immutable data class` de config.
- Delegación `by` sobre estado Compose → importa `androidx.compose.runtime.getValue` (`setValue` si `var`).
- `Modifier.weight`/`align` en un Composable extraído → declara el receptor `ColumnScope.`/`RowScope.`.
- `Modifier.background(if cond Color else Brush)` no resuelve (tipo común `Any`): ramifica el
  `Modifier` con `.then(if (c) Modifier.background(color) else Modifier.background(brush))`.

## Accesibilidad (obligatoria)

- `semantics { contentDescription = … }` en controles; rol correcto; estado (selected/disabled).
- Área táctil ≥ `AppSpacing.MinTouchTarget` (48dp).
- Grupo modal/overlay → `clearAndSetSemantics { liveRegion=Polite; contentDescription=… }` (anuncio
  único), **no** `mergeDescendants`+`contentDescription` (la política de ContentDescription acumula →
  duplica el anuncio).

## Cross-platform

- Componente en `commonMain`; **cero** API Android-only. `expect/actual` solo si es realmente de
  plataforma. Compila y valida en el target **desktop** (`:shared:compileKotlinDesktop`).

## Checklist de aceptación por componente

- [ ] Fiel al original en spacing/radios/sombra/tipografía/estados/curva de motion.
- [ ] Cero `dp`/`Color`/`RoundedCornerShape` literal fuera de archivos de token.
- [ ] Acento vía `favoriteAccentColor(...)`, no color hardcoded.
- [ ] a11y: semantics + `MinTouchTarget`; overlays con `clearAndSetSemantics`.
- [ ] `commonMain`-safe (sin API Android-only).
- [ ] `<Nombre>Samples.kt` con preview de todos los estados.
- [ ] Gates verdes: `:shared:compileKotlinDesktop` + `:shared:detekt` + `:androidApp:compileDebugKotlin`.
- [ ] Se ve **bespoke**, no plantilla Material por defecto.

## Precedentes a copiar (ya portados en este repo)

- `shared/src/commonMain/kotlin/com/mindos/app/design/components/AppTextField.kt` — input Uiverse
  floating-label (`@file:Suppress`, `@Immutable` config, `animate*AsState`+`tween`, `BasicTextField`,
  semantics) + `AppTextFieldSamples.kt`.
- `.../design/components/PhotoUploadLoader.kt` — loader CSS (tokens en SCREAMING_SNAKE, easing
  cúbico) + `AppLoadingOverlay.kt`/`AppBarsLoader.kt` (a11y `clearAndSetSemantics`, tinte por
  `favoriteAccentColor`).

Estos dos son el patrón: cuando dudes, ábrelos y replica su estructura.
