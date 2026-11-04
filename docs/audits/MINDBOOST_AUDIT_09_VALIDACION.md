# 9. Plan de validacion cientifica

# 9. Plan de validación científica

> **Postura previa (no negociable).** Nada de lo que sigue es "medir si el usuario mejora". Es **medir si el ejercicio mide algo, y después si ese algo cambia por razones distintas a la familiaridad**. En el estado actual del repo la mayor parte del aparato de validación **existe, está testeado y NO se ejecuta**: `RealImprovementCriterion` e `ImprovementEvidence` tienen **cero llamadores de producción** (solo su propio archivo y su test), `MemoryProbeBank` nunca se administra (`docs/audits/modules/memory.md:149-152`), y `ProbeReliabilityEstimator` usa `ReliabilitySource.EMPTY` por defecto (`focus/domain/evaluation/ProbeReliabilityEstimator.kt:14-19`), lo que fuerza `RciResult.NotEnoughData` **siempre** en producción. El producto **afirma** honestidad y **no ejecuta** el mecanismo que la sostendría. [Riesgo científico]

---

## 9.0 Gate V0 — validez de constructo: precondición, no fase

Ningún ejercicio entra en un experimento de eficacia antes de pasar V0. Un A/B sobre una métrica que no es función de la conducta del usuario produce un ganador estadísticamente significativo y científicamente vacío.

**Test de falsificación sintética (ejecutable HOY, en `desktopTest`, sin usuarios ni backend).** Para cada ejercicio, tres respondedores sintéticos:

| Respondedor | Conducta | Aserción obligatoria |
|---|---|---|
| `NULL` | no responde nunca | métrica gobernante = ausente (`null`/`Absent`), **jamás** un valor |
| `RANDOM` | responde al azar dentro de ventana | métrica ≈ nivel de azar, con IC que **contiene** el azar |
| `EXPERT` | responde según la regla óptima del paradigma | métrica > `RANDOM` con separación ≥ 1.0 SD y sin solape de IC95 |

Si `NULL`, `RANDOM` y `EXPERT` producen la **misma** métrica, el número no depende de la conducta. Ese ejercicio no es medible: se elimina o se le ocultan los números (§9.8, C0). El patrón ya existe en el repo (`core/antigaming/` + suite red-team) y la disciplina anti-vacuidad está documentada (CLAUDE.md 2026-07-20: envolver cada operando en un guard que **falle** ante ausencia, o `absent > absent` pasa trivialmente).

**Fallos V0 ya identificados por las auditorías, sin necesidad de experimento:** Enfoque E8/E9/E10 (métricas no son función de la conducta — `docs/audits/modules/focus.md`, riesgo sistémico 1), Velocidad `omissionRate` (siempre 0, sin canal de omisión), Memoria `ARC_CLUSTERING` (orden de recuperación artefactual) y `DELAYED_RECALL_ACCURACY` (etiqueta de demora sin demora), Lógica `ReasoningScorer.kt:48-51` (bug de precedencia que anula la penalización conceptual). **Estos no se validan: se arreglan o se retiran.** [Mejora crítica]

---

## 9.1 Jerarquía de evidencia y techo de claim

| Nivel | Qué prueba | Evidencia requerida | Claim permitido |
|---|---|---|---|
| **E0** Validez | la métrica depende de la conducta | V0 sintético | "esto mide X" |
| **E1** Fiabilidad | la métrica es estable | test-retest r ≥ 0.60 sobre ≥2 administraciones sin entrenar | "es comparable en el tiempo" |
| **E2** Mejora en la tarea | sube el rendimiento en el material entrenado | serie longitudinal intra-sujeto | **"practicaste"** — nunca "mejoraste" |
| **E3** Transferencia de **contenido** | sube en material reservado nunca entrenado | probe reservado (`TransferProbePartition`, `ProbeBank`) + RCI | "progreso frente a tu propia línea base" |
| **E4** Transferencia de **paradigma** | sube en otra tarea de la misma capacidad | probe `DIFFERENT_PARADIGM` — **diferido** (`memory/domain/evaluation/MemoryProbe.kt:17-20`) | ninguno hoy |
| **E5** Transferencia funcional | cambia una conducta fuera de la app | estudio con medida externa | **prohibido por charter** |

**Techo real hoy: E3, y solo de contenido.** Todos los probes existentes son `SAME_PARADIGM_RESERVED`. Un usuario que mejora en el probe reservado ha mejorado en *el mismo paradigma con palabras nuevas*, no en "atención". Cualquier copy que sugiera E4 sin probes E4 es un overclaim. [Riesgo científico]

---

## 9.2 Métricas internas

**Por ejercicio (una sola métrica gobernante pre-registrada; las demás son secundarias y no pueden ascender a primaria después de ver datos):**

- Métrica gobernante ya declarada por catálogo (`governingMetric`), **más**: `n` efectivo, flag `confident`, y verdicto de `MinSampleGate.evaluate` (`core/scoring/MinSampleGate.kt:63-73`).
- **Inconsistencia real citable:** `MinSampleGate` exige `requiresConfidenceInterval = true` para **todas** las métricas incluida `RCI` (`MinSampleGate.kt:41-42`), pero `RciResult.Value(rci, reliable)` (`memory/domain/evaluation/ReliableChangeIndex.kt:21`) **no transporta ningún intervalo**. El gate pide un IC que el productor no puede dar. [Riesgo científico]
- **Segunda inconsistencia:** `MinSampleGate.specFor(RCI).nMinPerCell = 2` (`MinSampleGate.kt:56`) frente a `ProbeReliabilityEstimator.MIN_ADMINISTRATIONS = 3` (`ProbeReliabilityEstimator.kt:35`). Dos autoridades distintas sobre "cuántas administraciones bastan". Reconciliar a **3** (el más conservador) o el gate central deja de ser autoridad. [Mejora crítica]

**Por sistema (agregadas, no por ejercicio):**

| Familia | Métrica | Fuente en código |
|---|---|---|
| Dosis | sesiones/semana, minutos activos | `SessionRecord.activeDurationMs` (`core/session/SessionRecord.kt:42`) |
| Completitud | `completedTrials / plannedTrials` | `SessionRecord.kt:40-41` |
| Abandono | tasa `ABANDONED` / (FINALIZED+ABANDONED) | `SessionStatus` (`SessionRecord.kt:17`) |
| Fatiga intra-sesión | índice de fatiga (VCE↓ + RT↑ + lapsos↑) | `speed/domain/metrics/FatigueIndex.kt:22-35` (≥3 bloques) |
| Fatiga por ítem | nivel 0..1, `isHigh ≥ 0.7` | `language/domain/gates/FatigueGate.kt` |
| Calidad de señal | % sesiones con timebase autoritativo | gate de `SpeedProfileSource` (`CHOREOGRAPHER_FRAME` + `NANOS_NATIVE`) |
| Integridad | % sesiones invalidadas por anti-gaming | `core/antigaming/AntiGaming.kt` |
| Honestidad | % dimensiones que salen de `CALIBRATING` | `MindBoostProfileAggregator` |

**Huecos:** no existe índice de fatiga en Enfoque ni en Memoria (solo Velocidad y Lenguaje). [Dato necesario]

---

## 9.3 Pruebas pre/post

**Protocolo canónico (8 semanas):**

```
Semana -1   PRE-1   probe reservado (3 constructos)   ┐ doble línea base
Semana  0   PRE-2   MISMO probe, forma rotada          ┘ sin entrenamiento entre PRE-1 y PRE-2
Semanas 1-6 ENTRENAMIENTO (dosis objetivo: 4 sesiones/sem)
Semana  7   POST-1  probe reservado, forma rotada
Semana  9   POST-2  seguimiento (retención a +2 semanas, sin entrenar)
```

**PRE-1 → PRE-2 es el instrumento clave y hoy no existe.** La diferencia PRE-2 − PRE-1, sin entrenamiento intermedio, **es el efecto práctica del propio probe**. Sin ella, `untrainedDelta > 0.05` (`RealImprovementCriterion.MIN_DELTA`, `RealImprovementCriterion.kt:44`) puede ser íntegramente práctica del probe y aun así se clasificaría `GENUINE_IMPROVEMENT`. [Mejora crítica]

**Corolario:** el criterio honesto no es `untrainedDelta > 0.05`, es `untrainedDelta > (PRE-2 − PRE-1) + 0.05`. El código actual **no descuenta el retest**.

**Infraestructura por módulo:**

| Módulo | Probe reservado | ¿Administrable hoy? |
|---|---|---|
| Enfoque | `ProbeBank` — seeds 1001-3003 disjuntos del training (`focus/domain/evaluation/ProbeBank.kt:26-52`) | **Sí**: `FocusEvaluationService` está cableado en `FocusGraph` → `FocusEvaluationStateHolder` |
| Memoria | `MemoryProbeBank` (3 probes) + `TransferProbePartition` 20% reservado y disjunto validado en `init` (`content/partition/TransferProbePartition.kt:27-38`) | **No**: ninguna sesión lo invoca |
| Emociones | `EmotionHoldoutProbe` — balanceado, quemado tras uso, fail-closed al agotarse (`emotions/domain/exercise/EmotionHoldoutProbe.kt:29-45`) | **Sí**, y es el **único holdout realmente servido** (`EmotionSessionStateHolder.kt:34`) |
| Velocidad / Lógica / Lenguaje / Decisiones / Creatividad | ninguno | No |

**`EmotionHoldoutProbe` es el modelo a copiar en los demás módulos**: reserva + **servicio** + quema del ítem + fallo cerrado. Un probe reservado que nunca se administra prueba exactamente cero — su propio KDoc lo dice (líneas 12-14). El de Memoria, además, **se degrada con el uso**: el mismo pool reservado de 20% se re-administra en cada evaluación, así que a partir de la 3ª administración deja de ser "no entrenado". [Riesgo científico]

---

## 9.4 Control del efecto práctica — cinco capas, ninguna suficiente sola

1. **Contenido reservado disjunto.** `TransferProbePartition` (`init` valida disjunción, líneas 27-38) y `ProbeBank.allReservedSeeds`. Controla familiaridad de ítem, **no** familiaridad de tarea.
2. **Doble línea base (§9.3).** Controla familiaridad de tarea. **Ausente hoy.**
3. **RCI (Jacobson-Truax) — ya en el repo, citado.** `memory/domain/evaluation/ReliableChangeIndex.kt:29-48`: `SE = sd_pre·√(1−r)`, `SE_diff = √2·SE`, `RCI = (post−pre)/SE_diff`, fiable si `|RCI| > 1.96` (`CRITICAL_Z`, línea 31). Gate honesto: si `r ∉ (0,1]` o `sd_pre ≤ 0` → `NotEnoughData` (líneas 34-35). Gemelo en Enfoque: `focus/domain/evaluation/ReliableChangeIndex.kt` + `FocusEvaluationService.evaluate(...)` (líneas 25-34).
   **Bloqueo operativo:** RCI necesita `r` (fiabilidad test-retest). Hoy `ReliabilitySource.EMPTY` ⇒ RCI **nunca** dispara. Poblar `r` **es un estudio**, no una tarea de código (§9.11).
4. **`RealImprovementCriterion`** (`RealImprovementCriterion.kt:42-69`): `classify` distingue `PRACTICE_EFFECT` (entrenado sube, no-entrenado no) de `GENUINE_IMPROVEMENT`; `isGenuine` exige `untrainedDelta > 0.05` **y** `effectSize ≥ 0.30` **y** `sustainedWeeks ≥ 2`. Diseño correcto. **Nadie lo llama en producción, y nadie produce `effectSize` ni `sustainedWeeks`.** [Dato necesario]
5. **`PlateauDetector.FalseImprovement`** (`memory/domain/antiplateau/PlateauDetector.kt:48-70`): entrenado > 0.15 y no-entrenado < 0.05 ⇒ falsa mejora; plateau tras 7 días estables. Cableado en `MemoryGraph` pero alimentado con métricas de reconocimiento saturadas (`docs/audits/modules/memory.md:161`), luego dispara por las razones equivocadas.

**Sexta capa que falta y es barata: control de exposición de ítems.** El repo ya persiste `SessionRecord.reproducibleIdentity` (semilla + versiones + dificultad efectiva, `SessionRecord.kt:53`) y tiene `core/bank/ExposureTracker.kt`. Con banco finito (Lógica: 6 ítems; Velocidad E3: 4 pares; Memoria: ~142 palabras) **la exposición acumulada es el mejor predictor de la "mejora"**. Registrarla y usarla como covariable convierte una confusión en un control. [Mejora crítica]

---

## 9.5 Activos vs control

**Regla:** ningún resultado con control pasivo (o sin control) sostiene una afirmación de eficacia. Expectativa + Hawthorne + regresión a la media explican la mayoría de los efectos publicados en entrenamiento cognitivo.

Tres diseños, en orden de preferencia:

**A. Control activo cruzado entre módulos (recomendado).** Brazo A entrena Enfoque, brazo B entrena Lenguaje. **Ambos** hacen los probes de **ambos** constructos en PRE/POST. Cada brazo es el control activo del otro: misma app, misma UI, misma expectativa, misma dosis, mismo esfuerzo. La predicción falsable es **cruzada y específica**: A mejora en probes de Enfoque y no en los de Lenguaje, B al revés. Un patrón "ambos mejoran en todo" = efecto inespecífico (retest/expectativa), **no** eficacia. Coste marginal: administrar los probes del otro módulo. Sin negar el producto a nadie.

**B. Control activo intra-ejercicio ("dosis mínima").** Misma UI, misma frecuencia, pero dificultad **fija y baja** (sin adaptación). Aísla la contribución de la **adaptación de dificultad**, que es la hipótesis mecanicista central del producto. Riesgo: el brazo control percibe la app como aburrida → sesga adherencia (medir adherencia como guardrail, no como éxito).

**C. Lista de espera escalonada (stepped-wedge).** Todos entrenan; el orden de entrada se aleatoriza. Éticamente cómodo, estadísticamente más débil (confundido con el tiempo/estacionalidad) y exige más N. Usar solo si A y B se rechazan por producto.

**Prohibido:** aleatorizar por **sesión** en vez de por usuario. Carryover y contaminación intra-sujeto hacen el contraste ininterpretable en entrenamiento cognitivo.

---

## 9.6 Transferencia cercana y lejana

- **Cercana (de contenido) — E3, medible hoy en Enfoque y Emociones:** probe del mismo paradigma con contenido/formas reservadas. Verdicto vía RCI + `RealImprovementCriterion`.
- **Cercana (de paradigma) — E4, NO medible:** exige `ProbeFormat.DIFFERENT_PARADIGM`, explícitamente diferido (`MemoryProbe.kt:17-20`). Construirlo es barato conceptualmente: Enfoque ya tiene 3 paradigmas distintos (PVT / flanker / go-no-go) y `ProbeExerciseMapping`; basta administrar como probe un paradigma **no entrenado** por ese usuario. **Es la mejora de mayor retorno científico de todo el plan.** [Mejora crítica]
- **Lejana / funcional — E5:** fuera de alcance por charter. Si alguna vez se aborda: medida externa validada, cegamiento, control activo, pre-registro, y N que este producto no tiene. **Ningún dato local puede sostenerla.** Cualquier autoinforme de "rindo mejor" es medida de expectativa, no de capacidad — y **no debe recogerse como si fuese eficacia**.

---

## 9.7 Adherencia, fatiga, abandono

Todo instrumentable **hoy, local-first**, sobre `SecureSessionRecordStore`:

| Indicador | Definición operativa | Umbral de alarma |
|---|---|---|
| Adherencia | usuarios con ≥3 sesiones/semana durante ≥4 semanas | < 25% |
| Dosis efectiva | Σ `activeDurationMs` de sesiones `FINALIZED` | mediana < 40 min/semana ⇒ brazo sin dosis, análisis inválido |
| Abandono intra-sesión | `ABANDONED` / (FINALIZED + ABANDONED) por ejercicio | > 35% |
| Completitud | `completedTrials / plannedTrials` | mediana < 0.80 |
| Fatiga | índice ≥ 0 y creciente (`FatigueIndex`, ≥3 bloques) o `FatigueState.isHigh` (≥0.7) | > 40% de sesiones |
| Supervivencia | curva de retención D1/D7/D30 por brazo | divergencia entre brazos > 10pp ⇒ el contraste de eficacia queda confundido con atrición diferencial |

**Trampa que hay que evitar:** la adherencia **no es** un desenlace de eficacia. Un ejercicio adictivo y estéril gana cualquier A/B de retención. La adherencia entra como **guardrail** y como **condición de validez** (sin dosis no hay hipótesis que probar), nunca como métrica primaria de un experimento de eficacia. [Riesgo científico]

**Atrición diferencial = el sesgo #1 de estos estudios.** Análisis primario por **intención de tratar** (todos los aleatorizados, incluidos los que abandonaron, con imputación conservadora). El análisis "solo completadores" es secundario y se etiqueta como sesgado.

---

## 9.8 Detección de ejercicios inútiles y criterio EXPLÍCITO de eliminación

**Gobernanza:** cada criterio se pre-registra en `docs/experiments/EXP-<id>.md` **antes** de mirar datos, con métrica primaria única, N planificado y regla de parada. Cambiar la métrica primaria después de ver datos invalida el experimento. Un ejercicio que dispara un criterio se retira **por defecto**; conservarlo exige una excepción escrita con evidencia, no una narrativa.

| ID | Criterio de ELIMINACIÓN | Evaluable | Acción |
|---|---|---|---|
| **C0** | Falla el test de falsificación V0: `NULL`/`RANDOM`/`EXPERT` no se separan, o la métrica se produce sin conducta | **Hoy**, sin usuarios | **Retirar del set o ocultar números** en ≤1 release. Aplica ya a Enfoque E8/E9/E10 |
| **C1** | Fiabilidad test-retest `r < 0.50` en la métrica gobernante tras 3 administraciones, o `SEM > 0.5·SD` | Estudio (N≥60) | Eliminar como **medida**; puede sobrevivir como práctica sin puntuación |
| **C2** | Techo: ≥60% de usuarios en el 5% superior de la escala en ≤3 sesiones. Piso: mediana ≤ azar+5pp tras 5 sesiones | Local agregable | Rediseñar dificultad o eliminar |
| **C3** | Tras 8 semanas: `classify` = `PRACTICE_EFFECT` en ≥70% de usuarios con dosis suficiente **y** media de `untrainedDelta` ≤ 0.05 con IC95 que contiene 0 | Estudio | Eliminar la afirmación de entrenamiento; deja de contribuir al perfil |
| **C4** | Equivalencia frente al control activo: IC95 de la diferencia en el probe enteramente dentro de ±0.20 SD, con N planificado alcanzado | A/B | Eliminar o fusionar |
| **C5** | Abandono intra-ejercicio > 35% **o** fatiga alta en > 40% de sesiones, **sin** ventaja en C4 | Local + A/B | Eliminar (coste conductual sin retorno) |
| **C6** | Redundancia: correlación entre-usuarios con la métrica gobernante de otro ejercicio `r ≥ 0.80`, sin ventaja de adherencia | Estudio | **Fusionar**. Candidatos ya nombrados por las auditorías: Enfoque E3+E4, E2+E5, E1+E12; Velocidad E1+E6 y E6+E7 |

**Orden de aplicación: C0 → C1 → C2 → C3 → C4 → C5 → C6.** C0 y C1 son fail-closed y **no requieren un solo usuario**: son deuda de ingeniería, no de investigación. Ejecutarlos primero elimina la mayoría de los candidatos sin gastar un experimento.

**Regla anti-rescate:** un ejercicio que falla C0 no se "mejora" retocando el copy ni bajando el umbral de la métrica. Se arregla la cadena estímulo→respuesta→puntuación o se retira. Bajar un umbral para que un ejercicio pase es fabricar el resultado.

---

## 9.9 Diseño A/B — plantilla canónica e instancias

### Plantilla (obligatoria para cada experimento)

1. **Hipótesis** — direccional, falsable, con mecanismo cognitivo nombrado.
2. **Diseño** — unidad de aleatorización = **usuario**; asignación **sticky** y determinista; brazos; duración; control activo.
3. **Métrica primaria** — una sola, pre-registrada, con su gate de muestra (`MinSampleGate`).
4. **Guardrails** — abandono, fatiga, dosis, calidad de señal, % sesiones invalidadas por anti-gaming, % dimensiones que salen de `CALIBRATING`.
5. **Muestra** — N por brazo, MDE, inflación por atrición.
6. **Regla de decisión** — umbral de éxito, umbral de equivalencia, regla de parada, y **qué se elimina si falla**.
7. **Instrumentación** — eventos exactos, dónde se persisten, qué NO se recoge.
8. **Riesgos** — sesgos concretos que pueden producir el resultado sin que la hipótesis sea cierta.

### Mecanismo de asignación (falta y hay que construirlo) [Mejora crítica]

`core/config/FeatureFlags.kt` es un conjunto de **kill-switches globales** — no hay bucketing por usuario, no hay experimentos. Construir:

```
bucket(userId, experimentId) = DeterministicRng.forSeed(
    SessionSeed(hash64(userId) xor hash64(experimentId), RngVersion.CURRENT)
).nextInt(100)
```

- PRNG **pinneado** (`core/runtime/DeterministicRng.kt`, splitmix64) — `kotlin.random.Random(seed)` no es contractual entre versiones de Kotlin (CLAUDE.md 2026-07-15). La asignación debe sobrevivir a un upgrade del compilador o los brazos se re-mezclan en silencio.
- Persistir la asignación (nunca recalcularla como fuente de verdad) en un namespace nuevo `mindboost_experiments`. **Al añadirlo a `MindBoostStorageNamespaces.AccountBound` hay que actualizar en el MISMO cambio el eraser de `AppGraph` (+ su test de cobertura) y `functions/src/accountDeletion.ts`** (CLAUDE.md 2026-06-28), o queda un hueco de borrado GDPR.
- Registrar cada cambio de exposición. Un usuario re-aleatorizado contamina ambos brazos.

### EXP-1 · Adaptación de dificultad vs dificultad fija (control activo intra-ejercicio)

1. **Hipótesis** — entrenar en la zona de desafío (≈60-75% de acierto) produce mayor ganancia en el **probe reservado** que entrenar a dificultad fija baja, porque el mecanismo entrenable es la operación en el límite de capacidad, no la repetición.
2. **Diseño** — 2 brazos, 50/50, 6 semanas de entrenamiento + PRE-1/PRE-2/POST-1/POST-2 (§9.3). Control activo = misma UI, misma dosis, dificultad fija.
3. **Métrica primaria** — delta en el probe reservado (`ProbeBank` en Enfoque; `MemoryProbeBank` cuando se administre), **corregido por el retest** (PRE-2 − PRE-1).
4. **Guardrails** — abandono ≤ +10pp vs control; fatiga alta ≤ 40% de sesiones; dosis mediana ≥ 40 min/sem en ambos brazos; % sesiones con timebase autoritativo ≥ 90% (si baja, el RT no es comparable entre brazos y el análisis se anula).
5. **Muestra** — d entre brazos = 0.30, α=.05 bilateral, potencia .80 ⇒ **175/brazo**; con atrición 40% ⇒ **≈290/brazo aleatorizados** (580 total). Para d=0.40 ⇒ 98/brazo (≈165 con atrición). [Dato necesario: no conozco la base de usuarios activos; si es < 300, este experimento **no es ejecutable** y hay que ir a EXP-3 N-of-1].
6. **Regla de decisión** — **éxito**: IC95 de la diferencia excluye 0 **y** el límite inferior ≥ 0.15 SD, sin violar guardrails. **Equivalencia (fracaso)**: IC95 dentro de ±0.20 SD ⇒ la adaptación no aporta ⇒ simplificar el motor (borrar la maquinaria adaptativa, no dejarla como teatro — cf. Lógica, cuya adaptación ya es decorativa: `LogicStateHolder.startSession()` nunca lee la dificultad). **Sin peeking**: análisis único en N planificado, o alpha-spending O'Brien-Fleming si se mira antes.
7. **Instrumentación** — asignación persistida; `SessionRecord` (status, trials, duración) ya existe; `reproducibleIdentity` para auditar la forma servida; probe administrations contadas (ya lo hace `FocusEvaluationStateHolder.kt:108`).
8. **Riesgos** — (a) atrición diferencial (el brazo fácil aburre) confunde el contraste; (b) el brazo adaptativo ve **más variedad de ítems**, así que parte del efecto puede ser exposición, no dificultad — controlar exposición acumulada como covariable; (c) sin `r` poblado, el veredicto es delta crudo, no RCI ⇒ el claim baja a "preliminar".

### EXP-2 · Especificidad cruzada Enfoque vs Lenguaje (control activo cruzado)

1. **Hipótesis** — la ganancia es **específica del constructo entrenado**: A (Enfoque) mejora en probes de Enfoque y no en los de Lenguaje; B al revés.
2. **Diseño** — 2 brazos, 6 semanas, **ambos brazos hacen los 2 conjuntos de probes** en PRE y POST.
3. **Métrica primaria** — **interacción** brazo × constructo (no el efecto principal). Es lo único que separa eficacia de efecto inespecífico.
4. **Guardrails** — los de EXP-1 + equivalencia de dosis entre brazos (si difieren > 20%, el contraste es de dosis, no de contenido).
5. **Muestra** — detectar una interacción exige ~4× el N de un efecto principal del mismo tamaño: **≈700/brazo** para d=0.30. [Dato necesario] Realista solo con base grande; con base pequeña, degradar a EXP-3.
6. **Regla de decisión** — **prometedor**: interacción significativa con el signo predicho. **Demoledor**: ambos brazos mejoran en **todo** por igual ⇒ el efecto es retest/expectativa ⇒ **ningún ejercicio del set puede reclamar eficacia específica**, y el copy debe reflejarlo.
7. **Instrumentación** — misma que EXP-1 + administración cruzada de probes.
8. **Riesgos** — probes cruzados añaden carga y suben el abandono; el usuario infiere el diseño; efecto de orden en los probes (contrarrestar el orden).

### EXP-3 · N-of-1 (**el único diseño verdaderamente local-first, sin backend**)

1. **Hipótesis** — dentro del mismo usuario, la fase de entrenamiento del constructo X produce cambio en el probe de X y no en el probe de Y (línea base múltiple entre constructos).
2. **Diseño** — línea base múltiple: probes de X e Y medidos periódicamente desde el día 0; el entrenamiento de X entra en la semana 2 y el de Y en la semana 5. La predicción es un **cambio de nivel/pendiente en el momento en que entra cada entrenamiento, y solo en su propio probe**. Alternativa: retirada ABAB (entrenar / parar / entrenar / parar).
3. **Métrica primaria** — cambio de nivel en la serie del probe propio, evaluado con RCI intra-sujeto una vez exista `r`; mientras tanto, delta crudo etiquetado **preliminar**.
4. **Guardrails** — mismos; además, ≥5 puntos de línea base antes de introducir la fase.
5. **Muestra** — **n = 1**, replicado en tantos usuarios como haya. 5-10 replicaciones ya dan un patrón interpretable.
6. **Regla de decisión** — replicación del patrón en ≥70% de los sujetos = prometedor; cambios que ocurren **antes** de introducir la fase = maduración/retest, no tratamiento.
7. **Instrumentación** — **100% local**: probes ya existentes + `SessionRecord` + almacenamiento cifrado por usuario. Cero agregación cross-user, cero backend.
8. **Riesgos** — series cortas y ruidosas; el propio probe se practica (por eso hace falta rotación de formas: `ProbeBank` tiene 3 formas reservadas por constructo — `ProbeBank.kt:31,40,49` —, así que **3 administraciones antes de repetir forma** es el techo duro del diseño). [Riesgo científico]

---

## 9.10 Qué sería prometedor y qué demostraría que un ejercicio NO sirve

**Prometedor (en orden de fuerza):**
- Interacción cruzada específica en EXP-2 con el signo predicho.
- Ganancia en el probe reservado que **excede** el retest (PRE-2 − PRE-1) con `|RCI| > 1.96` en ≥30% de usuarios con dosis suficiente, y `classify` = `GENUINE_IMPROVEMENT`.
- Retención a +2 semanas (POST-2 ≈ POST-1): el efecto sobrevive al cese del entrenamiento.
- Relación **dosis-respuesta** monótona: más sesiones ⇒ mayor delta en el probe. Es la señal más difícil de fabricar por expectativa.
- Replicación del patrón N-of-1 en sujetos independientes.

**Demuestra que NO sirve (cualquiera de estos, aislado, basta):**
- Falla V0 (§9.0): el número no depende de la conducta. **Veredicto inmediato, sin usuarios.**
- Mejora en el material entrenado con `untrainedDelta ≤ 0.05` ⇒ `PRACTICE_EFFECT` (`RealImprovementCriterion.kt:58-59`) o `PlateauStatus.FalseImprovement` (`PlateauDetector.kt:49-51`).
- La ganancia en el probe no supera PRE-2 − PRE-1: todo era efecto de retest.
- Equivalencia frente al control activo (C4): el efecto no es del contenido, es de "usar la app".
- Sin dosis-respuesta: los que entrenan 3× más mejoran igual ⇒ el mecanismo no es el entrenamiento.
- Efecto que se evapora a las 2 semanas: no hubo cambio de capacidad, hubo calentamiento.
- Techo alcanzado en ≤3 sesiones (C2): no queda nada que entrenar.
- Correlación r ≥ 0.80 con otro ejercicio (C6): no aporta varianza propia ⇒ fusionar.

---

## 9.11 Qué se puede hacer HOY (local-first, sin backend) y qué exige backend/estudio

**HOY, sin backend y sin usuarios:**
- **Test de falsificación V0** para los 40+ ejercicios (`desktopTest`). Es la acción de mayor retorno del plan entero y no depende de nadie.
- Reconciliar `MinSampleGate.RCI (nMin=2)` con `ProbeReliabilityEstimator.MIN_ADMINISTRATIONS (3)`, y dotar a `RciResult.Value` del IC que el gate ya exige.
- Test que verifique la **disjunción probe↔training end-to-end en runtime** (no solo en `init`): hoy `TransferProbePartition` valida la disjunción de las listas, pero nada prueba que la sesión real no sirva un ítem reservado.

**HOY, local-first con usuarios reales (sin agregación remota):**
- Adherencia, dosis, abandono, completitud, fatiga: `SessionRecord` **ya lo persiste** (`core/session/SessionRecord.kt:34-54`).
- Administrar probes en Enfoque (cableado) y Emociones (holdout servido) → serie longitudinal intra-sujeto.
- **EXP-3 N-of-1** completo.
- Doble línea base PRE-1/PRE-2 (es orden de administración, no infraestructura nueva).
- Cablear `RealImprovementCriterion` con datos reales (hoy tiene 0 llamadores) y producir `effectSize` y `sustainedWeeks`, que nadie calcula.
- Administrar `MemoryProbeBank` (existe, testeado, nunca invocado).

**Exige backend o estudio supervisado:**
- **Fiabilidad test-retest `r` por métrica** → poblar `ReliabilitySource` (`ProbeReliabilityEstimator.kt:14-19`). Sin esto **RCI nunca dispara y no hay verdicto de "cambio fiable" en todo el producto**. Requiere ≥60 pares pre/pre sin entrenamiento intermedio, agregados. **Es el cuello de botella único de toda la §9.**
- Comparación **entre brazos** de cualquier tipo (EXP-1, EXP-2): exige exportar agregados. `MindBoostAnalyticsHub` es no-op salvo en release Android con sink Firebase (`core/analytics/MindBoostAnalytics.kt`), y sus params son coarse y PII-free por construcción — apto para conteos, **no** para reconstruir series individuales. Un experimento de eficacia necesita el delta por usuario, que **no** cabe en ese canal sin ampliar el alcance de recolección ⇒ decisión explícita de privacidad + consentimiento, no un cambio silencioso.
- Normas poblacionales / percentiles (hoy `FatigueIndex` se normaliza contra la propia corrida, `FatigueIndex.kt:12-15`).
- Probes `DIFFERENT_PARADIGM` a escala y cualquier afirmación de nivel E4.
- Todo E5 (transferencia funcional): fuera de alcance del producto.

**Orden de ejecución recomendado:** V0 (elimina lo inválido) → cablear probes + doble línea base (hace medible lo válido) → N-of-1 (evidencia sin backend) → estudio de fiabilidad (desbloquea RCI) → A/B entre brazos (última, la más cara). Invertir este orden produce A/B caros sobre métricas que no miden nada.