# MindBoost · Memoria — Plan Maestro de Implementación por Fases

> **Fuente funcional:** `memoria_sistema_entrenamiento.md` (diseño científico, 16 secciones).
> **Precedente arquitectónico:** `features/mindboost/focus/` (MindBoost · Enfoque), feature KMP
> madura y completa que este plan replica capa por capa.
> **Ubicación del código nuevo:** `shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/memory/`
> **Regla de oro del proyecto:** cada fase es Kotlin puro testeable primero; Compose/plataforma al final.

---

## 0. Decisiones arquitectónicas que condicionan TODO el plan

El sistema de Memoria **no es** un clon de Enfoque. Comparte la arquitectura (domain/data/presentation/di,
persistencia cifrada local, separación training/evaluation, dificultad multidimensional, adaptación por
bloque), pero introduce 4 diferencias estructurales que hay que resolver antes de escribir código:

| # | Diferencia vs. Enfoque | Implicación de diseño |
|---|---|---|
| **D1** | **Recall diferido entre sesiones (24h+)** es métrica primaria (§6, §9, doc §14.2). Enfoque mide RT en el momento; Memoria debe **guardar el material estudiado hoy para testearlo mañana**. | Nueva entidad persistente `DeferredRecallItem` (qué se estudió, cuándo, ventana de test, sin re-estudio). El store cifrado local ya existe (`SecureKeyValueStore`); se reutiliza pero el esquema crece. Hay que manejar "el usuario no abrió la app en 48h" (doc Q13). |
| **D2** | **Contenido como activo de primera clase**: pools de >2000 palabras, >500 rostros, narrativas, pares (§4 repetido "riesgo efecto práctica ALTO"). | Capa `content/` nueva (pools + generación procedural con semilla). Sin pools grandes, el sistema mide familiaridad, no capacidad → viola el principio anti-engaño (§6, §13.4). Decisión bloqueante: **rostros** (IA / fotos con licencia / ilustraciones — doc Q6) y **audio/TTS** (doc Q2). |
| **D3** | **CME = vector de 8 dimensiones** (L·I·D·S·P·R·C·K, §5) vs. los ejes de Enfoque. La adaptación elige **qué dimensión** subir, no un escalar. | `MemoryDifficultyVector` con 8 ejes + fórmula CME. El motor adaptativo prioriza dimensión según patrón de error (§5.3), no sube todo a la vez. |
| **D4** | **Métricas con teoría de detección de señal y metacognición**: d-prime, K de Cowan, calibración confianza-precisión, Brier, gamma de Goodman-Kruskal, ARC clustering (§6). | Motor de métricas más rico que Enfoque. La **confianza** es un overlay transversal a TODOS los ejercicios (Ej. 16 es módulo, no ejercicio). Cada respuesta lleva un juicio de confianza. |

**Decisiones recomendadas para V1** (ajustables; ver §Decisiones al final):
- **Idioma:** español únicamente en V1 (coherente con el resto de la app). Pools en `es`.
- **Rostros:** ilustraciones/avatares generados proceduralmente con atributos controlados (sin fotos reales → evita privacidad/licencias). Suficiente para entrenar binding nombre-rostro.
- **Audio:** TTS de plataforma vía `expect/actual` (Android `TextToSpeech`, iOS `AVSpeechSynthesizer`, desktop stub) + fallback a texto leído. Modalidad auditiva **opcional** por sesión.
- **Material propio del usuario (import):** fuera de V1 (doc Q5). Se diseña el modelo para admitirlo en V2.
- **Persistencia:** local-first cifrada (mismo patrón que Enfoque). Sin backend en V1; sincronización diferida.

---

## 1. Mapa documento → código (las 6 capas del feature)

```
features/mindboost/memory/
  domain/
    model/        # Subcapacidades(12), Módulos(6), ExerciseId(16), CME vector, Stimulus, Response,
                  # ConfidenceLevel, TrialResult, BlockResult, SessionResult, Mode, MemoryUserProfile
    catalog/      # MemoryExerciseCatalog: 16 fichas declarativas (doc §4) — fuente de verdad
    content/      # Pools de estímulos + generadores procedurales con semilla (palabras, pares,
                  # rostros, grids, narrativas, instrucciones, escenas) — NUEVO respecto a Enfoque
    exercise/     # 16 generadores de trials (M1..M16) — uno por ejercicio del catálogo
    metrics/      # MetricKey, SignalDetection (d-prime/criterio/K-Cowan), Clustering(ARC),
                  # Calibration(Brier/gamma), MemoryMetricsEngine
    adaptive/     # CME, zonas de rendimiento, selección de dimensión, señales de alarma (§5)
    session/      # Blueprint, plantillas 5/10/20/30 min, ciclo 8 semanas, descansos, plan diario (§7,§8)
    deferred/     # DeferredRecallScheduler: qué/ cuándo testear entre sesiones (§9) — NUEVO
    evaluation/   # Baseline(§9), ProbeBank no-entrenadas, transfer probes, ReliableChange
    personalization/ # 11 perfiles cognitivos (§12), inferencia de arquetipo, evolución, anti-overtraining
    antiplateau/  # Detección de meseta + 9 protocolos de intervención (§10) — NUEVO
    transfer/     # 10 microprotocolos de transferencia funcional (§11)
    reporting/    # MemoryFitnessScore + 5 índices compuestos (§6), tendencias honestas (§13.14)
  data/
    dto/          # @Serializable DTOs con schemaVersion
    local/        # MemoryJson, LocalMemory*Repository (reusa SecureKeyValueStore), InMemory* (test)
    mapper/       # DTO ↔ domain
  di/
    MemoryGraph.kt + rememberMemoryGraph()  # espejo de FocusGraph
  presentation/
    state/        # MemoryUiState, holders (home, sesión, recall diferido, evaluación)
    ui/           # MemoryRootScreen + pantallas + renderers por ejercicio + componentes
```

**Soporte de plataforma (expect/actual)** — reutilizar lo existente, añadir lo nuevo:
- Reutiliza: `core/time/EpochClock`, `MonotonicClock`, `core/navigation/PlatformBackHandler`,
  `data/local/SecureKeyValueStore` (cifrado AES-GCM ya probado).
- Nuevo expect/actual: `MemoryTextToSpeech` (audio narrativo) en android/desktop/ios.

---

## 2. Las fases, en orden de ejecución

Cada fase: **entregable → archivos clave → tests (TDD primero) → criterio de salida**. El orden respeta el
grafo de dependencias (nada depende de algo no construido). Las fases 1–9 son Kotlin puro 100% testeable
sin device; 10–12 añaden persistencia, UI y wiring.

---

### FASE 0 — Andamiaje y decisiones de contenido
**Objetivo:** dejar resueltas las decisiones bloqueantes (D2) y crear el esqueleto de paquetes.
- Confirmar: idioma `es`, fuente de rostros, estrategia de audio, alcance V1 de módulos (recomendado: M1, M2, M3 primero — WM, Episódica, Asociativa, que cubren las subcapacidades de mayor impacto funcional).
- Crear árbol de paquetes vacío + `docs/mindboost_memoria_implementation_plan/` (este plan + sub-planes por fase si se desea, espejo de Enfoque).
- **Salida:** decisiones firmadas + paquetes creados, compila vacío.

---

### FASE 1 — Modelo de dominio (`domain/model/`)
**Objetivo:** el vocabulario inmutable del sistema. Sin lógica, sin Compose.
- Enums: `MemorySubcapacity` (12, doc §2), `MemoryModule` (6, doc §3), `MemoryExerciseId` (16, doc §4),
  `MemoryParadigm` (span complejo, pares asociados, DRM, Corsi, change-detection, RSVP…), `Mode` (TRAINING/EVALUATION/CALIBRATION), `TrialOutcome` (HIT/MISS/FALSE_ALARM/INTRUSION/CONJUNCTION_ERROR/BINDING_ERROR/OMISSION…).
- Value objects: `Proportion`, `ReactionTimeMs`, `ConfidenceLevel` (escala 2/4/6 niveles, doc §16),
  `AxisLevel`, `SerialPosition`.
- `MemoryDifficultyVector` con los 8 ejes (L,I,D,S,P,R,C,K) + función `cme()` que implementa la fórmula del doc §5.
- Resultados: `MemoryTrialResult`, `MemoryBlockResult`, `MemorySessionResult`, `MemoryUserProfile`.
- **Tests:** validación de value objects (rangos), cálculo CME contra ejemplos del doc, exhaustividad de enums.
- **Salida:** `:shared:desktopTest` verde para el modelo; detekt limpio (suprimir MagicNumber idiomático).

> ⚠️ Aprendizaje aplicable: usar `EpochClock`/`AppClock` inyectado, nunca `Clock.System.now()` directo
> (CLAUDE.md 2026-05-29). DTOs con `schemaVersion` (2026-06-05).

---

### FASE 2 — Catálogo de ejercicios (`domain/catalog/`)
**Objetivo:** las 16 fichas declarativas del doc §4 como única fuente de verdad (igual que `ExerciseCatalog` de Enfoque).
- `MemoryExerciseDefinition` (id, subcapacidad, módulo, paradigma, ejes relevantes, eje gobernante, métrica gobernante, métricas primarias, versiones principiante/avanzado/élite, riesgo de efecto práctica).
- `MemoryExerciseCatalog` con M1..M16: Span Complejo, Actualización de Set, Lista con Estrategia, Pares Asociados, Grid Espacial, Detección de Cambio, Secuencias, Nombres y Rostros, Instrucciones Complejas, Prospectiva Embebida, Listas con Interferencia, Discriminación Fina, Reconstrucción Parcial, Dual-Task Recall, Memoria Auditiva Narrativa, Calibración de Confianza (esta última marcada como **overlay transversal**, no ejercicio standalone).
- `implemented: List<MemoryExerciseId>` arranca con el subconjunto V1.
- **Tests:** catálogo exhaustivo (16 entradas), cada `relevantAxes` ⊆ ejes válidos, mapeo módulo↔subcapacidad coherente con la tabla maestra (doc §15).
- **Salida:** catálogo consultable por las demás capas.

---

### FASE 3 — Contenido y generación procedural (`domain/content/`)  ⟵ *capa nueva, crítica*
**Objetivo:** garantizar material **nuevo** en cada trial (principio anti-engaño, doc §13.4). Sin esto, el sistema mide práctica, no memoria.
- Pools versionados por idioma: `WordPool` (concretas/abstractas, alta/baja frecuencia, categorías), `PairPool`, `FacePool` (atributos: etnia/edad/género/rasgos → render procedural), `NarrativePool` (historias/noticias/instrucciones), `Scene/GridContent`, `NameLists`.
- `StimulusGenerator` con **semilla determinista** (reproducible para tests; nunca repite set al usuario) que controla similitud (S), interferencia/solapamiento categorial (I), concretud, fan effect.
- `TransferProbeContent`: subconjunto **reservado** que nunca se usa en entrenamiento (doc §9.3).
- **Tests:** dada semilla → set reproducible; no-repetición entre N sesiones consecutivas; control de similitud verificable; pools por encima del umbral mínimo de tamaño.
- **Salida:** generadores que alimentan a los de ejercicio.

> Decisión D2 se materializa aquí. Rostros e idioma definidos en Fase 0.

---

### FASE 4 — Generadores de trials por ejercicio (`domain/exercise/`)
**Objetivo:** un generador por ejercicio del catálogo que produce trials parametrizados por el `MemoryDifficultyVector`. Espejo de `E1VigiaGenerator..E13` de Enfoque.
- `TrialGenerator` interfaz común + `M1SpanComplejo`, `M2Actualizacion`, `M3ListaEstrategia`, `M4Pares`, `M5Grid`, `M6Cambio`, `M7Secuencias`, `M8NombresRostros`, `M9Instrucciones`, `M10Prospectiva`, `M11Interferencia`, `M12Discriminacion`, `M13Reconstruccion`, `M14DualTask`, `M15Narrativa`.
- Cada generador respeta sus "Variables de dificultad" y versiones principiante/avanzado/élite del doc §4.
- Implementar en orden de impacto V1: **M1, M3, M4** primero; luego M5, M6, M8; resto por oleadas.
- **Tests:** por generador — el trial generado cumple el nivel de cada eje; targets/lures/foils correctos en discriminación; fan effect controlado en pares; posiciones únicas en grid.
- **Salida:** trials válidos consumibles por el motor de métricas.

---

### FASE 5 — Motor de métricas (`domain/metrics/`)
**Objetivo:** convertir respuestas en las métricas científicas del doc §6.
- `SignalDetection`: d-prime = Z(hit) − Z(FA), criterio c, **K de Cowan**, hit/FA rate.
- `Clustering`: índice ARC (clustering categorial), clustering subjetivo, intrusiones extra-lista.
- `Calibration`: alineamiento confianza-precisión, **Brier score**, **gamma de Goodman-Kruskal**, sesgo over/under-confidence, resolución.
- `RetentionMetrics`: ratio diferido/inmediato, tasa de olvido (ajuste exponencial), curva posición serial.
- `MemoryMetricsEngine.blockMetrics()` / `sessionMetrics()` agregando todo.
- **Tests:** d-prime contra valores conocidos; ARC con listas categorizadas; calibración perfecta vs. sesgada; **aplicar aprendizaje 2026-06-04**: las métricas de tendencia central filtran outliers, pero conteos de eventos extremos (intrusiones, lapsos) se cuentan sobre crudos.
- **Salida:** métricas fiables para adaptación y reporting.

---

### FASE 6 — Motor adaptativo (`domain/adaptive/`)
**Objetivo:** mantener al usuario en la zona de dificultad deseable ajustando el **vector** CME (doc §5).
- `PerformanceScore` ponderado (doc §5 fórmula), `Zone` (frustración…demasiado fácil) con umbrales del doc.
- `DimensionSelector`: elige qué eje (L/I/D/S/P/R/C/K) ajustar según patrón de error (precisión alta + diferida baja → subir D; + intrusiones → subir I; etc.).
- `AlarmSignals`: fatiga, frustración, aburrimiento, impulsividad, estancamiento, falsa mejora (doc §5.4).
- Tres escalas temporales: intra-sesión / inter-sesión / inter-ciclo.
- **Tests:** mapeo zona→acción; selección de dimensión por escenario; disparo de alarmas; ajuste tras bloque (no tras trial, evita oscilación).
- **Salida:** progresión adaptativa por dimensión.

---

### FASE 7 — Sesiones y periodización (`domain/session/` + `domain/deferred/`)
**Objetivo:** construir sesiones (doc §7) y el ciclo de 8 semanas (doc §8); programar el recall diferido entre sesiones (D1).
- `SessionTemplate` para 5/10/20/30 min con su estructura de bloques (calentamiento→codificación→recuperación inmediata→interferencia→recuperación diferida→transferencia→cooldown).
- `MemorySessionAssembler` que ensambla bloques desde catálogo+generadores+plantilla.
- `TrainingCycle` (8 semanas, semanas de descarga 4 y 8, progresión entre ciclos sin reinicio).
- `RecoveryPolicy` (descanso intra-sesión, mín. 4h entre sesiones del mismo módulo, día de descanso semanal).
- **`DeferredRecallScheduler`** (nuevo, D1): registra `DeferredRecallItem` al codificar; al abrir la app decide qué material de sesiones previas testear; maneja ventana 24h y el caso "no abrió en 48h" (marca el dato como retención extendida o lo descarta — doc Q13).
- **Tests:** estructura de cada plantilla; descargas en semanas 4/8; scheduler entrega ítems correctos por ventana temporal; política de descanso.
- **Salida:** sesiones completas ensamblables + memoria entre sesiones.

---

### FASE 8 — Evaluación, baseline y transferencia (`domain/evaluation/` + `domain/transfer/`)
**Objetivo:** medir mejora **genuina** con material no entrenado y recall diferido (doc §9, §11, §13).
- `BaselineProtocol` (sesión 1 ~25 min, tabla doc §9) por subcapacidad.
- `ProbeBank` de pruebas **no entrenadas** (transfer probes) — usa contenido reservado de Fase 3.
- `ReliableChangeIndex` / threshold de "mejora real" (doc Q12: tamaño de efecto ≥0.3 o mejora sostenida 2 semanas en prueba no entrenada).
- `TransferProtocols`: los 10 microprotocolos funcionales del doc §11 (estudio, lectura, idiomas, networking, reuniones, instrucciones, presentaciones, decisiones, pendientes, anti-distracción).
- **Tests:** baseline cubre las subcapacidades de la tabla; probe nunca solapa con material de entrenamiento; RCI detecta cambio fiable vs. ruido.
- **Salida:** capacidad de distinguir mejora real de efecto práctica.

---

### FASE 9 — Personalización, anti-estancamiento y reporting (`personalization/`, `antiplateau/`, `reporting/`)
**Objetivo:** adaptar al perfil del usuario, romper mesetas y reportar con honestidad.
- `MemoryArchetype` (11 perfiles, doc §12) + `ArchetypeInference` desde métricas baseline (olvida rápido, confunde similares, over/under-confident, lento-preciso, etc.).
- `ProfileEvolution` + `OvertrainingGuard` (reusar patrón de Enfoque).
- `PlateauDetector` + los **9 protocolos** de intervención (doc §10) + regla de oro (si nada funciona en 2 semanas, redirigir tiempo).
- `MemoryFitnessScore` + 5 índices compuestos (Learning Velocity, Retention Quality, Resilience, Calibration Quality) + `Trends` que **muestran variabilidad honesta** (doc §13.14), reportan **por subcapacidad** (doc §13.15), distinguen velocidad de capacidad (doc §13.16).
- **Tests:** inferencia de arquetipo por escenario; detección de meseta con 7 días de datos; índices compuestos; reporting nunca afirma mejora basada solo en material entrenado/inmediato (principio anti-engaño §6).
- **Salida:** lógica de negocio completa, 100% testeable, sin UI.

---

### FASE 10 — Persistencia local cifrada (`data/`)
**Objetivo:** persistir perfil, baseline, historial, ítems de recall diferido y estado de ciclo — cifrado.
- DTOs `@Serializable` con `schemaVersion`; `MemoryJson { ignoreUnknownKeys=true; encodeDefaults=true }`.
- `LocalMemoryProfileRepository`, `LocalSessionHistoryRepository`, `LocalDeferredRecallRepository`, `LocalCycleStateRepository` sobre `SecureKeyValueStore` (reutilizado).
- `InMemory*` para tests; mappers DTO↔domain.
- **Aplicar aprendizajes:** solo persistir **resúmenes** de sesión, no trials crudos de alto volumen (2026-06-05); verificar exclusión de backups (`backup_rules.xml` ya cubre `sharedpref`); nunca inventar cripto.
- **Tests:** round-trip DTO↔domain con `InMemorySecureKeyValueStore`; tolerancia a `schemaVersion` antiguo; recall diferido sobrevive reinicio.
- **Salida:** estado persistente y seguro.

---

### FASE 11 — DI (`di/MemoryGraph.kt`)
**Objetivo:** un único punto de wiring del feature (espejo exacto de `FocusGraph`).
- `@Stable data class MemoryGraph(...)` con engines, assembler, scheduler, repos, clocks (interfaces, no concretos).
- `rememberMemoryGraph()` construye el wiring de producción; namespace `mindboost_memory` para el store.
- **Tests:** `MemoryGraphTest` construye el grafo con fakes (sin `rememberMemoryGraph`).
- **Salida:** feature inyectable de una sola dependencia.

---

### FASE 12 — Presentación y wiring en boostMind (`presentation/` + HomeScreen)
**Objetivo:** UI Compose Multiplatform y enganche en la app. Última fase (depende de todo).
- State holders: `MemoryHomeStateHolder`, `MemorySessionStateHolder`, `DeferredRecallStateHolder`, `EvaluationStateHolder` (patrón de Enfoque; tests con `cancelChildren()`/`backgroundScope`, aprendizaje 2026-05-29).
- Pantallas: `MemoryRootScreen` (nav interna home⇄sesión⇄recall diferido⇄evaluación + `PlatformBackHandler`), `MemoryHomeScreen`, `MemorySessionScreen`, renderers por ejercicio, `MemorySummaryScreen`, overlay de confianza transversal.
- Tokens visuales SIEMPRE en `design/AppColors.kt` etc. (aprendizaje 2026-05-27); nada de literales locales.
- **Audio:** `MemoryTextToSpeech` expect/actual (Android/desktop/ios) para narrativa.
- **Wiring boostMind:** hoy `HomeDestination.MindBoost → FocusRootScreen`. Introducir un **hub MindBoost** (selector Enfoque / Memoria) y montar `MemoryRootScreen` en el nuevo destino. Punto único de cambio en `HomeScreen.kt` (líneas ~286–295).
- **Tests:** smoke de navegación (desktopTest, como `MindOSAppNavigationSmokeTest`); holders con flows infinitos.
- **Salida:** feature Memoria usable end-to-end en boostMind.

---

## 3. Orden resumido (ruta crítica)

```
0 Andamiaje/decisiones
1 Modelo ─► 2 Catálogo ─► 3 Contenido ─┬─► 4 Generadores ─► 5 Métricas ─► 6 Adaptativo
                                       │
                                       └─► (3 alimenta también 8 transfer probes)
6 ─► 7 Sesiones+Recall diferido ─► 8 Evaluación+Transferencia ─► 9 Personalización/Anti-meseta/Reporting
9 ─► 10 Persistencia ─► 11 DI ─► 12 Presentación + wiring boostMind
```
Fases 1–9 sin device (KMP puro, TDD). 10–12 añaden plataforma. Dentro de 4 y 12 se entrega por oleadas de ejercicios (V1: M1/M3/M4 → +M5/M6/M8 → resto).

---

## 4. Estrategia de calidad (transversal a todas las fases)
- **TDD obligatorio** (regla del repo): test primero, 80%+ cobertura con Kover.
- **Detekt ratchet:** para feature grande no regenerar baseline a ciegas — arreglar UnusedParameter/UnusedPrivateProperty siempre; `@Suppress` idiomático en Composables/tablas estadísticas (aprendizaje 2026-06-04).
- **kotlinx-serialization-json** ya disponible (aprendizaje 2026-06-05) para la capa data.
- **Build:** `:shared:desktopTest` como bucle rápido (no requiere SDK Android ni JDK 21); detekt + assembleDebug antes de merge.
- Rama por tarea (Vibe Kanban), nunca sobre `main`.

---

## 5. Decisiones de alcance

**Confirmadas (2026-06-06):**
1. **Alcance V1 de módulos:** **M1 (Span Complejo, WM) + M3 (Lista con Estrategia, Episódica) + M4 (Pares Asociados, Asociativa)**. Es la primera oleada de las Fases 4 y 12; valida el pipeline completo end-to-end con la menor superficie. Resto de ejercicios por oleadas posteriores.
2. **Rostros (Ej. 8):** **ilustraciones/avatares procedurales** con atributos controlados (etnia/edad/género/rasgos). Sin fotos reales → evita privacidad/licencias. (Aplica cuando se construya M8, oleada 2.)
3. **Audio (Ej. 15):** **TTS de plataforma + fallback a texto**, vía `MemoryTextToSpeech` expect/actual (Android `TextToSpeech`, iOS `AVSpeechSynthesizer`, desktop stub). Modalidad auditiva opcional por sesión. (Aplica cuando se construya M15, oleada posterior.)

**Pendientes (no bloquean V1):**
4. **Import de material propio (Perfil i):** diferido a V2. El modelo de dominio se diseña para admitirlo (Fase 1/3) pero sin UI en V1.
5. **Frecuencia objetivo de sesiones** (doc Q1): 4–5/sem (ideal) vs. 2–3/sem (real) — afecta la calendarización del recall diferido (Fase 7) y la duración del ciclo (Fase 8). Asumir 3–4/sem por defecto y parametrizar.

---

> Este plan es el índice maestro. Si se desea, se pueden expandir sub-planes por fase (02..12) replicando
> el set de `docs/mindboost_enfoque_implementation_plan/`. La Fase 1 es el siguiente paso ejecutable en
> cuanto se confirmen las decisiones de §5.
