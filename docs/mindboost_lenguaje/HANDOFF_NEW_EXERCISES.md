# Handoff — MindBoost · Lenguaje: menú de ejercicios + 5+ ejercicios world-class

> **Para el chat nuevo:** este documento es autocontenido. Empieza leyendo
> `CLAUDE.md` (reglas + budget gate), luego este archivo. NO necesitas el chat anterior.
> Objetivo del usuario: *"el mejor entrenador cognitivo del mundo"* — máxima calidad, ejercicios
> perfectamente diseñados para maximizar la capacidad cognitivo-lingüística.

---

## 0. Arranque obligatorio (Claude Code en este repo)

1. **Budget gate antes de tocar código** (regla de `CLAUDE.md`):
   `node .claude/scripts/budget-gate.mjs --task "<desc>" --remaining <0-100>`
   - El gate es heurístico sobre el TEXTO. Evita en la descripción las palabras **"session"** (→ dispara `auth`) y **"multiple"** (→ `multiple_modules`); suben el impacto y lo bloquean.
   - Respeta su decisión: `allow` procede; `plan_only`/`split`/`deny` → reduce alcance. Pide al usuario su `--remaining` si hace falta.
2. **Trabaja en slices acotados**, gateando cada uno. Tras cada slice: tests + detekt.
3. **Verificación**:
   - Tests JVM: `./gradlew.bat :shared:desktopTest --tests "com.mindos.app.features.mindboost.language.*"`
   - Android: `./gradlew.bat :shared:compileAndroidMain`
   - Ratchet: `./gradlew.bat :shared:detekt` (one-way; arregla findings nuevos, no regeneres baseline).
   - APK demo: `./gradlew.bat :androidApp:installDebug` (emulador `Medium_Phone_API_36.1`; pkg `com.mindos.app.debug`; track Lenguaje en Home → MINDBOOST → Lenguaje).

---

## 1. Estado actual (lo que YA existe y funciona)

Feature `com.mindos.app.features.mindboost.language` en `shared/src/commonMain`. **Fases 1-5 del spine completas y verdes (~150 tests, detekt limpio)**. Mapa: `docs/mindboost_lenguaje/{COVERAGE_MATRIX,ARCHITECTURE,PRODUCTION_CHECKLIST,REVIEW_NOTES}.md`.

Motores reales (puros, offline, `commonMain`) listos para reutilizar:
- `domain/scoring/ClosedScoringEngine` — puntúa **3 formatos cerrados** vía `AnswerKey`:
  `SingleChoice`, `EvidenceSelection` (opción + evidencia requerida), `Ordering` (Kendall-tau). Aplica gates §5.6.
- `domain/repair/DefaultRepairEngine` — catálogo de 14 patrones de error → microlección + reintento isomórfico.
- `domain/evidence/DefaultEvidenceEngine` + `domain/projection/ProfileProjector` — event-sourcing: paquetes → perfil; peso 0 nunca promociona.
- `domain/claim/DefaultClaimEngine` — copy gobernado por evidencia (E0-E5); nunca infla.
- `domain/transfer`, `domain/evaluator` (stub offline), `domain/gates` (fatiga/confusor/fairness), `domain/adaptation` (utilidad §6.4).
- `presentation/` UI models + builders (`GenomeUiBuilder`, `CoachUiBuilder`, `ProgressUiBuilder`, `EvidenceCopy`).

**UI demo actual** (provisional, reutilizar como base):
- `presentation/LanguageDemoBank.kt` — 7 ítems: 5 rasgos (INF, LIT, LEX_P, SUM, ARG) × formatos (3 single-choice de distintos rasgos + 1 evidencia + 1 ordenación).
- `presentation/LanguageSessionStateHolder.kt` — `StateFlow` síncrono que orquesta scoring→evidencia→proyección→claim→repair. Stages `QUESTION/FEEDBACK/DONE`. Cabecera por habilidad + resumen por habilidad.
- `presentation/ui/LanguageRootScreen.kt` — Compose mínimo, 3 vistas por formato. Cabecera dinámica `Lenguaje · {trait.display}`. Reusa tokens `AppColors.Focus*` (sin tokens nuevos).
- Registrado como track `Lenguaje` en `presentation/ui/MindBoostHubScreen.kt`.

Taxonomía disponible: `domain/model/LanguageTaxonomy.kt` (19 `TraitCode`, `OperationType`, `ResponseMode`, `BankType`, 14 `ErrorType`). Familias de tareas élite en la spec v6 §7.5.

**Fuente de verdad de diseño:** `C:\Users\curro\Downloads\MindBoost_Lenguaje_v6_NoCompromise_WorldClass.md`.

---

## 2. Tarea a ejecutar

> Desarrollar **al menos 5 ejercicios nuevos, completos y de máxima calidad**, **elegibles desde un menú al entrar en el módulo Lenguaje**, diseñados para **maximizar la capacidad cognitivo-lingüística**.

Esto implica:
1. **Pantalla de selección (menú)** al entrar en Lenguaje: lista de ejercicios elegibles (icono/nombre/qué entrena), + opción "Sesión mixta". Stage nuevo `MENU` antes de `QUESTION`.
2. **Catálogo de ejercicios** (`LanguageExercise`): cada ejercicio = id, nombre, rasgo(s), familia v6, descripción, banco de ítems (≥5 calibrados por ejercicio), formato(s) de respuesta.
3. **5-6 ejercicios world-class** (diseño abajo), cada uno con banco real, scoring determinista, distractores diagnósticos mapeados a `ErrorType`, feedback de reparación real, y resumen claim-safe.
4. Mantener todo en `commonMain` puro + UI Compose mínima reutilizando el patrón actual.

---

## 3. Principios de diseño (por qué "maximizan capacidad cognitiva")

Anclar cada ejercicio en ciencia del aprendizaje (no gamificación vacía):
- **Práctica de recuperación** > reconocimiento: el usuario produce/decide, no solo lee.
- **Dificultad deseable** (Bjork): restricción (límite de palabras), material nuevo, distractores plausibles, interleaving de rasgos.
- **Razonamiento basado en evidencia**: penalizar la respuesta correcta SIN soporte (combate la sobreinferencia, error nº1).
- **Bucle de reparación**: cada error frecuente activa microlección + reintento isomórfico (ya implementado).
- **Transferencia**: medir en material no visto (banco `PARALLEL_FORM`/`EVALUATION`), no solo en el ítem entrenado.
- **Calibración metacognitiva**: capturar confianza (rasgo META) y contrastarla con el acierto.
- **Claims honestos**: nada por encima de la evidencia (ClaimEngine). Sin "subes tu IQ".
- **Equidad**: contenido `es_neutral`, sin dependencia cultural/regional no declarada.

DoD por ejercicio: ≥5 ítems calibrados (niveles de dificultad), `primaryTrait`+`operation`+familia declarados, distractores → `ErrorType` diagnóstico, scoring determinista, feedback vía `RepairEngine`, cero claims fabricados.

---

## 4. Diseño de los ejercicios (especificación lista para construir)

Cada uno reutiliza `ClosedScoringEngine` + un `AnswerKey`; añade su banco de ítems. Mapear a familias v6 §7.5.

### E1 · Evidence Lock — Inferencia con prueba  (INF, +LIT/SIT/META) · familia `evidence_lock`
- **Entrena:** inferencia legítima anclada en evidencia; combate sobreinferencia (INF_OVER) y subinferencia.
- **Mecánica:** texto 80-140 palabras → elegir la conclusión soportada **y** marcar la(s) frase(s) que la sostienen. Confianza opcional (META).
- **Formato/scoring:** `EvidenceSelection` + `DETERMINISTIC_KEY_PLUS_EVIDENCE`. Cap `unsupported_inference_cap`: opción correcta sin evidencia → 0.5 + `INF_OVER`. `evidenceContribution` = fracción de evidencia requerida.
- **Dificultad (ejes CognitiveDose):** novelty, transferDistance, ambiguity, semanticComplexity; distractor causal atractivo.
- **Excelencia (§7.5):** toda conclusión tiene soporte textual.
- **Semilla:** el ítem "parte meteorológico" ya existe; escalar a ≥6 ítems en 3 niveles.

### E2 · Semantic Surgery — Precisión léxica  (LEX_P, +LEX_D) · familia `semantic_surgery`
- **Entrena:** elegir el término **exacto** entre cuasi-sinónimos (matiz, registro, colocación) → claridad y comprensión fina.
- **Mecánica:** frase con hueco/palabra subrayada → elegir la más precisa entre 3-4 cuasi-sinónimos (no "correcto/incorrecto", sino matices). Variante: "cambia UNA palabra para ganar precisión".
- **Formato/scoring:** `SingleChoice` + `DETERMINISTIC_KEY`. Distractores → `PROD_VAGUE` (vaguedad), `PRAG_TONE` (registro), matiz incorrecto.
- **Dificultad:** semanticComplexity, ambiguity (sinónimos más cercanos).
- **Excelencia:** cambiar una palabra mejora medible la precisión.

### E3 · Compression Ladder — Síntesis bajo restricción  (SUM, +PAR) · familia `compression_ladder`
- **Entrena:** comprimir conservando la tesis bajo límite estricto (jerarquización + fidelidad). Dificultad deseable por restricción.
- **Mecánica:** dado un texto, elegir el mejor resumen a 15 palabras (entre: pierde la tesis / distorsiona / fiel). Variante `Ordering`: ordenar 3 resúmenes de más a menos fiel. Escalera 40→15.
- **Formato/scoring:** `SingleChoice` o `ORDERING_RANK_CORRELATION`. Caps `missing_thesis_cap`; errores `SUM_NO_THESIS`, `SUM_DISTORT`.
- **Dificultad:** workingMemoryLoad (longitud), semanticComplexity.
- **Excelencia:** conserva el núcleo bajo restricción.

### E4 · Argument X-Ray — Anatomía del argumento  (ARG, +CRIT) · familia `argument_xray`
- **Entrena:** separar tesis/premisa/evidencia y localizar el eslabón débil; combate hombre de paja (ARG_STRAW) y salto lógico (ARG_GAP).
- **Mecánica:** argumento corto → identificar el eslabón más débil (premisa no soportada / generalización indebida). Variante: etiquetar cada frase (tesis/premisa/evidencia) vía selección/orden.
- **Formato/scoring:** `SingleChoice` + `DETERMINISTIC_KEY`. Distractores → `ARG_GAP`, `ARG_STRAW`.
- **Dificultad:** ambiguity, semanticComplexity; falacias más sutiles.
- **Excelencia:** detecta el eslabón débil real.

### E5 · Scope & Negation — Alcance y negación  (SYN, +LIT) · familia (sintaxis fina)
- **Entrena:** alcance de negación/cuantificadores (no, todos, solo, salvo, ni…ni) — fuente sistemática de errores de comprensión.
- **Mecánica:** frase con operador → elegir la interpretación correcta del alcance. Ej.: "Todos aprobaron menos Luis" → ¿Luis aprobó?
- **Formato/scoring:** `SingleChoice` + `DETERMINISTIC_KEY`. Distractores → `SYN_SCOPE`, `LIT_DIST`.
- **Dificultad:** syntacticComplexity, operadores anidados.
- **Excelencia:** resuelve el alcance sin inferencias falsas.

### E6 (bonus) · Source Triage — Triaje de fuente  (CRIT, +INF) · familia `source_triage`
- **Entrena:** separar dato verificable / opinión / sesgo / omisión; alfabetización crítica sin credulidad ni cinismo.
- **Mecánica:** noticia breve → clasificar 3-4 afirmaciones por tipo (multi-select o emparejamiento).
- **Formato/scoring:** `SingleChoice`/multi + `DETERMINISTIC_KEY`. Distractores → `CRIT_SOURCE`.
- **Dificultad:** ambiguity, dominio temático controlado.
- **Excelencia:** distingue dato de juicio e interés.

> Mínimo pedido: **5**. Recomendado entregar E1-E5 completos y dejar E6 como stretch. Cada uno con ≥5 ítems calibrados en `es_neutral`.

---

## 5. Plan de implementación por slices (gatear cada uno)

1. **Catálogo de dominio.** `presentation/exercises/LanguageExercise.kt`: `data class LanguageExercise(id, title, subtitle, primaryTrait, family, items: List<DemoItem>)` + un `LanguageExerciseCatalog` que agrupa los bancos por ejercicio (refactor del actual `LanguageDemoBank` → un banco por ejercicio). Tests de catálogo (cada ejercicio ≥5 ítems, rasgos/operaciones declarados, claves bien formadas).
2. **Bancos de contenido E1-E5** (uno por slice o dos juntos): ≥5 ítems por ejercicio, niveles, distractores→ErrorType. Reutiliza el helper `single/evidenceSelection/ordering`. Tests por banco (clave correcta resoluble; distractores mapean a errores esperados).
3. **State holder selectable.** Añadir stage `MENU`; `start(exercise: LanguageExercise)` carga sus ítems; `back()` vuelve al menú. Resumen final por habilidad (ya existe vía `GenomeUiBuilder.evidenceCockpit`). Tests de navegación.
4. **UI menú + integración.** `LanguageRootScreen`: vista `MENU` (lista de ejercicios con título/subtítulo) → al elegir, corre ese ejercicio; al terminar, volver al menú. Verificar desktop+Android, detekt, emulador.
5. **(Opcional) Dificultad adaptativa intra-ejercicio** vía `AdaptationEngine`/`CognitiveDose`, y captura de confianza (META).

Convenciones de calidad ya pagadas (respetar):
- Sin Room/SQLDelight; datos `@Serializable`. Tiempo inyectado (nunca `Clock.System.now()` directo).
- Detekt: arregla findings nuevos; supresiones idiomáticas con comentario (`@file:Suppress("MagicNumber")` en objetos-token; `LongParameterList` en factories de contenido; envolver líneas >120).
- Toda decisión pedagógica en dominio/state holder, NUNCA en la UI.

---

## 6. Criterio de éxito

Al entrar en Lenguaje, el usuario ve un **menú con ≥5 ejercicios distintos**, cada uno una mecánica cognitiva real (no el mismo ejercicio repintado), con ≥5 ítems calibrados, scoring honesto, feedback de reparación y resumen claim-safe por habilidad. Compila desktop+Android, detekt limpio, suite verde, verificado en emulador. Diseño grounded en ciencia del aprendizaje (§3) y alineado con la spec v6.
