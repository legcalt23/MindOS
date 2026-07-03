# MindBoost — Inventario de versiones de scoring (Fase 1.2)

**Fecha:** 2026-07-03 · **Propósito:** barrido completo previo a centralizar las versiones de scoring en
`ScoringVersionRegistry` (Fase 1.2). Cero migración, cero cambio de valor/fórmula: solo mover la fuente de
verdad al registry conservando los strings exactos. Este doc fija QUÉ entra y QUÉ queda fuera (con motivo).

Alcance escaneado: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/**` (producción).

---

## Tier A — Versiones que fluyen a `ScoreProvenance.scoringVersion` (CENTRALIZAR — certeza)

Son las únicas versiones de scoring que hoy se persisten como provenance vía `ScoreResultFactory`. Todas
viven como `const val SCORING_VERSION` en un `*Score.kt` y se pasan a `ScoreProvenance(scoringVersion = …)`.

| Bloque | Símbolo | Valor exacto | Emite provenance |
|---|---|---|---|
| Decisiones | `ReversibilityScore.SCORING_VERSION` | `"e2-reversibility-v1"` | `ReversibilityScore.kt:37` |
| Memoria (WM span) | `MemorySpanScore.SCORING_VERSION` | `"ex1-span-v1"` | `MemorySpanScore.kt:42` |
| Memoria (pares) | `MemoryPairsScore.SCORING_VERSION` | `"ex4-pairs-v1"` | `MemoryPairsScore.kt:33` |
| Memoria (diferido) | `MemoryDeferredScore.SCORING_VERSION` | `"ex3-deferred-v1"` | `MemoryDeferredScore.kt:31` |

No hay literales de versión inline en ninguna construcción de `ScoreProvenance` (todas usan su const). Ningún
scorer setea `itemBankVersion` (default `null`) → sin versiones de banco persistidas todavía.

## Tier B — Versiones de scoring/modelo de bloque en pipeline propio (CENTRALIZAR — decisión CEO "transversal")

No van por `ScoreProvenance` (esos bloques aún no se enrutan por `ScoreResultFactory`), pero SON versiones de
scoring/modelo reales, persistidas en el pipeline propio del bloque (SpeedJson / SessionMetrics / rúbricas).
Por la decisión "no acotar a bloques cableados", entran al registry (string, valor exacto).

| Bloque | Símbolo | Valor exacto |
|---|---|---|
| Velocidad | `SpeedVersions.METRICS_VERSION` | `"1.0.0"` |
| Velocidad | `SpeedVersions.STAIRCASE_VERSION` | `"1.0.0-mvp1"` |
| Velocidad | `SpeedVersions.DIFFICULTY_MODEL_VERSION` | `"1.0.0"` |
| Velocidad | `SpeedVersions.SCORE_VERSION` | `"1.0.0-mvp3-preliminary"` |
| Lenguaje | `LocalStubEvaluatorClient.STUB_VERSION` | `"local-stub-1"` |
| Lenguaje | `LocalStubEvaluatorClient.RUBRIC_VERSION` | `"local-stub-rubric-1"` |
| Lenguaje | `ProfileProjector.ESTIMATOR_VERSION` | `"v6-projector-1"` |

## Tier B-borderline — resuelto (decisión CEO)

| Símbolo | Valor | Decisión |
|---|---|---|
| `CreativityRubricScorer.RUBRIC_VERSION` | `1` (**Int**, no String) | **FUERA** — versión de rúbrica (Int, pipeline propio, no `ScoreProvenance`). El registry queda homogéneo (solo Strings de versión de scoring/modelo). Se centralizará cuando Creatividad tenga scoring real/ProfileSource. |
| `SpeedVersions.DEBUG_EXPORT_VERSION` | `"v0"` | **FUERA** — formato de export de depuración, no fórmula de scoring. |

## Tier C — NO son versiones de scoring (FUERA — documentado, no se tocan)

Preocupaciones distintas (migración de DTO, contenido, build). No pertenecen al `ScoringVersionRegistry`.

- **Schema de serialización de DTO** (`SCHEMA_VERSION = 1`, Int): `SecureDiaryRepository`, `SecureDecisionSessionRepository`,
  `ReversibilityDtos`, `MemoryDtos` (×7), `SpeedJson`, `LanguageJson`, `CreativityRecords`, `CreativityExporter`,
  `LocalLogicTrainingRepository`, y `SCORE_RESULT_DTO_SCHEMA_VERSION` (`ScoreResultDto.kt`). Son marcadores de
  migración por-DTO (`ignoreUnknownKeys`), no versiones de fórmula.
- **Versiones de pool de contenido** (Int): `SpanishMemoryContentData.WORD_POOL_VERSION / PAIR_POOL_VERSION`.
- **Stamp de app/build** (UI): `SpeedExerciseScreen.APP_VERSION = "1.0-mvp0"` (privado, no scoring).
- **IDs de contenido** (no versiones): `ProbeBank` `probe_*_v1` (ids de probe).

## Hallazgos — bloques SIN versión de scoring persistida (no inventar)

- **Enfoque:** `DefaultMetricsEngine` no emite `ScoreProvenance` ni const de versión de scoring. Sin versión persistida.
- **Lógica:** `ReasoningScorer` (ICR) no emite `ScoreProvenance` ni tiene const de versión de scoring. Sin versión persistida.
- **Emociones:** `normalizedScore = null` por diseño; no emite versión de scoring. Sin versión persistida.

Cuando cualquiera de estos se enrute por `ScoreResultFactory` (fases futuras), deberá declarar su versión en el
registry en el mismo cambio.

---

## Plan del diff mecánico (tras confirmar la frontera)

1. Expandir `ScoringVersionRegistry` con Tier A + Tier B (+ borderline según decisión), conservando strings exactos.
2. Cada símbolo local pasa a `const val X = ScoringVersionRegistry.Y` (alias local, `const`→`const`, cero cambio de
   valor ni de referencias/tests).
3. Test guardián: (a) el registry cubre todas las versiones de Tier A/B; (b) cada `ScoreProvenance.scoringVersion`
   proviene del registry (anti-literal-disperso), SIN escanear comentarios/fixtures/golden (no frágil).
4. Gates verdes + `diff-auditor`. Commit `refactor(scoring): centralize scoring version registry`.
