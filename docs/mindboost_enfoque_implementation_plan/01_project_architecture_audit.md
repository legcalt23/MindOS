# 01 · Auditoría de Arquitectura del Proyecto

> Salida de la **Fase 0**. Describe la arquitectura *real* (no idealizada) y
> determina dónde y cómo se integra MindBoost Enfoque sin romper nada.

## 1. Stack y módulos Gradle

| Módulo | Rol | Targets |
|--------|-----|---------|
| `:shared` | Núcleo KMP: dominio, datos, presentación Compose | `commonMain`, `androidMain`, `desktopMain`, `iosMain` |
| `:androidApp` | App Android (entry point, Firebase Crashlytics, Remote Config) | Android |
| `:desktopApp` | App escritorio (JVM/Skiko) — útil para tests UI y dev rápido | JVM |
| `iosApp/` | Proyecto Xcode (consume framework `Shared`) | iOS |
| `mindos/` | **Build Gradle SEPARADO**: `command-router-*`, `mind-action-*`, `mind-security`, `mind-decompose`… sistema de comprensión/ejecución de acciones | KMP (composite build aparte) |
| `functions/` | Backend Cloud Functions (TypeScript, wallet/privacy) | Node |

**Decisión clave:** MindBoost Enfoque es una **feature de la app**, no un
servicio de comprensión de comandos. → Va en **`:shared`**, NO en `mindos/`.
El build `mindos/` (mind-action-*) es ortogonal; no se toca.

### Versiones relevantes (`gradle/libs.versions.toml`)

- Kotlin `2.3.10`, Compose Multiplatform `1.10.3`, AGP `9.1.0`.
- `minSdk 29`, `targetSdk 36`, `compileSdk 36`.
- `kotlinx-coroutines 1.10.2`, `kotlinx-serialization 1.9.0`,
  `kotlinx-datetime 0.6.0` (⚠ conflicto transitivo con 0.7.1 — ver §7).
- Firebase via **gitlive KMP SDK** (`dev.gitlive:firebase-*` 2.4.0): auth,
  firestore, functions, storage.
- Detekt `1.23.7` con baseline ratchet (nuevos findings fallan el build).

## 2. Patrón de arquitectura por feature

Estructura canónica observada (p. ej. `features/profile`, `features/dm`,
`features/social`):

```
features/<feature>/
  domain/
    model/         # data classes puras, sealed, value objects
    repository/    # interfaces (contratos)
    usecase/       # (cuando aplica) lógica de orquestación pura
    audio|media/   # sub-dominios con expect/actual si tocan plataforma
  data/
    Firebase<X>Repository.kt   # impl producción (gitlive Firestore/Storage)
    InMemory<X>Repository.kt   # impl test/preview
    mapper/                    # DTO <-> dominio
  presentation/
    <X>StateHolder.kt          # estado + lógica, mutableStateOf, CoroutineScope
    Remember<X>StateHolder.kt  # factory @Composable
    state/<X>UiState.kt        # sealed/data class de estado de UI
    ui/<X>Screen.kt            # Composable "tonto" que delega en el holder
```

**Es Clean Architecture pragmática + MVVM-sin-ViewModel-Android.** No usan
`androidx.lifecycle.ViewModel`; usan `StateHolder` plano multiplataforma. Esto
es deliberado (KMP) y debemos respetarlo.

### Ejemplo de contrato del StateHolder (real)

```kotlin
class MindOSProfileStateHolder(
    private val profileRepository: MindOSProfileRepository, // interfaz
    private val observerRepository: ProfileObserverRepository,
    private val presenceRepository: ProfilePresenceRepository,
    private val scope: CoroutineScope,                       // inyectado
) {
    var uiState: MindOSProfileUiState by mutableStateOf(MindOSProfileUiState.Loading)
        private set
    suspend fun load(...) { ... }   // transiciones tipadas, testeables con fakes
}
```

## 3. Inyección de dependencias

DI **manual** centralizada en `core/di/AppGraph.kt`:

- `@Stable data class AppGraph(...)` agrupa todas las dependencias root como
  **interfaces** (`DMRepository`, `WalletApiClient`, `MindOSProfileRepository`…).
- `@Composable fun rememberAppGraph()` construye el wiring de producción
  (Firebase) con `remember { }`.
- Los tests construyen `AppGraph(...)` directamente con fakes.
- **Regla del proyecto** (CLAUDE.md, 2026-05-29): toda app con 3+ deps root las
  agrupa en `*Graph`; las deps son interfaces; factory junto al graph.

→ MindBoost añadirá sus repos/servicios al `AppGraph` (o, mejor, un sub-graph
`FocusGraph` anidado — ver §6 y doc 10) para no inflar el constructor raíz.

## 4. Navegación

- `core/navigation/AppRoute.kt`: `sealed interface AppRoute` (Loading, Auth,
  Main, GlobalFeed, Settings, DMList, DMChat, Moneda…).
- `core/navigation/AppRouteStack.kt`: back stack explícito observable
  (`push/pop/replace/replaceAll/popTo`), respaldado por `SnapshotStateList`.
- `core/navigation/PlatformBackHandler.kt`: expect/actual (Android = BackHandler).
- Dentro de `HomeScreen` hay un `enum HomeDestination { Hub, Profile, Search,
  Notifications, Creator… }` que conmuta sub-pantallas (tabs) con
  `mutableStateOf(HomeDestination.X)`.

→ Punto de entrada de MindBoost: **un nuevo `HomeDestination.MindBoost`** (tab/
tarjeta en el Hub) que abre el grafo Enfoque, y/o una `AppRoute.MindBoost(user)`
para entrada profunda. Recomendado: empezar por `HomeDestination` (menos
superficie) y promover a `AppRoute` si se necesita deep-link.

## 5. Persistencia

- **Remota:** Firestore vía gitlive KMP. Repos `Firebase<X>Repository`. Todas las
  operaciones envueltas en timeouts (`core/network/FirestoreDefaults.kt`:
  `withFirestoreReadTimeout`/`WriteTimeout`/`BatchTimeout`).
- **Reglas Firestore** endurecidas (`firestore.rules`): toda `allow list` exige
  `request.query.limit`. Cualquier query cliente debe fijar `.limit(N)`.
- **Local cifrada:** patrón `expect fun rememberPublisherLocalDraftStore(namespace)
  : PublisherLocalDraftStore` con interfaz `suspend load/save/clear`; Android lo
  implementa con SharedPreferences + AES/GCM vía Android Keystore. **Este es el
  patrón a reutilizar para datos cognitivos locales** (baselines, historial).
- No hay Room ni DataStore en `commonMain`. No introducir SQLDelight salvo
  justificación fuerte (ver doc 10 y 14).

## 6. Dónde se integra MindBoost Enfoque

Paquete nuevo, autocontenido:

```
shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/focus/
  domain/        # modelo cognitivo, métricas, adaptación, ejercicios (PURO)
  data/          # repos local-first + Firestore para resúmenes
  presentation/  # StateHolders + Compose
shared/src/{android,desktop,ios}Main/.../features/mindboost/focus/
  # solo expect/actual: timing monotónico de alta resolución, persistencia
```

Wiring:
- `FocusGraph` (sub-graph) construido en `rememberFocusGraph()`, expuesto desde
  `AppGraph` o pasado al `HomeDestination.MindBoost`.
- `HomeScreen` añade un caso `HomeDestination.MindBoost -> FocusHomeScreen(...)`.

**Aislamiento:** igual que la decisión documentada del 2026-05-30 (perfil nuevo
como feature aislada), MindBoost vive aparte y se conecta por **un único punto de
wiring**. Cero cambios en features existentes salvo añadir el destino.

## 7. Riesgos arquitectónicos detectados

| ID | Riesgo | Impacto | Mitigación |
|----|--------|---------|-----------|
| A1 | **Timing/RT en móvil**: la latencia de input (touch→evento→frame) contamina el RT. Compose no garantiza precisión sub-frame. | Métricas RT/RT-CV/SSRT sesgadas. | Usar `kotlin.time.TimeSource.Monotonic` (multiplataforma) para sellar estímulo y respuesta; medir y restar latencia de display; documentar incertidumbre. Ver doc 03 §RT y doc 14. |
| A2 | **kotlinx-datetime 0.7.1** elimina `Clock.System` en runtime de tests. | Tests de tiempo rompen. | Toda lógica de "ahora" pasa por `EpochClock`/`MonotonicTimeSource` inyectado. Nunca `Clock.System.now()` en dominio/presentación. |
| A3 | **Volumen de datos por trial**: una sesión genera cientos de trials. Persistir todo en Firestore es caro y viola reglas de coste. | Coste Firestore, lecturas amplificadas. | Local-first cifrado; a Firestore solo resúmenes de bloque/sesión. Ver doc 10. |
| A4 | **Sobre-arquitectura**: el sistema tiene 13 ejercicios × N ejes; tentación de framework genérico prematuro. | Parálisis, código muerto. | YAGNI: vertical slice E1 primero (doc 12 Fase 5). Abstraer solo tras 2-3 ejercicios reales. |
| A5 | **Detekt baseline ratchet**: código nuevo con findings fuera de baseline falla el build. | CI rojo. | Escribir limpio desde el inicio; `MagicNumber` se tolera, pero `UnusedParameter`/`ComplexMethod` no. |
| A6 | **Reglas Firestore `limit`**: olvidar `.limit()` → PERMISSION_DENIED en prod. | Feature rota silenciosamente. | Toda query de historial fija `.limit(N)`; añadir regla+test espejo. |
| A7 | **Privacidad de datos cognitivos**: perfil atencional es dato sensible (puede inferir TDAH, fatiga, salud). | Legal/ético/confianza. | Local-first, opt-in explícito para sync, nunca compartir, no vanity claims. Ver doc 14. |
| A8 | **expect/actual incompletos**: olvidar un `actual` (iOS) rompe build iOS. | Build iOS roto (no se detecta en Windows). | Checklist: todo `expect` con actual en android/desktop/ios. Stubs documentados en desktop/iOS al inicio. |

## 8. Conclusión

El proyecto tiene una arquitectura **madura, consistente y testeable**. MindBoost
Enfoque encaja como feature aislada en `:shared/features/mindboost/focus`,
reutilizando: patrón StateHolder, DI por graph, persistencia local cifrada,
timeouts, observabilidad e inyección de reloj. **No requiere dependencias nuevas
para el núcleo** (dominio/métricas/adaptación son Kotlin puro). La única pieza
de plataforma nueva es un `MonotonicTimeSource` de alta resolución (cubierto por
`kotlin.time`, sin libs externas).
