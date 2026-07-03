# INFRA-03 · AntiGamingFramework con perfiles sintéticos adversarios

> Issue P0. Fuente: `INFRA_P0.md` (PARTE B, D1–D12), `AUDIT_V2_GROUND_TRUTH.md` (anti-gaming),
> `AUDIT_V2_TECNICO.md` (§9 INFRA-3, §10 test plan).
> Leyenda de evidencia por afirmación: `hecho` (verificado en ground-truth) · `inferido` ·
> `por_confirmar` (`POR_CONFIRMAR_EN_REPO`).

---

## Título
INFRA-03 · `AntiGamingFramework` + suite adversarial sintética (D1–D12)

## Prioridad
P0 (bloqueante transversal; desbloquea el claim "anti-gaming implementado+testeado" — Regla #2).

## Objetivo
Convertir los detectores hoy dispersos y los **diseñados** en un contrato común
(`GamingDetector` → `DetectorSignal` → `GamingVerdict`) ejercido por una **suite de perfiles
sintéticos adversarios** (`hecho`: hoy solo Arena tiene `RedTeamAdversarialTest`, fuera de
cognitive-training). **Invariante #d:** un detector solo cuenta si está implementado **y** un perfil
sintético adversario lo dispara en test; "diseñado" no cuenta. **Invariante #c:** ningún detector
genera copy de far-transfer/clínico/IQ. **Invariante #a (`null != 0`):** la acción anti-gaming NUNCA
escribe un score bajo — reclasifica a `LocalFeedbackOnly`/`Invalidated` con barras `null`/`—`.

## Archivos probables (`ruta probable`, salvo anclas `hecho`)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/antigaming/Detector.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/antigaming/AntiGamingFramework.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/antigaming/detectors/D{01..11}*.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/core/antigaming/AntiGamingRedTeamTest.kt` (NUEVO, D12)
- Anclas existentes (`hecho`, NO reescribir, ENVOLVER):
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/SsrtCalculator.kt` (D2)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/exercise/TrialClassifier.kt` (D4)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/metrics/MetricCalculator.kt` (D3/D4)
  - `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/scoring/ScoringEquations.kt` (D6, `entropyFactor` muerto)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/simulation/SyntheticUserRunner.kt` (base de perfiles)

## Dependencias
- INFRA-02 `TimingPolicy`/`TimebaseGuarantee` (D4/D10 consumen estado de timebase).
- INFRA-01 `ScoreResult`/state machine (la acción del detector se traduce a `ScoreState`).
- INFRA-04 `ItemBank` exposure tracking (D5/D7 requieren `priorExposures`/holdout).

## Cambios técnicos
1. **Contrato común** (`inferido` del diseño): `TrialTelemetry`, `interface GamingDetector`,
   `DetectorSignal(triggered, strength 0..1, recommendedAction, evidence)`,
   `enum GamingAction { NONE, FLAG_ONLY, WITHHOLD_PROFILE, INVALIDATE }`, agregador
   `GamingVerdict` (acción más severa gana).
2. **Envolver lo que ya existe** sin reescribirlo: D2 (`SsrtCalculator.trustworthy`), D4-trial
   (`TrialClassifier`/`MetricCalculator` anticipación).
3. **Implementar lo ausente**: D1, D3, D5, D6, D7, D8, D9, D10, D11.
4. **D6 revive el término muerto**: calcular entropía REAL de respuesta y alimentar el
   `entropyFactor` del AGRS (hoy `spatialEntropy=1.0` hardcoded — `hecho`).
5. **D12 = harness**: perfiles sintéticos + `HonestUserProfile` (control FP) + `SafetyExitProfile`
   (control Regla #8).

## Catálogo de adversarios (uno por detector)

Para cada adversario: **qué observa · threshold inicial · falsos positivos · test unit · test
adversarial (rojo si puntúa alto) · acción sobre `ScoreResult` · estado real**.

| ID | Adversario (perfil) | Detector | Qué observa | Threshold inicial | Falsos positivos | Acción `ScoreResult` | Estado real (Regla #2) |
|---|---|---|---|---|---|---|---|
| D1 | `RhythmicTapper` | RhythmTapping | CV de intervalos inter-respuesta + corr(RT, dificultad)≈0 | `iriCv < 0.08` Y `\|corr\|<0.1` sobre ≥12 trials | usuario regular en ISI fijo → exigir además accuracy no-covaría | `WITHHOLD_PROFILE` | `ABSENT` (cadencia solo Arena synth) → **REVISE crítico** |
| D2 | `WaitingStrategist` | WaitingStrategy | inflación go-RT en stop-signal; `p(respond\|signal)∉[0.25,0.75]` | `pRespond<0.25` (reusar `SsrtCalculator`) | participante cauto → solo invalida SSRT, no bloque | `WITHHOLD_PROFILE` (solo SSRT) | `IMPLEMENTED+TESTED` → solo envolver |
| D3 | `SpeedAccuracyHacker` | SpeedAccuracyHack | punto fuera de SAT: RT ultrabajo + accuracy≈azar | `medianRt<chanceFloor && acc≤chance+0.05` | experto rápido-y-preciso NO dispara (acc≈azar requerido) | `INVALIDATE` | `PARTIAL` (ceiling ×0.5, no invalida) → REVISE |
| D4 | `ButtonSpammer`/`RespondToAll`/`AnticipatorProfile` | TooFastToProcess | RT < piso fisiológico; tasa de bloque | trial `RT<anticipationFloorMs`; bloque `anticipationRate>0.30` | lapsus motor aislado → tasa de bloque, no trial | trial descartado (existe) + bloque `WITHHOLD_PROFILE` (nuevo) | `IMPLEMENTED+TESTED` trial / `PARTIAL` bloque |
| D5 | `BankMemorizer`/`RoteMemoryProfile` | Memorization | caída RT + acc→1.0 en ítem repetido, isomorfo plano | repetido≥2ª vez: `rtDrop>40% && acc→1.0 && isomorphGain<0.05` | aprendizaje genuino de la regla → exige isomorfo plano | `WITHHOLD_PROFILE` + señal de rotación | `ABSENT` (sin exposure tracking) → **BLOCK** claim Logic/Language |
| D6 | `LeftStubborn`/`ButtonSpammer` (posicional) | PositionBias | entropía de `responseKey` vs `correctKey` | `responseEntropy<0.4 bits && acc≤expectedFromBias+0.05` | bloque con correcto mayoritario de un lado → comparar vs `correctKey` real | `INVALIDATE` (revive `entropyFactor` AGRS) | `ABSENT` (`spatialEntropy=1.0` muerto) → **REVISE crítico** |
| D7 | `BankGrinderProfile` | RepeatedBankExploit | fracción de sesión = ítems ya servidos | `reexposedFraction>0.5` + ganancia concentrada en reexpuestos | banco pequeño legítimo → retener perfil, no culpar | `WITHHOLD_PROFILE` + fuerza rotación | `ABSENT` (sin holdout activo) → **BLOCK** claim progreso |
| D8 | `KeywordStuffer`/`KeywordSpammerProfile` | KeywordStuffing | densidad de keywords sin estructura; type-token ratio bajo | `TTR<0.4 && keywordDensity>0.3 && ideaCount inflado por duplicados` | estilo conciso legítimo → exige TTR bajo Y duplicación | `INVALIDATE` métrica fluidez/originalidad | `ABSENT + BLOCKED por diseño` → **BLOCK** score creatividad |
| D9 | `RevealAllNoRead`/`PeekerProfile` | LowEffortReveal | `revealedExplanation` antes de responder | `revealedThenCorrectFraction>0.5` | estudia explicación tras fallar → solo reveal **antes** del commit | `WITHHOLD_PROFILE` (solo trials revelados) | `ABSENT` (sin orden reveal↔commit) → REVISE |
| D10 | `MinEffort`/`ButtonMasher` | MotorContamination | ráfagas multi-touch, sub-50ms, fuera de zona | `multiTouchTrials/total>0.15` o IRI<50ms intra-trial | temblor/accesibilidad → reclasificar `DEVICE_UNRELIABLE`, NO fraude | `WITHHOLD_PROFILE` (calidad), nunca penaliza | `ABSENT` (sin multi-touch capture) → REVISE |
| D11 | `Quitter` (cognitivo) vs `SafetyExit` (emocional) | AbandonmentManipulation | abandono selectivo covaría con dificultad | `corr(dificultad, abandono)>0.5` | **Regla #8: en Emotions/frustración NUNCA dispara** (desactivado por diseño) | `WITHHOLD_PROFILE` (solo no-emocional) | `PARTIAL` → REVISE + Regla #8 bloqueante |
| D12 | `AbsentNoSearch` + harness completo | SyntheticAdversaries | N/A — genera telemetría y verifica que D1–D11 atrapan + `HonestUser` no dispara | la suite ES el test | — | red anti-regresión; sin ella ningún detector cuenta | `ABSENT` (solo Arena) → **BLOCK** transversal |

> `AbsentNoSearch` (búsqueda visual, E7-Focus): responde "ausente" sin escanear → debe aplanar la
> pendiente de set-size o producir `Contaminated`; vive como perfil dentro de D12 sobre el ejercicio
> de búsqueda (`por_confirmar` que el generador exponga `setSize` por trial).

## Tabla de veredictos de partida (`hecho`, Regla #2)
De 12 detectores, **2 cuentan hoy** (D2 waiting, D4-trial too-fast); D5/D7/D8/D12 son **BLOCK**;
D1/D3/D6/D9/D10/D11 son **REVISE**. El conjunto está en **REVISE/BLOCK** hasta que D12 esté verde.

## Criterios de aceptación
- Cada detector consumido por un ejercicio tiene su perfil sintético que DEBE puntuar bajo o producir
  `SUSPECTED_GAMING`/`Contaminated`/`Invalidated` (nunca `Valid`).
- API uniforme: `framework.evaluate(profile.synthesize(seed)) -> GamingVerdict` enganchable desde el
  test de cada ejercicio.
- `HonestUserProfile` NO dispara ningún detector (control de falsos positivos: `GamingAction.NONE`).
- `SafetyExitProfile` en módulo emocional NUNCA penaliza (Regla #8).
- La acción severa más alta gana en el agregador; ningún path escribe score bajo (Invariante `null!=0`).
- D6 inyecta entropía REAL al `entropyFactor` del AGRS (deja de pasar `spatialEntropy=1.0`).

## Tests obligatorios
- Unit por detector (caso limpio idle + caso disparo) — uno por D1..D11.
- `AntiGamingRedTeamTest` (D12): `every adversarial profile is caught by its detector`;
  `honest user triggers no detector`; `safety exit in emotional module never penalized`.
- Cobertura mínima por celda de la **MATRIZ_ANTI_GAMING** (doc 10): cada par detector×ejercicio
  marcado con un perfil debe tener test rojo-si-puntúa-alto.
- Regla #3: bajo `MIN_*` (`MIN_IRI=12`, `MIN_CHOICE=10`, `MIN_REPEATED=4`) → `idle()`, no afirma.

## Riesgos
- **Falsos positivos** en D1/D6 con usuarios genuinamente regulares → mitigar con doble condición
  (entropía baja Y accuracy no-covaría). `inferido`.
- D5/D7 dependen de exposure tracking que hoy NO existe (`hecho`): sin INFRA-04 quedan inertes →
  el claim Logic/Language sigue **BLOCK** por construcción (resultado correcto, no bug).
- D8/D10 requieren capturas (TTR, multi-touch) que ningún ejercicio realiza hoy (`hecho`): decidir
  por Regla #10 si la captura extra se justifica antes de implementarlos.

## Definition of Done
- Contrato + D1–D11 implementados; D2/D4 envueltos; D12 suite verde con ≥12 perfiles.
- Cada detector listado en la MATRIZ con su par ejercicio×adversario y test rojo-si-alto.
- Ningún detector emite copy; toda acción reclasifica a estado no-`Valid` con `null`/`—`.
- `:shared:desktopTest` verde; Detekt sin nuevos findings (suite pesada en `desktopTest`).

## Comando/check de validación (`por_confirmar` nombres de tarea)
- `:shared:desktopTest` (suite adversarial).
- `:shared:detekt`.
