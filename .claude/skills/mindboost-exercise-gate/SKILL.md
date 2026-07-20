---
name: mindboost-exercise-gate
description: >
  The mandatory pipeline + hard gates for building a world-class MindBoost cognitive exercise
  that HONESTLY trains and measures. Use whenever creating, designing, reviewing, scoring, or
  wiring a cognitive exercise, training task, assessment, adaptive-difficulty loop, or anything
  that will feed the cognitive profile. Trigger on "ejercicio", "exercise", "entrenamiento",
  "training task", "scoring", "dificultad adaptativa", "assessment", "baseline", "profile".
  Load BEFORE designing so the exercise is evidence-grounded, measured honestly, and never
  overclaims — the discipline is what separates a real cognitive trainer from brain-game slop.
---

# MindBoost — Gate de ejercicios (pipeline obligatorio)

> **Por qué existe.** El mejor entrenador cognitivo del mundo no es el que tiene más ejercicios
> — es el que **solo shippea los que miden honestamente y mejoran algo real**, y **rechaza** el
> resto. Este gate hace obligatorio ese workflow. Saltárselo produce brain-game genérico que
> sobrepromete; la evidencia (Owen 2010, Simons 2016, Melby-Lervåg — en tu corpus) es tajante.

## Verdad científica no-negociable (léela antes de diseñar)

- El brain-training mejora de forma fiable la **tarea entrenada** (near-transfer). El
  **far-transfer** a inteligencia general / vida diaria es débil-a-nulo en la mejor evidencia.
- Por tanto: un ejercicio se justifica por su **near-transfer plausible** a una tarea real que el
  usuario valora (estudio, trabajo, decisiones, foco), **nunca** por "sube tu capacidad
  cognitiva general / IQ". Claims prohibidos: clínicos, IQ, transferencia lejana no respaldada
  (ver `shared/.../core/copy/ForbiddenLexicon.kt` + scanners de build).
- Lenguaje permitido: "perfil cognitivo funcional", "práctica", "progreso vs tu propia línea
  base", "dificultad adaptativa", "orientativo".

## Fuentes de verdad (no dupliques — enlaza)

- **Charter de ejercicios:** `docs/mindboost_exercise_design/00_CHARTER.md` + el diseño por dominio
  (`03_1_Enfoque.md` … `03_8_Creatividad.md`).
- **Contrato de calidad / niveles de readiness:** `docs/mindboost_exercise_design/EXERCISE_QUALITY_CONTRACT.md`
  (L1 jugable local · L2 · L3 MVP-ready · L4 profile-updating). **Nunca confundas "jugable" (L1)
  con "listo para MVP" (L3) ni con "actualiza el perfil" (L4)** — son niveles con gates distintos.
- **Rigor charter de medición:** `CLAUDE.md §2` + `.claude/skills/ceo/references/MINDBOOST_PRODUCT_DOCTRINE.md`.
- **Ledger de auditoría (OBLIGATORIO):** `MINDBOOST_AUDIT_LEDGER.md` — léelo al empezar y
  actualízalo antes de cerrar (validador: `node scripts/validate-audit-ledger.mjs`).

## Pipeline obligatorio (en orden; cada flecha es un gate que puede DETENER)

```
1. cognitive-science-principles (skill)   → mecanismo + honestidad de transferencia
2. Evidencia real: MCP de literatura       → search_papers / search_trials (paradigma validado
   (search_papers/search_trials)             + tamaño de efecto real). deep-research si es amplio.
3. create-cognitive-exercise (skill)       → objetivo · mecánica · ítems paramétricos
4. design-adaptive-scoring (skill)         → scoring + staircase/IRT + baseline
5. psychometrician (agente)          [GATE] → mates del scoring (Brier, RT drift-diffusion,
                                              fiabilidad, gating de impulsividad). Falla ⇒ para.
6. efficacy-guardian (agente)        [GATE] → ¿entrena/mide lo que dice? Veredicto de eficacia.
7. cognitive-transfer-agent (agente)       → near-transfer honesto a tarea real (o lo declara nulo)
8. Anti-gaming red-team              [GATE] → detector con call-site vivo + adversario sintético
                                              que dispara + honesto que NO dispara (CLAUDE.md §2)
9. molvex-design-language + dataviz + motion-experience → feel premium (charts firma con dataviz)
10. mindboost-experimentation (agente)     → plan A/B vs baseline (sin esto, "mejora" = opinión)
11. qa-release-gate (skill) + diff-auditor → veredicto final
12. Ledger: reconciliar + checkpoint + SHA (validate-audit-ledger.mjs)
```

Máximo 2–3 agentes concurrentes; si una lente devuelve vacío, reintenta solo esa.

## Los 10 gates de calidad de una feature (de CLAUDE.md §1 — todos requeridos)

No está lista sin: (1) objetivo cognitivo claro · (2) mecánica de entrenamiento · (3) método de
scoring · (4) adaptación de dificultad (o razón explícita de estática) · (5) feedback al usuario ·
(6) eventos de analítica · (7) revisión de seguridad de claims · (8) tests de la lógica importante ·
(9) revisión de privacidad de datos cognitivos · (10) ruta de verificación.

## Gates de medición honesta (rigor charter — bloquean el perfil)

Un ejercicio solo **actualiza el perfil** (L4) vía un `ScoreResult` sellado que pasó TODOS sus gates:

- **`null ≠ 0`** — dato insuficiente/ inválido ⇒ `CALIBRATING` / "—", nunca un 0 fabricado (ni un
  máximo fabricado: auditar fórmulas `1−x` / `100·(1−a)·(1−b)` alimentadas por estadísticos que
  devuelven 0.0 en lista vacía — ver aprendizajes 2026-07-15).
- **MinSampleGate** — muestra mínima; el sample-count es parámetro OBLIGATORIO, no derivado.
- **Timebase autoritativo** para señal RT-crítica (`CHOREOGRAPHER_FRAME` + `NANOS_NATIVE`; con
  fallback la sesión es jugable pero EXCLUIDA del perfil).
- **BankIntegrityAuthority** — integridad de banco derivada, nunca literal `BankIntegrity.*` en el
  call-site; fail-closed a `SMALL_UNVALIDATED`.
- **Anti-gaming con `DetectorCoverage` declarada** por el propio detector; dirección fail-closed
  (retener el update, nunca acusar por señal no inferible).
- `dataSufficiency` (muestra) ⊥ `confidence` (estabilidad) — nunca mapear uno desde el otro.
- UI honesta: placeholders no se renderizan como hechos.

## Gates de build (entregar comandos al usuario si el daemon del IDE está ocupado)

`:shared:desktopTest` · `:shared:detekt` (ratchet one-way) · `:shared:compileAndroidMain` /
`:androidApp:compileDebugKotlin` · copy/terminology scanners (variante del source-set tocado) ·
`:shared:koverVerifyDesktop` si aplica. No avanzar con un gate en rojo. Persistencia cognitiva
sensible = `@Serializable` + `SecureKeyValueStore` (no Room/SQLDelight); solo resúmenes.

## Definición de "hecho" para un ejercicio

Declara explícitamente el **nivel de readiness alcanzado** (L1/L3/L4 del quality contract), NO
"terminado" a secas. Cierre: qué cambió · archivos · tests · cómo verificar · near-transfer
declarado · nivel de readiness · riesgos/follow-up · entrada en el ledger.
