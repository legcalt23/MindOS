# 09 · Plan de Integración UI / UX

> **Fase 5 (mínima) → 10 (reportes).** La UI llega DESPUÉS del dominio, métricas
> y adaptación. Sigue el patrón del repo: `StateHolder` + `rememberXStateHolder()`
> + Composable "tonto". Integración por **un único punto de wiring** en
> `HomeDestination`. Honestidad científica: incertidumbre visible, cero
> pseudociencia.

## 0. Integración con la navegación existente

**Punto de entrada (mínima superficie):** añadir un caso al enum interno
`HomeDestination` de `HomeScreen.kt` y una tarjeta en el Hub.

```kotlin
// dentro de features/home/presentation/HomeScreen.kt
enum class HomeDestination { Hub, Profile, Search, Notifications, Creator,
    MindBoost /* NUEVO */ }

// en el when(destination):
HomeDestination.MindBoost -> FocusHomeScreen(
    graph = focusGraph,                 // del AppGraph / FocusGraph
    user = user,
    onBack = { destination = HomeDestination.Hub },
)
```

Navegación interna de MindBoost: **stack propio ligero** dentro de la feature
(las pantallas de ejercicio son efímeras y de pantalla completa). Para entrada
profunda/deep-link futura, promover a `AppRoute.MindBoost(user)` + `AppRouteStack`
(no necesario en Fase 5).

**Back handling:** durante un ejercicio activo, interceptar back con
`PlatformBackHandler(enabled = exerciseInProgress, onBack = { confirmAbort() })`
para no perder un bloque por un gesto accidental.

## 1. StateHolders (patrón del repo)

```kotlin
// presentation/FocusHomeStateHolder.kt
class FocusHomeStateHolder(
    private val planGenerator: DailyPlanGenerator,
    private val profileRepo: FocusProfileRepository,
    private val historyRepo: SessionHistoryRepository,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
) {
    var uiState: FocusHomeUiState by mutableStateOf(FocusHomeUiState.Loading)
        private set
    suspend fun load(userId: String) { ... }
    fun startSession(template: SessionTemplate) { ... }
}

@Composable
fun rememberFocusHomeStateHolder(graph: FocusGraph, user: AuthenticatedUser):
    FocusHomeStateHolder { /* remember + scope, igual que rememberMindOSProfileStateHolder */ }
```

```kotlin
// presentation/SessionRunnerStateHolder.kt  — orquesta una sesión completa
class SessionRunnerStateHolder(
    private val runtime: ExerciseRuntime,
    private val metrics: MetricsEngine,
    private val adaptive: AdaptiveEngine,
    private val historyRepo: SessionHistoryRepository,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
) {
    var uiState: SessionUiState by mutableStateOf(SessionUiState.Warmup)
        private set
    // recibe input del renderer, sella tiempos, agrega bloque, llama a adaptive (si TRAINING)
}
```

## 2. Mapa de pantallas

| Pantalla | Fase | Propósito |
|----------|------|-----------|
| `FocusHomeScreen` | 5 | Hub MindBoost: plan del día, elegir duración (5/10/20/30), CTA "Entrenar", acceso a baseline/progreso. |
| `BaselineIntroScreen` + `StateQuestionnaireScreen` | 4/5 | Onboarding de baseline + cuestionario de estado. |
| `ExerciseScreen` (host) | 5 | Pantalla full-screen que monta el `renderer` del ejercicio activo y feed de input al runtime. |
| `ExerciseRenderer` (por ejercicio) | 5,6,9 | Render concreto (E1 primero). Tonto: dibuja `Stimulus`, reporta `Response`. |
| `SessionSummaryScreen` | 5 | Resumen honesto post-sesión: qué pasó, sin sobre-claim. |
| `ProgressScreen` | 10 | Tendencias, Focus Score con banda de incertidumbre, retests. |
| `ExerciseExplainerSheet` | 9 | "Qué entrena este ejercicio" (plainDescription honesto). |

## 3. UI States (sealed, como en el repo)

```kotlin
sealed interface FocusHomeUiState {
    data object Loading : FocusHomeUiState
    data class Ready(
        val plan: DailyFocusPlan,
        val profileSummary: ProfileSummaryUi,
        val needsBaseline: Boolean,
    ) : FocusHomeUiState
    data class Error(val message: String) : FocusHomeUiState
}

sealed interface SessionUiState {
    data object Warmup : SessionUiState
    data class TrialActive(val render: StimulusRenderModel, val phase: SessionPhase) : SessionUiState
    data class BlockBreak(val nextIn: Int) : SessionUiState
    data class MicroPause(val reason: String) : SessionUiState    // cooldown del adaptive
    data class Finished(val result: SessionResult) : SessionUiState
    data class Aborted(val partial: SessionResult?) : SessionUiState
}
```

## 4. Componentes reutilizables

- Reutilizar `design/` (AppColors, AppSpacing, AppShapes…). Añadir tokens
  MindBoost a `AppColors.kt` (no crear tokens locales — regla del repo 2026-05-27).
- Reutilizar `design/components/StateComponents.kt` (Loading/Error/Empty).
- Nuevos componentes propios: `StimulusCanvas`, `ResponseButtons`,
  `FocusGauge` (medidor con banda de incertidumbre), `UncertaintyBadge`.
- **Animaciones:** por defecto NINGUNA decorativa; solo las funcionales del
  estímulo (RSVP, MOT). (Memoria del usuario: animaciones solo on-demand.)

## 5. Mostrar métricas sin pseudociencia (reglas de UX honesto)

1. **Toda métrica/índice mostrado lleva su incertidumbre.** Si `confident=false`,
   no se muestra como número duro: "datos insuficientes aún" o banda ancha.
2. **Distinguir hecho vs estimación:** "Subiste de nivel" (hecho) vs "tendencia
   preliminar" (estimación sin RCI).
3. **Nunca afirmar transferencia lejana** (CI, inteligencia, "más listo").
4. **Probes = la única evidencia fuerte:** la pantalla de progreso destaca el
   cambio en probes no entrenados por encima del progreso intra-ejercicio.
5. **Lenguaje no diagnóstico** (doc 08 §5): describir estado, no etiquetar salud.
6. **Reportar el mal día:** si el estado (sueño/hora) explica una caída, decirlo,
   no penalizar la percepción de progreso.

```kotlin
data class FocusGaugeUi(
    val label: String,
    val value: Double?,            // null si no fiable
    val band: ClosedRange<Double>?, // banda de incertidumbre
    val confident: Boolean,
    val honestCaption: String,     // p. ej. "estimación preliminar — 2 de 3 retests"
)
```

## 6. Explicar qué entrena cada ejercicio

`ExerciseExplainerSheet` usa `ExerciseDefinition.plainDescription` + la capacidad
en lenguaje llano: "Vigía entrena tu capacidad de mantener la atención cuando
casi no pasa nada — como vigilar un proceso largo. Mide cuántos lapsos tienes y
cuán estable es tu tiempo de reacción." Sin jerga, sin promesas.

## 7. Verificación de UI (preview tools del repo)

El `:desktopApp` permite ejercitar la UI sin device. Para Fase 5, verificar en
desktop: flujo baseline → E1 → resumen. (El timing de RT real se valida en device
Android; desktop sirve para flujo/estado, no para precisión de RT.)

## 8. Criterios de aceptación (UI)

- [ ] Integración por un solo `HomeDestination.MindBoost` (cero cambios en otras features).
- [ ] StateHolders testeables con fakes (patrón `rememberX` + scope inyectado).
- [ ] Composables "tontos"; lógica en holders.
- [ ] Toda métrica mostrada respeta `confident`/banda de incertidumbre.
- [ ] Tokens en `design/AppColors.kt`, no locales.
- [ ] Back durante ejercicio confirma antes de abortar.
- [ ] Cero afirmaciones de transferencia lejana en strings.
