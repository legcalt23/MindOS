# 17 — Checklist final de blindaje (versión extendida)

> Verificación última antes de declarar el plan "listo para ejecutar" y antes de cualquier GO de perfil.
> Cada item lleva **referencia al documento que lo cubre** y, donde aplica, su **comando/test de validación**.
> Estado: **(hecho)** verificado en repo · **(inferido)** · **(POR_CONFIRMAR_EN_REPO)**.
>
> **Invariantes que se repiten a lo largo del checklist:** `null != 0`; Enfoque/Velocidad sin profile-update
> hasta timebase+jank reales; cero claims de far-transfer NI para negarlos; anti-gaming solo si
> implementado+testeado con adversarios; banco pequeño/no-validado bloquea perfil.

---

## 1. Invariantes técnicas

- [ ] `null != 0` repetido como invariante en INFRA_01, 04_ARQUITECTURA, 13_PROFILE_UPDATE y cada ficha v1.
  → cubre: `INFRA_P0.md §0/§7`, `AUDIT_V2_GROUND_TRUTH.md` ("null vs 0 enforced").
- [ ] Ningún estado no-VALID se serializa como número (property test exigido).
  → `INFRA_P0.md §6` · test `ScoringStateMachineTest.ningunEstadoNoValid_emitePrimarySignalNoNull`.
- [ ] `ProfileUpdateGate`/aggregator consume solo `ScoreResult`, nunca `Double` crudo.
  → `15_ADR` ADR-01 · `INFRA_P0.md §5` (`toModuleContribution`).
- [ ] `ScoringEquations` `0.0`-sentinela (Decisiones) NO se mapea a `value=0` sino a `INVALIDATED`.
  → (hecho) `AUDIT_V2_GROUND_TRUTH.md` (`PHD=0.0` si AGRS>0.75) · `15_ADR` riesgo "0.0 sentinela".
- [ ] `VALID` es el único estado con `updatesProfile=true` (un solo `return` lo pone).
  → `INFRA_P0.md §3.2` regla R-VALID-ONLY · test `validEsElUnicoEstado_conUpdatesProfileTrue`.

## 2. Catálogo

- [ ] El plan **no** lanza los ~71 ejercicios como perfil.
  → `AUDIT_V2_PRODUCTO.md §1` · `14_RUNBOOK §2.2-2.3`.
- [ ] Existe catálogo v1 corto (9) y catálogo v2 experimental.
  → `AUDIT_V2_PRODUCTO.md §6` (v1: Span Dual, Updating Ranura, Grid Binding, Pares A-B/A-C, Episódica Diferida,
    Triaje Reversibilidad, Pausa go/no-go, Recuperación post-error, Detección de lo Inverosímil) y `§7` (v2).
- [ ] Existe lista de retirados y fusionados (≥18 ejercicios).
  → `AUDIT_V2_PRODUCTO.md §8` · `15_ADR` ADR-07.
- [ ] Cada ejercicio tiene veredicto (GO/REVISE/BLOCK/MERGE/REBUILD/RETIRE) y razón.
  → `AUDIT_V2_PRODUCTO.md §2` (tabla de 71 filas).
- [ ] Ningún ejercicio BLOCK/RETIRE aparece como `canUpdateProfile`.
  → test `*CatalogProfileEligibilityTest` (POR_CONFIRMAR_EN_REPO) · `14_RUNBOOK §2.2`.
- [ ] `registry ⟺ implemented` (test de consistencia) verde.
  → (hecho) `ExerciseCatalogConsistencyTest` (focus) · extender a Emociones (`16_PROMPTS S-2`).

## 3. Bloqueos por dispositivo

- [ ] Enfoque y Velocidad **bloqueados para perfil** hasta timebase autoritativa + jank real.
  → `AUDIT_V2_PRODUCTO.md §6` ("Enfoque y Velocidad NO entran a v1") · `15_ADR` ADR-02 · `14_RUNBOOK §2.3`.
- [ ] `frameDropFraction`/`criticalFrameJitter` son medición real, no constantes.
  → (hecho: hoy `0.0`/`false` en `SpeedSessionStateHolder.kt:259-260`) · `INFRA_P0.md` PARTE A.2 ·
    `16_PROMPTS P0-2` · test `TimingPolicyClassifierTest.frameDropSobreUmbral_esDeviceUnreliable`.
- [ ] Existen tests con jank simulado y device-swap.
  → `AUDIT_V2_TECNICO.md §10.1/§10.2` · tests `TimingPolicyClassifierTest`, `DeviceSwapInvarianceTest` (POR_CONFIRMAR).
- [ ] RT-crítico en Focus/Emociones/Decisiones degrada a `DEVICE_UNRELIABLE` sin Android autoritativo.
  → (hecho: Focus usa `MonotonicClock` no-frame) · `16_PROMPTS P0-2b/S-1`.
- [ ] iOS RT-crítico = `DEVICE_UNRELIABLE` hasta portar CADisplayLink.
  → (hecho: `SpeedTimebase.ios.kt:14` fallback) · `15_ADR` riesgo "iOS sin CADisplayLink".

## 4. Anti-gaming

- [ ] Cada detector consumido tiene perfil sintético adversario en test rojo.
  → `INFRA_P0.md` PARTE B · `16_PROMPTS P0-3` · tests `*AntiGaming*Test`.
- [ ] Matriz detector × ejercicio × adversario × resultado esperado.
  → `AUDIT_V2_TECNICO.md §10` (10.1-10.15: jank, timebase/device-swap, rhythm-tapping, waiting-strategy,
    speed-accuracy, memorization, answer-leakage, keyword-stuffing, bank-exhaustion, cultural-prior,
    abandonment, typing-dexterity, button-spam, reveal-all, priori=posterior).
- [ ] Anti-gaming "diseñado" no cuenta como hecho; cobertura ausente bloquea `VALID`.
  → (hecho: ~4/8 reales; cadencia solo Arena; posicional inexistente; keyword-stuffing BLOCKED) ·
    `15_ADR` ADR-03 · `INFRA_P0.md §2.1` (`DetectorCoverage.adequateFor`).
- [ ] AGRS recibe entropía real (no `spatialEntropy=1.0` fijo) donde se liste Decisiones.
  → (hecho: hardcoded en `BayesArenaStateHolder.kt:71`/`ReversibilityTriageStateHolder.kt:73`) ·
    `16_PROMPTS` EX-E1-BAYES/EX-E2-REVERSIBILITY.

## 5. Bancos

- [ ] Banco pequeño/no validado **bloquea perfil** (build fail).
  → `15_ADR` ADR-04 · `16_PROMPTS P0-4` · test `BankValidatorTest.bancoMenorQueMinSize_buildFail`.
- [ ] Holdout servido solo donde corresponde; rotación anti-memorización.
  → (hecho: holdout fantasma en Emotions, `isEval` filtrado) · test `holdoutNuncaServido_enPractica`.
- [ ] Inter-rater ≥0.80 donde el "correcto" es juicio humano.
  → `AUDIT_V2_TECNICO.md §"Bancos a construir/validar"` · test `interRaterMenor080_bancoRechazado`.
- [ ] Balanceo de respuesta correcta verificado (chi²).
  → (hecho: Argument X-Ray correcta="b" en 6/6) · test `balanceoPosicionCorrecta_dentroDeChi2`.
- [ ] Banco objetivo `≥40-50` por ejercicio profile-updating de juicio.
  → `AUDIT_V2_TECNICO.md §"Bancos"` (Lenguaje ≥40-50, Creatividad ≥40, formas paralelas equated).

## 6. Claims

- [ ] Copy-scanner enganchado a `check` (rojo bloquea merge).
  → `15_ADR` ADR-05 · `16_PROMPTS P0-5` · comando `./gradlew :shared:mindboostCopySafetyScanner`.
- [ ] Lexema prohibido bloqueado incluso en negación/compuesto/guion (cero claims NI para negarlos).
  → (hecho: `\b` cruza guion, aprendizaje 2026-06-23) · test `lexemaProhibido_enNegacion_nonClinical_esRojo`.
- [ ] Ningún constructo absoluto en UI sin `VALID`.
  → `AUDIT_V2_TECNICO.md §11.2` (final) · test `AbsoluteConstructGatingTest` (POR_CONFIRMAR).
- [ ] `TransferLabel` por grupo definido.
  → (hecho: `EmotionCatalogIds.TransferLabel{TASK_ONLY/PLAUSIBLE/NOT_DEMONSTRATED/PLACEBO_RISK}`) ·
    `AUDIT_V2_PRODUCTO.md §"Mapa de claims"` (NEAR_TASK_PRACTICE/NO_TRANSFER/EXPERIMENTAL_PROXY).
- [ ] Frases permitidas presentes en pantallas de estado (`AUDIT_V2_TECNICO.md §11.1`).
  → "Señal de práctica", "Estimación preliminar", "Datos insuficientes", "Calibrando tu línea base",
    "No actualizado por calidad de medición", "Medición contaminada — no contabilizada".

## 7. Psicometría

- [ ] `nMin`/`nEffective` por métrica (MinSampleGate); n<nMin ⇒ CALIBRATING (no 0).
  → (hecho: `MIN_N_*=3..5` dispersos, demasiado bajos) · `16_PROMPTS P0-6/INFRA_06` · subir a umbrales §3.
- [ ] Comparativas null si falta una celda (CONGRUENCY/SWITCH/POST_ERROR).
  → `AUDIT_V2_PRODUCTO.md` respuesta 3 · test `ComparativeMetricNullTest` (POR_CONFIRMAR).
- [ ] Mejora de capacidad solo vía sonda UNTRAINED + RCI (`|>1.96|`).
  → `15_ADR` ADR-06 · `16_PROMPTS S-3` · tests `*TransferProbeWiringTest`.
- [ ] Comparativas `nMin ≥ 20`/celda + IC; SSRT `nStops ≥ 30` + `p∈[0.25,0.75]`.
  → `AUDIT_V2_TECNICO.md §"Thresholds (MinSampleGate)"`.
- [ ] Brier/ECE solo agregado inter-sesión (`≥40-60` juicios), nunca standalone.
  → `AUDIT_V2_PRODUCTO.md` respuesta 8 · `AUDIT_V2_TECNICO.md §"MVE por métrica"`.
- [ ] BUR excluye trials `priori=posterior`; `min-n=12`.
  → (hecho: hoy BUR=1.0 cuenta priori=posterior) · `AUDIT_V2_PRODUCTO.md` dissent #8.

## 8. Redundancias / ortogonalidad

- [ ] Clusters de varianza duplicada fusionados o probada `r<0.7` antes de listar dos.
  → `AUDIT_V2_PRODUCTO.md §"Mapa de redundancias"` · `15_ADR` ADR-07.
- [ ] Solape inter-módulo controlado (Lenguaje E4↔Lógica; Decisiones familia bayesiana).
  → `AUDIT_V2_PRODUCTO.md §"Solape entre módulos"` · estudio "ortogonalidad empírica" (`AUDIT_V2_TECNICO.md §"Estudios internos"`).

## 9. Release

- [ ] GO/NO-GO explícito con comando por casilla.
  → `14_RUNBOOK` (§1–§9, cada item con comando Gradle/scanner/test).
- [ ] Criterios de rollback definidos.
  → `14_RUNBOOK §7`.
- [ ] Señales de alerta post-release definidas.
  → `14_RUNBOOK §8`.
- [ ] Riesgos residuales documentados (probabilidad × impacto).
  → `15_ADR §C` (tabla de riesgos) + "riesgos residuales tras arreglos".
- [ ] Dissent log completo (8 desacuerdos con decisión final).
  → `15_ADR §B`.
- [ ] Prompts por fase para agentes de código (con límites + comando de test).
  → `16_PROMPTS` (P0-1..6, P0-2b, P1-A/B, S-1/S-2/S-3, Q-1).

## 10. Cierre

- [ ] `MANIFEST.md` lista todos los documentos del árbol.
  → (POR_CONFIRMAR_EN_REPO).
- [ ] El plan distingue `hecho` / `inferido` / `POR_CONFIRMAR_EN_REPO` en todo documento.
  → presente en `14`, `15`, `16`, `17`.
- [ ] Cada issue P0 tiene criterios de aceptación + tests (nombrados) + DoD + comando de validación.
  → `16_PROMPTS` (cada bloque) + `AUDIT_V2_TECNICO.md §9` (roadmap por issue).
- [ ] Tres bloqueadores estructurales rastreados hasta su INFRA: jank-gate (INFRA_02), anti-gaming (INFRA_03),
      bancos (INFRA_04) + scanner (INFRA_05).
  → `AUDIT_V2_PRODUCTO.md §1` · `15_ADR §C`.

---

**Si todas marcadas:** el plan está blindado para ejecución. **El GO de perfil sigue condicionado** a que la
implementación P0 (INFRA_01..06) + los gates v1 pasen en código real, con los comandos de `14_RUNBOOK §1–§6` en
verde. Hasta entonces: catálogo lanzable como **minijuegos sin profile-update**, con el copy-scanner (§6) como
único gate cuyo fallo bloquea incluso ese lanzamiento (riesgo regulatorio existencial).
