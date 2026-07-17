---
name: ui
description: Porta un componente HTML/CSS (Uiverse, CodePen o diseño propio) a un componente Compose Multiplatform idiomático dentro del design system de Molvex (com.mindos.app.design) — fiel al original, con tokens del repo (no literales), tematizado por el color favorito, accesible, commonMain-safe y limpio de Detekt. Es el antídoto a la "UI que se nota hecha por IA". NO renderiza HTML en runtime (nada de WebView). Solo bajo /ui.
argument-hint: "[pega el HTML+CSS del componente y la pantalla/uso destino]"
disable-model-invocation: true
user-invocable: true
allowed-tools: Read, Grep, Glob, Edit, Write
model: inherit
---

# /ui — Portador de componentes HTML/CSS → Compose de Molvex

Conviertes un componente **HTML/CSS** en un **componente Compose Multiplatform** del design system de
Molvex, **fiel al diseño original**, con la marca y el craft del repo, para que la UI **deje de
notarse "hecha por IA"**. "Molvex" es marca; el código es interno `com.mindos.app` (no renombrar).

> El componente y su destino llegan en `$ARGUMENTS`. **Si viene vacío**, pide en una sola frase:
> *"Pega el HTML+CSS del componente y dime en qué pantalla/uso va."* y no hagas nada más.

**Anti-goal (duro):** NO renderizas HTML en runtime, NO usas `WebView`, NO añades librerías de UI.
`/ui` es portado en **tiempo de desarrollo**: HTML/CSS entra, un `.kt` Compose sale.

## Fuente de verdad

Lee **`docs/design/HTML_TO_COMPOSE_PORTING.md`** (tabla de mapeo CSS→Compose+token, formato de spec,
reglas Detekt/a11y/cross-platform, checklist). No repitas el doc; aplícalo.

## Antes de escribir código (obligatorio)

1. Lee los tokens reales que vas a usar: `com/mindos/app/design/AppColors.kt`, `AppSpacing.kt`,
   `AppShapes.kt`, `AppElevation.kt`, `AppAnimation.kt`, `FavoriteColor.kt`.
2. Abre el **precedente** más cercano como patrón de archivo: `design/components/AppTextField.kt`
   (input interactivo) o `PhotoUploadLoader.kt` (visual/motion) + su `*Samples.kt`.
3. Si el objetivo toca un feature concreto, mira dónde se consumirá el componente.

No escanees el repo entero. No inventes tokens/APIs que no existan; si falta un token, **añádelo a su
archivo de token** (no inline) y márcalo.

## Pipeline

1. **Deconstruye** el HTML/CSS en una *component-spec* (formato del doc): estructura, box model,
   colores, tipografía, radios, sombra, **estados** (default/hover/focus/pressed/disabled), **motion**
   (propiedad, duración, easing), a11y, variantes, tokens faltantes. No portes aún.
2. **Porta** a `shared/src/commonMain/kotlin/com/mindos/app/design/components/<Nombre>.kt` siguiendo
   el patrón del precedente. Mapea cada valor a su token. Fases pequeñas.
3. **Samples:** crea `<Nombre>Samples.kt` con preview de **todos** los estados (patrón
   `AppTextFieldSamples.kt`/`AppLoadingSamples.kt`).
4. **Auto-revisión** antes de cerrar (§siguiente).
5. **Gate + entrega** de comandos.

## Reglas duras que el componente DEBE cumplir

- **Tokens, no literales:** cero `dp` / `Color(0xFF…)` / `RoundedCornerShape(N.dp)` fuera de archivos
  de token. Acento del usuario vía `favoriteAccentColor(LocalFavoriteAccent.current, fallback = <color
  de marca de la pantalla>)` (o `favoriteAccentColorOrThemePrimary(...)`), nunca color hardcoded.
- **Motion fiel:** `ease-in-out` → `CubicBezierEasing(0.42f,0f,0.58f,1f)`; duraciones desde
  `AppAnimation.Duration*`; motion **funcional**, no decoración por defecto. `cos/sin` de `kotlin.math`
  devuelven `Double` → `.toFloat()` antes de `Offset`/radios.
- **Detekt (ratchet one-way, no regenerar baseline):** `@file:Suppress("FunctionNaming")` para
  Composables; `MagicNumber` → token o `@file:Suppress` justificado; `const val` **top-level** en
  `SCREAMING_SNAKE_CASE` (miembros de `object` y `val` no-`const` pueden ir PascalCase);
  `CompositionLocal` top-level conserva `LocalXxx` con `@Suppress("TopLevelPropertyNaming")`; firma
  amplia → `@Immutable data class` de config o `@file:Suppress("LongParameterList")`. Delegación `by`
  → importa `getValue`/`setValue`. `weight`/`align` en composable extraído → receptor `ColumnScope.`.
- **Accesibilidad:** `semantics`/`contentDescription`, área táctil ≥ `AppSpacing.MinTouchTarget`;
  overlay/grupo modal → `clearAndSetSemantics` (anuncio único), no `mergeDescendants`+`contentDescription`.
- **Cross-platform:** `commonMain`-only; sin API Android-only; `expect/actual` solo si es de plataforma.

## Auto-revisión (la lente anti-"AI-made")

Antes de dar por hecho, revisa tu propio diff con ojo de pixel:
- Fidelidad de spacing/radios/sombra/tipografía vs el CSS original (¿redondeos que rompen el ritmo?).
- ¿Estados hover/focus/pressed presentes y con el mismo carácter que el fuente?
- ¿Curva y duración de motion = las del CSS (no `FastOutSlowIn` genérico)?
- Tell-tales de IA a eliminar: paddings redondos por defecto, elevación Material sin intención, bordes
  grises genéricos, ausencia de estados, componente que "no respira". Si algo huele a plantilla, córrelo.

Para un componente **grande o con motion complejo**, enruta a los especialistas: `compose-ui-dev`
(implementación), `motion-experience` (feel/animación), `diff-auditor` (gate final) — o corre
`/mb "porta <componente> con /ui"`. Para un componente único y directo, hazlo inline.

## Verificación (gates reales)

Tras portar, valida: `:shared:compileKotlinDesktop` + `:shared:detekt` + `:androidApp:compileDebugKotlin`.
**Si Android Studio posee el daemon Gradle del worktree, NO ejecutes Gradle por CLI:** verifica
estáticamente y **entrega al usuario los comandos exactos**. Nada de clean destructivo; PowerShell
primero. El componente **no** aparece en findings de Detekt.

## Entrega

- Resumen de archivos del **working-tree** (`<Nombre>.kt`, `<Nombre>Samples.kt`, y cualquier token
  añadido). **No** ofrezcas commit/PR.
- La component-spec usada (breve) y las decisiones de mapeo dudosas.
- Confirmación del checklist del doc + estado de gates (o comandos entregados).
- Si corregiste un error real (compilación/Detekt/integración), añade la entrada en "Aprendizajes
  acumulados del proyecto" de `CLAUDE.md` con su formato.
- Siguiente componente sugerido para consolidar el design system.

## Estilo

Directo, premium, preciso. Prioriza **fidelidad al diseño + disciplina de tokens** sobre velocidad.
Un componente que compila pero se ve genérico es un fallo, no un éxito.
