# 4. Auditoria especial del modulo de Emociones

# Auditoría Módulo Emociones — lente psicólogo de la emoción + diseñador de ejercicios + neurocientífico cognitivo

Base: código real en `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/**`, leído en esta sesión. La auditoría previa (`docs/audits/modules/emotions.md`) está **parcialmente obsoleta**: EM3/EM10 sí tienen dificultad real hoy (`SeparatorDifficulty.kt`, MC8u-B §2), y existen guardas nuevas (`EmotionGamingGuard`, `EmotionSessionQuality`, `HIT_RATE`, exposure guard). No repito lo que sigue vigente; corrijo lo caducado y añado lo que esa auditoría **no vio**, que es lo grave.

---

# PARTE A — Auditoría brutal de los 6 cableados

## A.0 Cinco fallos sistémicos que invalidan la mayor parte del entrenamiento (leer antes que nada)

### [Mejora crítica] S1 — La dificultad NUNCA puede subir. En ningún ejercicio. En ninguna sesión.

Cadena verificable:

1. `EmotionSessionStateHolder.kt:799-800`: `BLOCKS = 2`, `TRIALS_PER_BLOCK = 10`.
2. `EmotionSessionStateHolder.kt:128`: `blocksSinceChange = 0` al crear el holder; solo se incrementa en `applyDecision` (`:436`).
3. `EmotionAdaptiveEngine.kt:27-35`: orden de reglas → `stepDown` **antes** de `holdForHysteresis` **antes** de `stepUp`.
4. `EmotionAdaptiveEngine.kt:87-88` + `:156`: `holdForHysteresis` devuelve HOLD si `blocksSinceChange < COOLDOWN_AFTER_DECISIONS (=2)`.

Con 2 bloques hay como máximo **2 decisiones por sesión**: la primera con `blocksSinceChange=0`, la segunda con `1`. Ambas `< 2` ⇒ **`stepUp` es inalcanzable estructuralmente**. Y `difficulty` se reinicializa en cada holder (`:123`, `EmotionDifficultyVector.initial` → `DEFAULT_START = 0.3`, `EmotionDifficulty.kt:30`) y **nunca se restaura de persistencia** (grep: `difficultyStart` solo se **escribe** en `EmotionDtos.kt:85` y `EmotionSessionStateHolder.kt:626`; ningún lector lo devuelve al runtime).

**Consecuencia:** el sistema adaptativo es un trinquete de una sola dirección — solo puede BAJAR (via `stepDown` por 3 errores consecutivos o zona fallida, que sí precede a la histéresis). Todo usuario, siempre, entrena a nivel 0.3 o menos. El "entrenamiento adaptativo" del módulo no existe: es una animación de una variable que se descarta al terminar. Esto invalida el eje "progresión" de los 6 ejercicios a la vez.

### [Mejora crítica] S2 — EM3 y EM10 leen una métrica que su motor no emite: adaptación muerta en ambos sentidos

`DefaultEmotionMetricsEngine.classificationMetrics` (`:153-162`) emite `CATEGORY_ACCURACY`, **nunca** `ACCURACY`. El motor adaptativo, para `AdaptiveZone.GENERAL` (la zona de EM3/EM10, `EmotionExerciseCatalog.kt:60,149`), lee `EmotionMetricKey.ACCURACY`:

- `EmotionAdaptiveEngine.kt:106` → `?: 1.0` ⇒ `1.0 < 0.78` es falso ⇒ **nunca baja**.
- `EmotionAdaptiveEngine.kt:134` → `?: 0.0` ⇒ `0.0 >= 0.85` es falso ⇒ **nunca sube**.

Son dos claves distintas del mismo enum (`EmotionMetricKey.kt:13` vs `:22`). Ningún test lo detecta porque S1 ya impide el step-up y nadie comprueba el step-down por zona. La dificultad de los dos ejercicios con contenido genuinamente emocional está **congelada en 0.3 de por vida**.

### [Mejora crítica] S3 — La métrica gobernante de EM1/EM8 no puede alcanzar confianza en el bloque que gobierna la adaptación

`MIN_NOGO = 6` (`DefaultEmotionMetricsEngine.kt:228`), pero el bloque tiene 10 ensayos:

| Ejercicio | ratio no-go | no-go/bloque a nivel 0.3 | máx. teórico (nivel 1.0) | ¿≥6? |
|---|---|---|---|---|
| EM1 (`EM1PausaGenerator.kt:79-80`) | 0.28 + 0.12·lvl | 3 | **4** | **nunca** |
| EM8 (`EM8AntiImpulsoGenerator.kt:49-50`) | 0.40 + 0.15·lvl | 4 | 6 (solo en lvl = 1.0) | solo en un nivel inalcanzable (S1) |

`zoneMastered(COMMISSION)` exige `valueIfConfident(COMMISSION_RATE)` no nulo (`EmotionAdaptiveEngine.kt:136-137`) ⇒ **imposible** ⇒ EM1/EM8 no suben nunca aunque S1 se arreglara. Y `shouldStepDown(COMMISSION)` usa `?: 0.0` (`:108`) ⇒ tampoco bajan por comisión. EM8 está en **bloqueo circular**: su métrica solo sería fiable a una dificultad que solo se alcanza si su métrica es fiable.

Nota: en el resultado FINAL sí hay 6–8 no-go (`criticalTerminal` ensambla `allTrials`, `:548`), así que la puntuación mostrada puede ser "confiable" mientras la adaptación estuvo ciega toda la sesión. Peor: con n=6 no-go, la resolución de `COMMISSION_RATE` es 1/6 ⇒ el score 0–100 (`EmotionBlockScore.of`) solo puede tomar **7 valores** y una sola falsa alarma cuesta ~17 puntos. Un score 0–100 con 7 estados posibles es ruido presentado como precisión.

### [Mejora crítica] S4 — `confidence` se fabrica desde `dataSufficiency` (violación directa del rigor charter)

`EmotionAggregateFactory.kt:42` y `:51`:
```
sufficiency = sessions / 5.0
confidence  = sufficiency
```
El charter del repo (CLAUDE.md §2) es explícito: *"`dataSufficiency` (tamaño de muestra) y `confidence` (estabilidad) son ortogonales; nunca mapear uno desde el otro"*. Aquí se mapea de forma literal. Y ese objeto es exactamente lo que `EmotionProfileSnapshot` expone al perfil global (`EmotionProfileReadOnly.kt:11-16`): si algún día se cablea al `MindBoostProfileAggregator`, entrará una `confidence` **fabricada** desde un conteo de sesiones. Ninguna sesión mide estabilidad: 5 sesiones idénticas y 5 sesiones erráticas producen la misma "confianza".

En el mismo sitio: `trend = ProfileTrend.UNKNOWN` (`:50`) y `transferConfidence = NOT_DEMONSTRATED` (`:48`) son **constantes**. El módulo, por construcción, no puede demostrar que mejore nada a lo largo del tiempo. Honesto, sí — pero significa que la promesa nuclear del producto ("progreso frente a tu propia línea base") no está implementada en Emociones.

### [Riesgo científico] S5 — La mitad del sistema de seguridad es código muerto en una sesión de 2 bloques

`applyAdaptationAndSafety` llama `captureBaseline` **después** de detectar proxies (`:378-388`), y el check-in solo se dispara `if (proxies.isNotEmpty() && !isLastBlock)` (`:391`) — es decir, únicamente tras el bloque 0, cuando `rtMedianBaseline`/`rtCvBaseline` aún son `null`:

| Proxy | ¿Puede armar el check-in? | Por qué |
|---|---|---|
| `SUSTAINED_SLOWING` | **No** | requiere baseline; en el bloque 0 es null (`OverloadDetector.kt:54`) |
| `EXTREME_RT_VARIABILITY` | **No** | ídem (`:62`) |
| `REPEATED_EXIT_ATTEMPTS` | **No** | `exitAttempts = 0` hardcodeado (`EmotionSessionStateHolder.kt:383`) |
| `COMMISSION_SPIKE` | **No** en EM1/EM8 | exige `valueIfConfident(COMMISSION_RATE)` → S3 |
| `ERROR_STREAK` | Sí | `consecutiveErrors ≥ 3` |
| `REPEATED_PAUSE` | Sí | `pauseUses` |

Y `AdaptiveAction.COOLDOWN` exige `SUSTAINED_SLOWING` (`EmotionAdaptiveEngine.kt:66`) ⇒ **rama inalcanzable**. La red de seguridad real son 2 de 6 señales. El módulo está *más* sobre-documentado en seguridad de lo que está *operativo* en seguridad — que es un problema distinto y peor que estar poco protegido, porque genera confianza injustificada.

---

## A.1 EM1_PAUSA — "Pausa antes de actuar"

- **Mecanismo real:** inhibición de respuesta prepotente (go/no-go). La palabra afectiva (`EM1PausaGenerator.kt:56-75`) es distractor puro: el marco decide (`EmotionTrialClassifier.kt:26-29`). Lo que se entrena es **discriminación perceptiva de un borde** (6dp vs 2dp) + retención motora. La regulación emocional no participa.
- **Transferencia honesta:** `TASK_ONLY` (`EmotionExerciseCatalog.kt:30`) — correcto y bien etiquetado. La transferencia de go/no-go a control de impulsos en la vida real no está demostrada. **No subir esta etiqueta jamás sin datos propios.**
- **Riesgo de falsa mejora: ALTO.** El aprendizaje perceptivo del marco satura en 1–2 sesiones; la comisión baja por familiaridad, no por control. Con S1, además, la tarea nunca se endurece ⇒ **curva de mejora garantizada por práctica pura**, que es exactamente el pseudoprogreso que el charter prohíbe vender.
- **Métricas que engañan:**
  - `COMMISSION_RATE`: resolución 1/6–1/8; el score 0–100 salta ~15 puntos por ensayo. Confunde además *criterio de respuesta* con *sensibilidad* — un usuario conservador y lento puntúa como uno con inhibición fuerte. **Falta d′.**
  - `AFFECTIVE_INTERFERENCE_COST`: `MIN_CELL = 5` por celda de HITs con RT (`:47`, `:232`). Por bloque las celdas son 4/3 ⇒ nunca confiable; a nivel de sesión son ~8/6 ⇒ confiable solo si el usuario acierta casi todos los GO incongruentes. La métrica "estrella afectiva" del ejercicio es, en la práctica, decorativa.
  - `ANTICIPATIONS`: umbral fijo `IMPULSIVE_FLOOR_MS = 350` (`:239`) sin calibración por usuario. Para un usuario rápido honesto es un falso positivo sistemático.
  - `congruent` en no-go (`EM1PausaGenerator.kt:71-75`): se calcula y **nunca entra en la métrica** (el coste solo usa HITs). Semántica muerta.
- **[Riesgo científico] Inducción emocional:** mínima y bien acotada — 24 palabras leves (`EmotionContent.kt:16-41`), sin contenido intenso. El riesgo real aquí no es dañar: es **afirmar afecto donde solo hay decoración**. El `displayName` "Pausa antes de actuar" sugiere control emocional; el ejercicio es motor. Eso es un claim implícito no respaldado.

## A.2 EM2_RECUPERACION — "Recuperación tras error"

- **Mecanismo real:** monitorización de errores y ajuste post-error (constructo sólido: PES/PEA). El compuesto condicional está bien pensado — penaliza el enlentecimiento **solo** si la precisión no se recuperó, y penaliza impulsividad post-error (`DefaultEmotionMetricsEngine.kt:186-197`). Es la mejor pieza de ciencia del módulo.
- **Transferencia honesta:** catálogo dice `PLAUSIBLE` (`:48`). **Es optimista y hay que bajarla a `NOT_DEMONSTRATED`.** Recuperarse tras no distinguir dos barras no es recuperarse tras un fallo que importa. El host es perceptivo y neutro: llamar a esto "recuperación emocional" es una analogía, no una medición.
- **Riesgo de falsa mejora: ALTO por otra vía — el ejercicio deja de medir justo en quien mejora.** El generador planta ensayos difíciles, **no planta errores** (`EM2RecuperacionGenerator.kt:30-43`): la respuesta correcta sigue siendo determinista (barra mayor). Con `HARD_DISC = 0.45` el gap de altura es ~0.36 de la escala (`emotionFieldFor`, `EmotionSessionStateHolder.kt:78-83`) — perfectamente visible. Un usuario competente comete 0–1 errores ⇒ `postErrorRecovery` devuelve `available = false` (`:182`) ⇒ la métrica gobernante sale `unavailable` ⇒ la sesión **no cuenta para calibración** (`EmotionSessionQuality` → `ANOMALY_LOW_CONFIDENCE`). El ejercicio castiga al bueno con "sin datos" y solo mide al que falla. Fallo de diseño psicofísico: falta un **staircase** que fije la tasa de error en ~30 %.
- **Métricas que engañan:** `POST_ERROR_RECOVERY` con `MIN_POST_ERROR_CASES = 3` (`:234`) es ruido puro (3 pares ⇒ error estándar enorme). `RT_CV` se computa sobre correctos limpios y se vende como estabilidad; es sensibilidad atencional y fatiga.
- **[Riesgo científico] Inducción:** el error inducido es leve y perceptivo — **riesgo bajo**, y correctamente por debajo del techo de intensidad 3. Pero la inducción es tan leve que **no activa el sistema que dice entrenar**: no hay coste, no hay significado, no hay reacción afectiva que regular. Seguridad comprada al precio de la validez de constructo.

## A.3 EM3_SEPARADOR — "Separar emoción, pensamiento, impulso y hecho"

- **Mecanismo real:** defusión / granularidad — distinguir estado interno, interpretación, tendencia de acción y dato observable. **Es el único ejercicio del módulo con mecanismo cognitivo-emocional genuino** y la única base teórica plausible de transferencia (la granularidad predice mejor regulación en la literatura; la reevaluación se apoya en esta separación). Corrección a la auditoría previa: **la dificultad SÍ es real hoy** — `SeparatorDifficulty.select/window/interStimulus` (`SeparatorDifficulty.kt:26-58`) mueve ambigüedad, ventana e ISI.
- **Transferencia honesta:** `NOT_DEMONSTRATED` (`:61`) — correcto. Mantener.
- **Riesgo de falsa mejora: MÁXIMO, y no resuelto.**
  - Banco de entrenamiento = **28 frases** (`EmotionContent.kt:60-105`, 40 menos 12 `isEval`). Una sesión consume **20** (`BLOCKS × TRIALS_PER_BLOCK`). En la sesión 2 el usuario ha visto prácticamente todo el banco. Lo que sube no es la capacidad de separar: es el **recuerdo frase→categoría**.
  - El `EmotionExposureGuard` (`SeparatorDifficulty.kt:34-42`) mitiga la repetición *dentro* de un banco de 28 — no puede crear ítems nuevos. Es un paliativo, no una defensa.
  - El **holdout existe y no se usa**: los 12 ítems `isEval = true` están reservados "para la Ola 4" y **ningún generador los presenta jamás** (`EM3SeparadorGenerator.kt:31`, `EM10SepPresionGenerator.kt:30` filtran `!isEval`). Existe la defensa contra el sobreajuste y está desconectada. Sin holdout, la precisión reportada no es habilidad: es memoria.
  - Solo hay **8 ítems ambiguos** (`ambiguity = 1`) en todo el banco de entrenamiento (2 por categoría). A dificultad 1.0 la cuota ambigua se agota en un bloque. El techo de dificultad de contenido es de una sesión.
- **Métricas que engañan (las dos primarias):**
  - `EMOTION_THOUGHT_SEPARATION` (`:128-134`) usa **solo** INTERNAL_STATE y THOUGHT. IMPULSE y FACT quedan fuera. La confusión más importante para la regulación real —**impulso vs hecho**, "quiero discutir" vs "he pulsado dos veces"— no entra en la métrica que lleva el nombre de "separación". Un usuario que confunde sistemáticamente impulso con hecho puede sacar 1.0.
  - `GRANULARITY_INDEX` (`:140-145`): categorías distintas acertadas ÷ categorías presentadas. Acertar **un** ítem de cada categoría = 1.0, con 60 % de precisión global. No mide granularidad; mide cobertura. Engañoso por nombre y por valor.
  - La **matriz de confusión 4×4 se calcula y se tira**: `EmotionSessionAssembler.kt:37-49` la construye y `EmotionBlockResult` la guarda; ninguna métrica ni feedback la consume. El dato más rico del módulo (qué confundes con qué) se descarta.
- **[Riesgo científico] Inducción:** contenido autorreferencial leve ("Creo que no soy capaz de esto", `EmotionContent.kt:78`). Riesgo real pero bajo y bien acotado por el banco curado. **Marcado:** cualquier ampliación del banco debe pasar por el mismo filtro de intensidad; la ambigüedad debe crecer por **frontera semántica**, nunca por intensidad del contenido — el propio KDoc lo declara (`SeparatorDifficulty.kt:15`) y es la regla correcta.

## A.4 EM8_ANTIIMPULSO — "Anti-impulso por comisión"

- **Mecanismo real:** idéntico a EM1. Mismo payload, mismo renderer, mismo clasificador, misma métrica, mismo eje. Difieren en **3 constantes** (`EM8AntiImpulsoGenerator.kt:49-51`). No es un ejercicio: es un preset.
- **Transferencia honesta:** catálogo dice `PLAUSIBLE` (`:126`) mientras EM1, **el mismo paradigma**, dice `TASK_ONLY` (`:30`). Una de las dos es deshonesta y sabemos cuál: la optimista. Corregir EM8 a `TASK_ONLY` es obligatorio — el enum de transferencia es la fuente única de honestidad del módulo (`EmotionExerciseDefinition.kt:11`) y hoy es internamente inconsistente.
- **Riesgo de falsa mejora: ALTO + inflado de cobertura.** EM1 y EM8 ocupan 2 de 6 huecos del menú para la misma tarea. El usuario percibe variedad que no existe. Además `congruent` se calcula (`:40`) y EM8 no computa interferencia ⇒ código semánticamente muerto, otra vez.
- **Métricas que engañan:** las mismas de EM1, más el bloqueo circular de S3.
- **[Riesgo científico] Inducción:** mismo banco leve; sin balanceo de congruencia el "afecto" ni siquiera se mide. Es decoración reconocida como tal — mejor eso que un claim falso, pero entonces **el ejercicio no pertenece al módulo de emociones**: pertenece a Velocidad/Enfoque (donde ya existe E8 Go/No-Go).

## A.5 EM10_SEP_PRESION — "Separación bajo presión leve"

- **Mecanismo real:** EM3 con `BASE_PRESSURE = 0.25` constante y `WINDOW_SPAN = 0.35` (`EM10SepPresionGenerator.kt:47-49`). Ventana efectiva mínima **900 ms** para leer una frase de 5–7 palabras y elegir entre 4 botones.
- **Transferencia honesta:** `PLAUSIBLE` (`:150`) mientras EM3 —el mismo banco, el mismo juicio— es `NOT_DEMONSTRATED`. Segunda inconsistencia del enum de transferencia. Añadir prisa **no añade evidencia de transferencia**; añade un confound.
- **Riesgo de falsa mejora: MÁXIMO.** Hereda la memorización del banco de 28 y suma una segunda vía de pseudoprogreso: **aprendizaje del mapeo motor** (rejilla 2×2 fija de `EmotionFourCategoryField`). La "mejora" es lectura rápida + dedo rápido.
- **Métricas que engañan:** `CATEGORY_ACCURACY` bajo 900 ms **mezcla dos constructos**: conocer las categorías y responder deprisa. Un usuario con separación perfecta pero lectura lenta sale "peor en separación". La métrica gobernante mide velocidad lectora disfrazada de regulación. Lo interesante —¿la separación **aguanta** la prisa?— exige un **índice de robustez** (precisión bajo presión ÷ precisión en calma) que no existe.
- **[Riesgo científico] Inducción:** la presión temporal ES una inducción de carga, leve pero real, y es la única del módulo que escala automáticamente con la dificultad. **Marcado:** con S2 la dificultad está congelada, así que hoy es inofensiva; el día que se arregle S2, `MIN_WINDOW_MS = 900` con contenido autorreferencial pasa a ser una inducción de urgencia sostenida. Hay que fijar el suelo de ventana **por longitud de frase**, no por constante global, antes de desbloquear la adaptación.

## A.6 EM11_FEEDBACK_NEG — "Continuidad después de un error"

- **Mecanismo real:** EM2 con más ensayos difíciles plantados (`MIN_HARD = 4`, `HARD_EVERY = 3`, `EM11ContinuidadGenerator.kt:55-59`). Su única ventaja real: más pares post-error ⇒ métrica algo menos ruidosa.
- **Promesa incumplida:** el `plainDescription` dice "tras un aviso neutro de error" (`EmotionExerciseCatalog.kt:164`). **No existe aviso de error alguno** en el flujo: `runBlock` (`:341-373`) presenta estímulo, espera respuesta, pasa al siguiente. Nada distingue un error para el usuario. El ejercicio se llama por un mecanismo que no implementa. Eso es un claim de producto falso, aunque sea pequeño.
- **Transferencia honesta:** `PLAUSIBLE` (`:162`) → debe ser `NOT_DEMONSTRATED`, igual que EM2.
- **Riesgo de falsa mejora:** doble conteo de cobertura (EM2+EM11 = 2/6 huecos para el mismo paradigma) + práctica perceptiva.
- **[Riesgo científico] Inducción:** aquí está la única inducción con potencial de molestia real —densidad de error elevada— y es **la que menos protección tiene**: con `HARD_EVERY = 3` un usuario puede encadenar errores, y `ERROR_STREAK` (≥3) es uno de los dos únicos proxies vivos (S5). Es correcto que dispare check-in; es incorrecto que sea casi el único centinela funcionando.

## A.7 Resumen Parte A

| | EM1 | EM2 | EM3 | EM8 | EM10 | EM11 |
|---|---|---|---|---|---|---|
| Paradigma | go/no-go | 2-AFC | 4-cat | go/no-go | 4-cat | 2-AFC |
| Mecanismo emocional real | no | no | **sí** | no | **sí** | no |
| Adaptación funcional | **muerta** (S1+S3) | muerta (S1) | **muerta** (S1+S2) | **muerta** (S1+S3) | **muerta** (S1+S2) | muerta (S1) |
| Métrica gobernante fiable | frágil | falla en buenos | sí, pero mal nombrada | sí (sesión) | contaminada | sí |
| Riesgo pseudoprogreso | alto | alto | **máximo** | alto | **máximo** | alto |
| Etiqueta transferencia | correcta | **inflada** | correcta | **inflada** | **inflada** | **inflada** |

**6 ejercicios = 3 paradigmas. 3 paradigmas × 0 adaptación funcional × 1 banco memorizable = el módulo hoy entrena práctica de tarea y mide poco.** La seguridad está sobre-diseñada y medio muerta; la medición está sub-dimensionada; el afecto es decorativo en 4 de 6.

---

# PARTE B — Diseño

## B.1 Las 20 capacidades del brief vs. la realidad del código

| # | Capacidad | Cobertura hoy | Veredicto |
|---|---|---|---|
| 1 | Identificación emocional | ninguna (el self-report es medida, no entrenamiento) | **ausente** |
| 2 | Granularidad | `GRANULARITY_INDEX` trivial (A.3) | **falsa cobertura** |
| 3 | Separación emoción/pensamiento/impulso/hecho | EM3/EM10, métrica de 2 de 4 categorías | parcial, rota |
| 4 | Tolerancia a la frustración | EM6 no cableado, `PLACEBO_RISK` autodeclarado | ausente (bien etiquetada) |
| 5 | Regulación de impulsos | EM1/EM8 (inhibición motora ≠ impulso afectivo) | **proxy débil** |
| 6 | Recuperación tras error | EM2/EM11 (host perceptivo neutro) | parcial |
| 7 | Control de reactividad | ninguna | **ausente** |
| 8 | Decisión bajo emoción intensa | EM5 no cableado | **ausente — el hueco de mayor valor** |
| 9 | Atención bajo estrés | ninguna (EM10 es presión temporal, no carga afectiva) | ausente |
| 10 | Flexibilidad cognitiva emocional | ninguna: **ningún ejercicio cambia la regla** | **ausente** |
| 11 | Reencuadre | ninguna | **ausente — el hueco teórico más grave** |
| 12 | Autoconciencia | self-report pre/post, no entrenada | ausente |
| 13 | Calibración de intensidad | EM4/EM14 no cableados; `CALIBRATION_ERROR` existe y **ningún generador lo produce** | **ausente — el hueco más barato de cerrar** |
| 14 | Detección de sesgos emocionales | EM5 no cableado | ausente |
| 15 | Metacognición emocional | ninguna | ausente |
| 16 | Persistencia ante incomodidad | EM6 no cableado | ausente |
| 17 | Claridad de valores | ninguna | ausente |
| 18 | Estado emocional vs verdad objetiva | rozada por EM3 (hecho vs pensamiento) | parcial |
| 19 | Reconocimiento de patrones internos | imposible: `trend = UNKNOWN` constante (S4) | **estructuralmente ausente** |
| 20 | Pausar antes de actuar | EM1/EM8 | cubierta (`TASK_ONLY`) |

**Cobertura honesta: 2 de 20 capacidades entrenadas con mecanismo defendible (3 y 20), 1 de ellas rota por medición.**

## B.2 Veredicto sobre los 9 no cableados

| Id | ¿Construir? | Orden | Razón funcional |
|---|---|---|---|
| **EM4 / EM14** (calibración) | **SÍ — fusionados** | **1** | Coste marginal casi nulo: una pregunta de confianza 1–5 tras cada respuesta convierte los 20 ensayos ya existentes de **cualquier** ejercicio en señal metacognitiva (Brier + sobreconfianza). `CALIBRATION_ERROR` ya existe en el enum (`EmotionMetricKey.kt:17`) sin productor. Es la única capacidad del brief que se puede medir de forma **objetivamente verificable** (confianza declarada vs acierto real) sin banco nuevo, sin inducción y sin claim de transferencia. Máximo valor / mínimo coste. |
| **EM5** (sesgo afectivo) | **SÍ** | **2** | Único con potencial real de transferencia a decisiones bajo carga afectiva, y la capacidad #8 del brief. Coste alto (renderer de texto + banco ≥120 escenarios generativos). Sin él, el módulo no entrena "decidir bajo emoción". |
| **EM9** (etiquetado) | **NO como ejercicio** | — | Se absorbe como **modo** del Separador (discriminación entre etiquetas vecinas). Un ejercicio propio duplica el paradigma. |
| **EM6** (tolerancia) | **NO como ejercicio v1** | 5 | `PLACEBO_RISK` autodeclarado (`:101`) y **acertado**. "Aguantar dificultad" sin coste real ni consecuencia mide obediencia a la tarea, no tolerancia. Se rescata como **capa de medición** (índice de persistencia segura) sobre ejercicios existentes, nunca como puntuación gobernante. |
| **EM7** (reentrada) | **NO** | — | El constructo real ("volver con control tras pausa") es medible como **métrica** (Δ rendimiento pre/post pausa) dentro de cualquier ejercicio; no merece slot. |
| **EM13** (variabilidad) | **NO** | — | RT-CV no es regulación emocional: es ruido atencional y fatiga. Convertirlo en ejercicio gobernado por RT-CV es medir fatiga y llamarlo estabilidad. **Rechazar por validez de constructo.** |
| **EM12** (espera) | **NO** | — | Tercera variante del mismo go/no-go. Se absorbe como tramo de dificultad (foreperiodo variable) del ejercicio de Inhibición fusionado. |
| **EM15** (recuperación encadenada) | **NO como ejercicio** | — | Se absorbe como tramo de alta densidad de error del ejercicio de Recuperación fusionado. |

**Resultado:** de 9 fichas, **2 se construyen** (EM4/EM14 fusionados, EM5), 5 se absorben como modos/métricas, 2 se rechazan por constructo débil.

## B.3 Diseños

Regla dura respetada en todos: **nunca se muestra la respuesta correcta; el usuario la DERIVA del estímulo.** En los ejercicios de juicio, "correcto" = la opción que es la única sostenida por lo presentado, sin marcar, sin pista de formato, sin posición fija.

---

### R1 · INHIBICIÓN AFECTIVA (fusión EM1 + EM8 + EM12) — *rediseño*

- **Objetivo:** inhibir una respuesta prepotente y medir si la carga afectiva degrada esa inhibición.
- **Mecánica exacta:** ensayos GO frecuentes (marco abierto, respuesta rápida). En un subconjunto, tras un SOA variable, el marco se cierra (**señal de stop**) y hay que abortar. La palabra afectiva se presenta 200 ms antes del marco y su tono (leve, banco actual) **nunca** predice la respuesta. Acierto: responder en GO dentro de ventana; retener en STOP. El SOA se ajusta con escalera 1-arriba/1-abajo hasta 50 % de inhibición exitosa ⇒ **SSRT estimable**.
- **Duración:** 4 bloques × 24 ensayos ≈ 6–7 min.
- **Progresión / adaptación:** el propio staircase ES la adaptación (por ensayo, no por bloque) → inmune a S1/S3. Eje `INHIBITORY_DEMAND` solo controla la proporción de stop (0.25→0.33), nunca el SOA.
- **Métricas:** primaria **SSRT**; secundaria **ΔSSRT afectivo** (stop tras palabra negativa − tras neutra, celdas ≥12 cada una); **d′** de GO/NO-GO. *Engañaría:* `COMMISSION_RATE` cruda (confunde criterio con sensibilidad) y `ANTICIPATIONS` con umbral fijo de 350 ms sin calibrar por usuario.
- **Feedback:** "has frenado a tiempo en X de Y avisos; tu freno tarda ~N ms". Nada sobre estado emocional.
- **Evidencia esperada: `TASK_ONLY`.** SSRT es un constructo inhibitorio validado *dentro de la tarea*; la transferencia a impulsos cotidianos **no está demostrada**. No subir la etiqueta sin datos longitudinales propios.
- **Riesgos / sobrecarga:** el staircase converge a 50 % de fallo de inhibición → sensación de fracaso sostenida. **Señal de sobrecarga:** >4 comisiones consecutivas o petición de pausa; respuesta = bajar proporción de stop, nunca alargar la sesión.
- **[Riesgo científico] Inducción:** mínima (palabra leve). El ΔSSRT afectivo solo es interpretable con ≥12 ensayos por celda; por debajo, **no reportar** — un delta con n=5 es ruido presentado como efecto afectivo.
- **Conexión:** Velocidad E8 (go/no-go) y Enfoque E4 (stop-signal) usan el mismo paradigma; **compartir el `SsrtCalculator` existente** y diferenciarse solo por la capa afectiva. Si no se logra un ΔSSRT afectivo medible, este ejercicio **debe migrar a Velocidad** y salir del módulo.

---

### R2 · RECUPERACIÓN TRAS ERROR (fusión EM2 + EM11 + EM15) — *rediseño*

- **Objetivo:** mantener estrategia y precisión en los ensayos que siguen a un fallo con coste.
- **Mecánica exacta:** clasificación rápida de 2 alternativas sobre estímulos con dificultad controlada por **staircase psicofísico** que fija la tasa de error en **30 %** (garantiza pares post-error para todos). Tras cada error, un marcador neutro y **breve** de 250 ms (el "aviso" que EM11 prometía y nunca implementó) y un **coste visible pero recuperable** (p. ej. el contador de racha vuelve a 0). El usuario nunca ve cuál era la correcta.
- **Duración:** 3 bloques × 20 ≈ 5 min.
- **Progresión:** el staircase mantiene la dificultad en el punto de error objetivo; el eje `POST_ERROR_LOAD` solo controla la longitud de las cadenas de error (1 → 2 → 3 seguidos, tramo EM15).
- **Métricas:** primaria **PEA** (ajuste de precisión post-error) con ≥8 pares y línea base post-acierto ≥8; secundarias **PES**, **persistencia de estrategia** (¿cambia el patrón de respuesta tras error?), **abandono post-error**. *Engañaría:* `POST_ERROR_RECOVERY` con 3 pares (`MIN_POST_ERROR_CASES = 3`) — ruido; y el `unavailable` en usuarios buenos, que hoy parece honestidad y es un fallo de diseño.
- **Feedback:** "tras fallar, tu precisión se mantuvo/bajó X puntos; tardaste N ms más". Sin lenguaje de estado.
- **Evidencia: `NOT_DEMONSTRATED`** (bajar desde el `PLAUSIBLE` actual de EM2/EM11). PES/PEA son constructos reales de control cognitivo; su transferencia a recuperación tras fallos vitales **no está demostrada**.
- **Riesgos:** un staircase al 30 % de error genera frustración sostenida por diseño. **Señales de sobrecarga:** 3 errores consecutivos, ≥2 pausas por bloque, enlentecimiento ≥1.5× la línea base **intra-bloque** (hoy imposible por S5). Respuesta: bajar tasa objetivo a 20 % y ofrecer salida.
- **[Riesgo científico] Inducción:** ESTA es la inducción de frustración más fuerte del módulo y debe llevar techo explícito: máximo 3 errores seguidos, coste siempre recuperable, ninguna métrica visible en rojo, salida siempre a un toque. La línea base de RT debe capturarse en los **primeros 8 ensayos del bloque 0**, no tras el bloque, o los proxies de sobrecarga siguen muertos.
- **Conexión:** Enfoque (post-error slowing ya conceptualizado en su motor de métricas), Decisiones (recuperación tras una decisión mala).

---

### R3 · SEPARADOR (fusión EM3 + EM10 + EM9) — *rediseño; máxima prioridad de arreglo*

- **Objetivo:** distinguir estado interno / interpretación / impulso / hecho observable, y sostener esa distinción bajo prisa.
- **Mecánica exacta:** se presenta **una** frase; el usuario elige una de 4 categorías (posición **aleatorizada por ensayo** para matar el aprendizaje motor). Nada indica la correcta. Modo A (calma, ventana 2500 ms) y Modo B (presión, ventana escalada por longitud de frase: `600 ms + 90 ms/palabra`, nunca constante de 900 ms).
- **Banco:** **generativo composicional** — plantilla × sujeto × complemento (`{Siento|Noto|Hay} + {sensación} + {localización}`, `{Creo|Pienso|Supongo} que + {predicción}`, `{Quiero|Me dan ganas de} + {acción}`, `{contador} + {evento observable}`) ⇒ **≥300 ítems** con eje de confusión etiquetado (pensamiento↔hecho, impulso↔hecho, estado↔pensamiento). **Holdout rotatorio obligatorio**: 20 % de los ítems nunca vistos por ESE usuario, presentados sin marca.
- **Duración:** 3 bloques × 16 ≈ 5 min.
- **Progresión:** proporción de ítems frontera 20 % → 60 % según precisión-holdout; presión solo se desbloquea con precisión-holdout ≥80 % en calma (desacopla conocimiento de velocidad).
- **Métricas:**
  - **Primaria: precisión en holdout** (única defensa contra memorización). Si la precisión de entrenamiento sube y la de holdout no, **es memorización y hay que decirlo**.
  - Secundarias: **d′ por categoría** (las 4, no 2); **confusión impulso↔hecho** y **pensamiento↔hecho** desde la matriz que `EmotionSessionAssembler.kt:37-49` **ya calcula y hoy se tira**; **índice de robustez** = precisión-bajo-presión ÷ precisión-en-calma.
  - *Engañarían:* `EMOTION_THOUGHT_SEPARATION` actual (ignora 2 de 4 categorías) y `GRANULARITY_INDEX` actual (cobertura disfrazada de granularidad). **Ambas deben renombrarse o retirarse**: hoy el nombre promete más que el cálculo.
- **Feedback:** específico desde la confusión: "sueles etiquetar impulsos como hechos". Accionable, no valorativo.
- **Evidencia: `NOT_DEMONSTRATED`** (mantener). Con banco generativo + holdout, es el **único** candidato del módulo a subir algún día a `PLAUSIBLE`, y solo con datos propios.
- **Riesgos:** contenido autorreferencial ⇒ el banco generativo debe pasar el mismo filtro de intensidad leve que el manual; **prohibido** que el generador combine plantillas en frases de intensidad alta.
- **[Riesgo científico] Inducción:** leve, pero es contenido sobre uno mismo. La dificultad debe crecer por **frontera semántica**, jamás por intensidad — regla ya escrita en `SeparatorDifficulty.kt:15` y que hay que preservar en el generador.
- **Conexión:** Lenguaje (*Argument X-Ray*, *Scope & Negation* — separar afirmación de inferencia es el mismo músculo aplicado a texto ajeno); Decisiones (separar hecho de interpretación antes de decidir).

---

### N1 · CALIBRACIÓN (EM4 + EM14 fusionados) — *nuevo, prioridad 1*

- **Objetivo:** ajustar la confianza declarada al acierto real (metacognición), y detectar cómo la carga afectiva la desajusta.
- **Mecánica exacta:** capa **transversal**, no una tarea nueva. Tras cada respuesta de **cualquier** ejercicio (R1/R2/R3), una tira de 3 botones: *poco seguro / medio / muy seguro* (≤600 ms, no bloqueante — si no responde, ese ensayo no entra en calibración). Adicionalmente, **una predicción por bloque**: "de los próximos 16, ¿cuántos acertarás?" (0–16). El usuario nunca ve la respuesta correcta ni su acierto ensayo a ensayo; el desajuste se revela solo al final del bloque.
- **Duración:** +0 min (parasita las sesiones existentes) + 20 s de predicción.
- **Progresión:** ninguna artificial. La dificultad la aporta el ejercicio anfitrión. La calibración mejora sola con feedback agregado.
- **Métricas:** primaria **puntuación de Brier** (o error absoluto de calibración) con **n ≥ 30 juicios** — por eso es transversal, no un ejercicio de 20 ensayos; secundarias **sobreconfianza** (confianza media − acierto medio, con signo), **resolución** (¿discrimina sus aciertos de sus fallos?), **error de predicción de bloque**. *Engañaría:* un Brier con n<30 (inestable); y usar la confianza media sola, que no distingue "seguro y acertado" de "seguro y equivocado".
- **Feedback:** "cuando dijiste 'muy seguro', acertaste el 62 % — tu certeza va por delante de tu acierto". Es el feedback más accionable posible y no requiere ningún claim afectivo.
- **Evidencia: `PLAUSIBLE`.** La calibración es entrenable y medible con verdad objetiva por diseño (no depende de auto-informe interpretado). Es la **única** capacidad del brief con verificación objetiva incorporada.
- **Riesgos:** pedir confianza puede inducir titubeo y ralentizar; por eso es opcional por ensayo y no bloqueante. **Sobrecarga:** si el tiempo de la tira supera al de la respuesta principal, desactivar la capa.
- **[Riesgo científico]** Ninguna inducción. Riesgo cero. Es la mejora de mayor relación valor/riesgo del módulo entero.
- **Conexión:** Lógica (confianza ya modelada en el scorer ICR), Decisiones (calibración es el núcleo de decidir bien), Memoria (JOL — juicios de aprendizaje). **Debería ser un servicio de `core/`, no de emociones.**

---

### N2 · DECISIÓN BAJO CARGA AFECTIVA (EM5) — *nuevo, prioridad 2*

- **Objetivo:** elegir según la evidencia presentada cuando una opción es más atractiva por su envoltorio afectivo.
- **Mecánica exacta:** escenario corto (≤35 palabras) con **dos opciones**: una con datos que la sostienen y redacción sosa; otra con redacción atractiva/urgente y datos peores. La atractividad **nunca** se correlaciona con la corrección (contrabalanceo 50/50 por bloque). El usuario elige; **jamás se marca cuál era**: la corrección se deriva de comparar las cifras presentadas. Un 20 % de ensayos son de control **neutro** (ambas opciones sosas) para separar "sesgo afectivo" de "no saber comparar cifras".
- **Duración:** 2 bloques × 12 ≈ 6 min (ensayos largos).
- **Progresión:** distancia entre las opciones (evidente → ajustada) y fuerza del envoltorio (leve → moderada, techo 3). Nunca contenido intenso.
- **Métricas:** primaria **`AFFECTIVE_DECISION_QUALITY`** = precisión en ensayos con conflicto − precisión en ensayos neutros (**el sesgo, no la precisión bruta**); secundarias RT de conflicto vs neutro (coste de interferencia real), y coherencia con la propia elección previa. *Engañaría:* la precisión bruta — un usuario malo en cifras y otro sesgado por el envoltorio puntúan igual sin la condición de control.
- **Feedback:** "cuando la opción sonaba urgente, tu acierto bajó X puntos". Descriptivo, no moral.
- **Evidencia: `PLAUSIBLE` con reserva.** La reducción de sesgo dentro de la tarea es medible; la transferencia a decisiones reales **no está demostrada** y no debe insinuarse.
- **Riesgos:** el banco es el cuello de botella (≥120 escenarios; con menos se memorizan). Debe evitar dominios sensibles (dinero real, salud, relaciones): usar contextos operativos neutros.
- **[Riesgo científico] Inducción:** el envoltorio "urgente/atractivo" ES una inducción afectiva deliberada — **la más fuerte del módulo propuesto**. Techo estricto: urgencia sí, amenaza no; nunca personal, nunca sobre el usuario. Y la métrica es **diferencial** (conflicto − neutro), lo que la hace robusta al estado basal del usuario.
- **Conexión:** Decisiones (Arena Bayesiana, Triaje de reversibilidad) — **este ejercicio pertenece tanto a Decisiones como a Emociones**; construirlo compartido evita un tercer banco de texto.

---

### N3 · CAMBIO DE REGLA BAJO CARGA (flexibilidad cognitiva emocional) — *nuevo, prioridad 3*

- **Objetivo:** cubrir la capacidad #10, hoy **totalmente ausente**: ningún ejercicio del módulo cambia nunca la regla.
- **Mecánica exacta:** clasificación por una regla que el usuario **infiere del feedback implícito** (acierto/fallo, nunca enunciada) y que **cambia sin aviso** cada 8–14 ensayos. La carga afectiva se manipula sobre el material (palabra leve del banco), nunca sobre el usuario.
- **Duración:** 3 bloques × 20 ≈ 5 min.
- **Progresión:** frecuencia del cambio y ambigüedad de la regla nueva.
- **Métricas:** primaria **coste de cambio** = ensayos hasta recuperar el criterio tras la reversión; secundarias errores perseverativos (seguir aplicando la regla vieja) y **Δ coste con material afectivo vs neutro**. *Engañaría:* la precisión global — promedia el post-cambio con el estable y borra el efecto.
- **Feedback:** "tardaste 4 intentos en darte cuenta del cambio". Concreto.
- **Evidencia: `TASK_ONLY`** hasta demostrar lo contrario. La flexibilidad set-shifting es un constructo real; su transferencia, no.
- **Riesgos:** los errores perseverativos frustran por diseño. **Sobrecarga:** >6 ensayos sin recuperar criterio ⇒ ampliar el intervalo entre cambios.
- **[Riesgo científico]** Inducción indirecta (frustración por incertidumbre), leve. Marcado: la regla debe ser siempre **derivable** en ≤4 ensayos; si no, es aleatoriedad disfrazada de tarea y produce indefensión, no flexibilidad.
- **Conexión:** Enfoque E6 (task-switching ya implementado, `RuleSwitch`) — **reutilizar su renderer y su `SWITCH_COST`**; la única aportación de Emociones es la capa de material afectivo. Si el Δ afectivo no aparece, no construir versión propia.

---

### N4 · PATRONES INTERNOS / AUTOPREDICCIÓN — *nuevo, prioridad 4*

- **Objetivo:** capacidades #19 (patrones internos) y #12/#15 (autoconciencia, metacognición) — hoy **estructuralmente imposibles** porque `trend = UNKNOWN` es constante (S4).
- **Mecánica exacta:** al empezar la sesión, el usuario **predice** su propio rendimiento relativo ("hoy rendiré peor / igual / mejor que tu media") y una condición ("¿en qué momento del día sueles ir mejor?"). Al terminar se contrasta con el dato real ya persistido. Sin banco, sin estímulos nuevos.
- **Duración:** 20 s por sesión.
- **Progresión:** ninguna. Es una capa longitudinal.
- **Métricas:** primaria **precisión de autopredicción** (correlación entre lo predicho y lo observado, **n ≥ 8 sesiones**); secundaria detección de patrón real (varianza intra-usuario explicada por hora/día). *Engañaría:* cualquier lectura con n<8; y presentar un patrón espurio de 3 sesiones como "tu patrón" — es el riesgo de sobreinterpretación clásico.
- **Feedback:** "acertaste tu propia predicción en 6 de 9 sesiones". Si n<8: **"—", nunca un número**.
- **Evidencia: `NOT_DEMONSTRATED`.** Es una medida honesta de autoconocimiento; no hay evidencia de que entrenarla mejore la regulación.
- **Riesgos:** máximo riesgo de **narrativa fabricada**. Prohibido inferir causas ("rindes peor por estrés"); solo asociaciones descriptivas con n suficiente.
- **[Riesgo científico]** Sin inducción. El riesgo es interpretativo, no afectivo. **Requisito previo:** arreglar S4 (calcular tendencia real) — sin eso este ejercicio no puede existir.
- **Conexión:** todo el perfil MindBoost; es la capa que convierte sesiones sueltas en "progreso frente a tu propia línea base".

---

### N5 · COHERENCIA CON PRIORIDADES (claridad de valores, #17) — *nuevo, prioridad 5, con reserva fuerte*

- **Objetivo:** cubrir #17 sin caer en un cuestionario de valores (no medible, no entrenable, riesgo de placebo alto).
- **Mecánica exacta:** el usuario ordena **una vez** 5 prioridades operativas neutras (velocidad, precisión, ahorro de esfuerzo, ayudar al equipo, aprender). Después, en ensayos de decisión rápida bajo presión y tras error, elige entre opciones que enfrentan dos de esas prioridades. El sistema **no juzga qué está bien**: mide **consistencia con el orden que el propio usuario declaró**.
- **Duración:** 4 min.
- **Métricas:** primaria **índice de coherencia bajo carga** = % de elecciones consistentes con el orden propio, comparado entre condición calma y condición presión/post-error. *Engañaría:* la coherencia absoluta — la señal es el **delta bajo carga**, no el nivel.
- **Evidencia: `PLACEBO_RISK`.** Declararlo así, como el catálogo hace bien con EM6. Es el diseño más honesto posible para una capacidad que no tiene verdad objetiva externa.
- **[Riesgo científico]** No inducir conflicto de valores personales reales. Contextos operativos, nunca vitales. **Si no se puede medir el delta bajo carga con n suficiente, no construirlo**: un ejercicio de valores sin métrica diferencial es exactamente el placebo que el charter prohíbe.

---

### Capas de medición (no ejercicios): rescatan EM6, EM7, EM13 y la capacidad #9

- **Persistencia segura (EM6, #16):** índice sobre R2/R3 — continuar tras dificultad creciente con **la pausa contando como regulación, no como fallo**. Nunca métrica gobernante. `PLACEBO_RISK`.
- **Reentrada (EM7):** Δ precisión y Δ RT en los 3 ensayos posteriores a una pausa, dentro de cualquier ejercicio. Métrica, no ejercicio.
- **Variabilidad (EM13):** RT-CV se conserva **solo** como veto de step-up (ya lo hace, `EmotionAdaptiveEngine.kt:127`) y como señal de fatiga. **Nunca** como métrica gobernante de "estabilidad de regulación".
- **Atención bajo estrés (#9):** no construir tarea nueva. Medir el **delta** del mismo ejercicio de Enfoque (E1/E3) con y sin capa afectiva. Coste marginal cero, constructo limpio (diferencia intra-sujeto).

## B.4 Orden de ejecución

| Ola | Trabajo | Por qué en ese orden |
|---|---|---|
| **0 — Reparación** | S1 (histéresis + persistir dificultad), S2 (`ACCURACY` vs `CATEGORY_ACCURACY`), S3 (ensayos/bloque ≥24), S4 (`confidence` ≠ `sufficiency`, tendencia real), S5 (línea base intra-bloque, `exitAttempts` real) | **Sin esto, todo lo demás se construye sobre un motor adaptativo muerto y un perfil con confianza fabricada.** Ningún ejercicio nuevo antes. |
| **1** | N1 Calibración (capa transversal) | Coste mínimo, riesgo cero, cubre la capacidad de mayor valor metacognitivo y **da uso a `CALIBRATION_ERROR`**, hoy huérfana. |
| **2** | R3 Separador: banco generativo ≥300 + holdout activo + métrica de 4 categorías + explotar la matriz de confusión | Es la joya y hoy produce pseudoprogreso por memorización en 2 sesiones. |
| **3** | R1 y R2 (fusiones + staircases) · retirar EM8/EM10/EM11 del menú · corregir las 4 etiquetas de transferencia infladas | Menos entradas, mejor medidas; libera 3 huecos de menú. |
| **4** | N2 Decisión bajo carga afectiva (compartido con Decisiones) | Alto valor, alto coste de banco. |
| **5** | N3 flexibilidad (reusando Enfoque E6) · N4 autopredicción (requiere S4) · capas de medición | Cierra 4 capacidades más con coste marginal bajo. |
| **Nunca** | EM13 como ejercicio; EM9/EM12/EM15 como entradas propias | Constructo débil o redundancia pura. |

---

# SECCIÓN 4 — Auditoría especial del módulo de emociones

## 4.1 Qué entrena realmente el módulo hoy

Tres paradigmas de control cognitivo (inhibición motora, ajuste post-error perceptivo, clasificación semántica de 4 vías) presentados como seis ejercicios de regulación emocional. **En 4 de los 6, el afecto es decorativo**: la palabra emocional nunca decide la respuesta y su efecto (`AFFECTIVE_INTERFERENCE_COST`) casi nunca alcanza confianza. Solo EM3/EM10 tienen contenido cognitivo-emocional genuino, y su medición está rota por banco finito y por una métrica que ignora la mitad de las categorías.

## 4.2 Seguridad de la inducción emocional — [Riesgo científico] en cada punto

**Lo que está bien hecho y hay que preservar:**
- Techo operativo duro de intensidad 3; los niveles 4/5 **solo** llegan de auto-informe, nunca del motor (`EmotionDifficulty.kt:43-61`).
- La seguridad precede estrictamente al rendimiento en el orden de reglas (`EmotionAdaptiveEngine.kt:27-35`).
- Los proxies **arman una comprobación**, nunca infieren emoción (`OverloadDetector.kt:27-30`). Es la postura epistémica correcta: el sistema no sabe cómo te sientes y no finge saberlo.
- El auto-informe se almacena **separado** de los proxies conductuales y nunca se fusiona con ellos (`SelfReport.kt:24-29`).
- La pausa **nunca penaliza** y su abuso reduce intensidad en vez de castigar (`EmotionSafetyLimits.kt:30-37`).
- El banco de contenido es leve, cotidiano, no íntimo, no moralizante y está bajo escáner de terminología.

**Lo que está mal y es un riesgo real:**
1. **[Riesgo científico] La red de seguridad está medio muerta** (S5): 4 de 6 proxies no pueden dispararse en una sesión de 2 bloques. Un sistema de seguridad que se documenta como completo y opera al 33 % es peor que uno modesto y honesto, porque autoriza a subir la exigencia confiando en centinelas que no existen.
2. **[Riesgo científico] La inducción más fuerte (densidad de error de EM11) es la peor vigilada**: `ERROR_STREAK` es uno de los dos únicos proxies vivos, y la línea base de RT llega demasiado tarde para detectar enlentecimiento sostenido.
3. **[Riesgo científico] El auto-informe pre-sesión introduce características de demanda**: preguntar valencia/activación antes de entrenar sesga tanto la percepción como el rendimiento posterior. Hoy `selfReportBefore` alimenta la decisión de step-up (`EmotionAdaptiveEngine.kt:115`) — es decir, **una medida sesgada por la propia pregunta gobierna la dificultad**. [Dato necesario] hay que verificar el efecto del auto-informe pre sobre el rendimiento antes de seguir usándolo como puerta.
4. **[Riesgo científico] Cuando S2 se arregle, EM10 pasa a ser inducción de urgencia real** con suelo de ventana constante de 900 ms sobre frases autorreferenciales. Fijar el suelo por longitud de frase **antes** de desbloquear la adaptación.
5. **[Riesgo científico] El banco generativo propuesto para R3 puede salirse del techo de intensidad por combinación**: dos plantillas leves pueden componer una frase no leve. El filtro debe aplicarse a la **frase generada**, no a los fragmentos.

## 4.3 Honestidad de las afirmaciones

| Afirmación en el código | ¿Sostenida? |
|---|---|
| `EM8 = PLAUSIBLE` vs `EM1 = TASK_ONLY` (mismo paradigma) | **No.** Inconsistencia interna del único enum que garantiza honestidad. |
| `EM2/EM11 = PLAUSIBLE` | **No.** Recuperación post-error perceptiva → recuperación emocional no está demostrada. |
| `EM10 = PLAUSIBLE` vs `EM3 = NOT_DEMONSTRATED` (mismo banco) | **No.** Añadir prisa no añade evidencia. |
| "Continúa tras un aviso neutro de error" (EM11) | **No.** El aviso no existe en el flujo. |
| `GRANULARITY_INDEX` mide granularidad | **No.** Mide cobertura de categorías. |
| `EMOTION_THOUGHT_SEPARATION` mide la separación | **Parcial.** Solo 2 de las 4 categorías. |
| `EmotionBlockScore` es rendimiento en la tarea, no estado emocional | **Sí**, y el disclaimer es explícito y correcto (`EmotionBlockScore.kt:22`). Buen trabajo. |
| `trend = UNKNOWN`, `transferConfidence = NOT_DEMONSTRATED` | **Sí, honesto** — pero significa que el módulo no puede demostrar mejora alguna. |
| `confidence` del perfil | **No.** Fabricada desde el conteo de sesiones (S4), violando el charter. |

## 4.4 Medición de constructos emocionales: el problema de fondo

Un constructo emocional solo es medible si se cumplen tres condiciones, y el módulo falla las tres a la vez en la mayoría de sus ejercicios:

1. **El estímulo debe activar el proceso.** Un marco con borde grueso y la palabra "prisa" no activa regulación. Sin activación no hay nada que regular y la métrica mide otra cosa (discriminación perceptiva).
2. **La respuesta debe depender del proceso.** En EM1/EM8/EM2/EM11 la respuesta correcta es derivable **ignorando** por completo el afecto. Un usuario que no procesa nada afectivo puntúa perfecto.
3. **La métrica debe ser diferencial.** El afecto se mide con **contrastes** (congruente − incongruente, conflicto − neutro, presión − calma), nunca con un valor absoluto. El módulo tiene UNA métrica diferencial (`AFFECTIVE_INTERFERENCE_COST`) y está estructuralmente sub-potenciada (`MIN_CELL = 5` con celdas de 3–4 por bloque).

**Regla de diseño que debe adoptarse:** *toda métrica que reclame el adjetivo "afectiva" debe ser un contraste entre dos condiciones con ≥12 observaciones por celda; si no se alcanza, no se reporta.* Ningún ejercicio actual cumple esa regla.

## 4.5 Riesgo de falsa mejora del módulo, ordenado

1. **Memorización del banco (EM3/EM10):** 28 ítems, 20 por sesión, holdout implementado y desconectado. En la sesión 3 la precisión sube por recuerdo. **Es el pseudoprogreso más grave del módulo y afecta a su mejor ejercicio.**
2. **Práctica perceptiva (EM1/EM8/EM2/EM11):** el marco y las barras se automatizan en 1–2 sesiones.
3. **Aprendizaje motor (EM10):** rejilla 2×2 fija ⇒ respuesta por posición.
4. **Dificultad congelada (S1/S2/S3):** al no poder subir, la mejora aparente es 100 % efecto de práctica sobre una tarea constante. **Es el multiplicador de los tres riesgos anteriores.**
5. **Calibración desbloqueable por repetición:** 5 sesiones válidas ⇒ tier STABLE (`EmotionAggregateFactory.kt:33,76-81`) sin ninguna exigencia de estabilidad de rendimiento. "Estable" significa hoy "has vuelto 5 veces".

## 4.6 Lo que el módulo NO puede afirmar (y hoy no afirma — mantenerlo así)

No puede afirmar que mejore la regulación en la vida real, que reduzca la reactividad, que mida un estado interno, ni que su número 0–100 diga algo sobre la persona. El disclaimer de `EmotionBlockScore` y los `TransferLabel` son la infraestructura correcta para impedirlo; el problema es que **cuatro etiquetas están infladas** y ese es el único mecanismo de contención.

## 4.7 Las tres intervenciones que más elevan el poder cognitivo del módulo

1. **Reparar el motor (Ola 0).** La histéresis de 2 bloques en sesiones de 2 bloques, el desajuste de clave `ACCURACY`/`CATEGORY_ACCURACY`, el `MIN_NOGO=6` sobre bloques de 10, la dificultad no persistida y la `confidence` copiada de `dataSufficiency` son **cinco defectos de una línea cada uno** que, juntos, anulan la adaptación y el perfil. Es el mejor retorno por línea de código de todo MindBoost.
2. **Calibración transversal (N1).** Convierte 20 ensayos inertes en la única señal metacognitiva del producto con verdad objetiva incorporada, sin banco, sin inducción y sin riesgo. Y da productor a una métrica que lleva meses declarada y huérfana.
3. **Separador con banco generativo + holdout activo + métrica de 4 categorías + matriz de confusión explotada.** Es el único ejercicio del módulo con base teórica para transferir; hoy lo mata un banco de 28 frases y una métrica que ignora la confusión más importante (impulso↔hecho). Los 12 ítems de holdout ya existen: activarlos es la defensa que separa habilidad de memoria.

## 4.8 Veredicto

**El módulo es seguro, honesto en su lenguaje y sólido en su ingeniería de persistencia — y débil en lo único que justifica su existencia: entrenar y medir regulación cognitivo-emocional.** Se ha invertido más rigor en no dañar y en no mentir que en activar el proceso que dice entrenar. Hoy son tres tareas de control cognitivo con decorado afectivo, con la adaptación muerta en los seis ejercicios y el ejercicio más valioso saboteado por un banco memorizable y una métrica mal definida.

Con la Ola 0 (cinco arreglos puntuales) + N1 (calibración) + R3 (separador con holdout), el módulo pasa de **2 de 20 capacidades entrenadas** a **6–7 medidas honestamente**, sin un solo claim nuevo y sin subir un solo grado la inducción emocional.