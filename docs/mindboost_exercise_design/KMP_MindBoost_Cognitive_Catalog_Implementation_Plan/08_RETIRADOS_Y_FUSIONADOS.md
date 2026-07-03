# 08 — Retirados y fusionados (detallado)

> **Regla 9 (anti-redundancia) + regla 10 (mejores señales, no más ejercicios).** De ~81 fichas,
> **al menos 18 miden varianza ya cubierta**. El catálogo limpio es más vendible y más defendible que el
> inflado: el brain-profile deja de **contar la misma varianza dos veces** (congruencia ×3, post-error ×3,
> visoespacial ×2).
>
> **Invariante de honestidad que gobierna esta sección:** toda fusión exige un **test concreto** que verifique
> que el clon **NO se expone como ejercicio profile-updating separado**. El mecanismo de gating es el ya
> existente `registry ⟺ implemented` (ver `ExerciseCatalogConsistencyTest` en Enfoque): un generador puede
> existir sin estar en `implemented`; lo que NO puede ocurrir es que el clon aparezca en `implemented` y
> contribuya una `ModuleContribution` propia al perfil. El test rompe el build si eso pasa.
> Recordatorio de invariantes globales: `null != 0`; Enfoque/Velocidad sin profile-update hasta timebase+jank
> reales; cero claims de far-transfer ni para negarlos; anti-gaming solo si implementado+testeado con
> adversarios; banco pequeño/no-validado bloquea perfil.

---

## Retirados (sin suavizar)

| Ejercicio | Módulo | Razón del retiro | Condición de no-retiro (qué lo rescataría) | Test de retiro (verifica que NO actualiza perfil) | Estado |
|---|---|---|---|---|---|
| **E8 Dúo (dual-task)** | Enfoque | El generador NO produce 2ª tarea; `DUAL_TASK_COST` mide ruido puro (flag cosmético sobre go/no-go single-tap) | Capturar `accSecondary` de una tarea secundaria REAL en runtime + `taskBalance`; si el runtime no captura 2 respuestas/trial ⇒ RETIRE, absorber en E9/E11 | `e8Duo_notInImplemented_registry()` (no en `implemented`); `e8Duo_dualTaskCost_isNull_whenSecondaryAccuracyAbsent()`; `e8Duo_doesNotEmitModuleContribution()` | RETIRE salvo rebuild runtime |
| **E5 Cadencia** | Velocidad | La señal central (RT<0 / adelanto) **rompe** `computeRtNanos` (lanza excepción) | `TimingPolicy` con signo (`SignedTiming`, INFRA-02) priorizada en release dedicado | `e5Cadencia_notInImplemented()`; `signedTiming_negativeRt_doesNotThrow()` (regresión del bug); `e5Cadencia_noProfileWrite()` | RETIRE salvo INFRA-02-signo |
| **E7 RSVP** | Velocidad | Bloqueado por jank-gate muerto; SOA = ficción (sin onset por frame) | Onset real por `frameTimeNanos` + gate jank vivo | `e7Rsvp_notInImplemented()`; `e7Rsvp_block_isContaminated_withoutFrameMeasurement()` | RETIRE salvo INFRA-02 |
| **E8 Apuesta Temporal** | Velocidad | Mezcla velocidad+metacognición; premia "cobardía"; léxico de apuesta | Sacar de Velocidad y reubicar en metacognición | `e8Apuesta_notInSpeedImplemented()`; `copyScanner_rejects_apuesta_inSpeedModule()` | RETIRE de Velocidad |
| **E6 Source Triage** | Lenguaje | Claim "fake news" = frontera regulatoria; sin copy-scanner para Lenguaje | Copy separado de "fake news" + scanner Lenguaje (INFRA-05) + decoy bidireccional + banco≥40; si el copy no se separa ⇒ RETIRE | `sourceTriage_copyScanner_rejects_fakeNews_andCompounds()`; `sourceTriage_authorityDecoyHigh_doesNotRaiseSelection()`; `sourceTriage_notInImplemented_untilBankValidated()` | BLOCK (P0) / RETIRE si regulatorio |
| **E7 Lexical Access / E8 Calibrated Comp** | Lenguaje | No existen en código; RT sin timebase / Brier de sesión = ruido (duplican Speed/metacognición) | — | `lexicalAccess_e8CalibratedComp_absentFromCatalog()` (catálogo no los lista) | RETIRE directo |
| **EM5 Decisiones sesgo afectivo** | Emociones | Solapa EM-G + induce claim de far-transfer ("decides mejor") | — | `em5_absentFromCatalog()`; `copyScanner_rejects_decidesMejor()` | RETIRE directo |
| **EM6 Tolerancia a la demanda** | Emociones | Prolongar la espera para medir frustración **ES inducción emocional** (frontera de seguridad); el nombre promete rasgo vital (PLACEBO_RISK) | Prueba documentada de no-inducción (única carga = demora, sin contenido afectivo escalado); abandono no penalizado | `em6_notInImplemented_withoutNonInductionProof()`; `em6_abandonment_doesNotReduceScore()` (Regla #8); `copyScanner_rejects_frustracion_tolerancia()` | RETIRE salvo prueba no-inducción |
| **EM13 Variabilidad (RT_CV)** | Emociones | RT_CV sin timebase = "regulación" falsa | Timebase autoritativa o degradar a feedback de sesión (no perfil) | `em13_rtCv_isDeviceUnreliable_withoutTimebase()`; `em13_noProfileWrite()` | RETIRE / degradar |
| **EX04 Analogías (modo libre)** | Creatividad | El puente sintáctico absurdo pasa el filtro = titular fatal | Conservar SOLO modo proporcional `A:B::C:?` verificable | `ex04FreeMode_notInImplemented()`; `ex04Proportional_hasVerifiableKey()` | RETIRE modo libre |
| **EX08 Improvisación bajo presión** | Creatividad | Mide destreza de tecleo, no ideación; la presión mata la divergencia | Calibrador motor + timebase; reposicionar a anti-bloqueo | `ex08_notInImplemented_withoutMotorBaseline()`; `ex08_typingSpeed_normalizedByIbiBaseline()` (D12.tecleo) | RETIRE salvo calibrador motor |

---

## Fusionados (misma varianza, distinto envoltorio)

> **Patrón de fusión:** el destino absorbe la varianza; el origen puede sobrevivir como **parámetro de
> dificultad** (no como ejercicio). El test de cada fila verifica las DOS cosas: (a) el clon no está en
> `implemented` como id separado, y (b) el clon no emite una `ModuleContribution` propia al
> `MindBoostProfileAggregator`.

| Origen → Destino | Módulo | Razón (varianza compartida) | Qué se conserva del origen | Test concreto que verifica que el clon NO es profile-updating |
|---|---|---|---|---|
| **E5 Conflicto → E2 Filtro** | Enfoque | Mismo `CONGRUENCY_EFFECT` / `DefaultMetricsEngine` | parámetro de proporción de incongruencia | `e5Conflicto_notInImplemented()`; `e5Conflicto_doesNotEmitModuleContribution()`; (si `r(E2,E5)≥0.7`) `congruency_countedOnce_inProfile()` |
| **E3 Multivía → E6 Flanker Express** | Velocidad | `SWITCH_COST` inexistente en speed; misma familia de conflicto | nada hasta tener switch-cost honesto | `e3Multivia_notInSpeedImplemented()`; `e3Multivia_noProfileWrite()`; `switchCost_absent_doesNotFabricateMetric()` |
| **EX6 Detección Cambio → EX5 Grid** | Memoria | EX6 es el modo change-detection de EX5 | modo `changeDetection` dentro de EX5 | `ex6_isModeOfEx5_notSeparateId()`; `ex6_doesNotEmitOwnContribution()` |
| **EX8 Nombres-Rostros → EX4 Pares** | Memoria | Caso particular de asociativa; EX8 tiene sesgo étnico fuerte | nada hasta banco de caras diverso/licenciado | `ex8_notInImplemented_withoutDiverseFaceBank()`; `ex8_mergedIntoEx4_noOwnContribution()` |
| **EM8 / EM12 → EM1 Pausa** | Emociones | Mismo go/no-go afectivo (un parámetro de ratio / anticipaciones secundarias) | EM8=ratio no-go; EM12=anticipaciones (secundaria) | `em8_em12_notInImplemented_asSeparateProfile()`; `em8_em12_doNotEmitContribution()` |
| **EM10 → EM3 Separación** | Emociones | Idéntico + dependencia de timebase | parámetro de presión temporal | `em10_isParamOfEm3_notSeparateId()`; `em10_noProfileWrite()` |
| **EM11 / EM15 → EM2 Post-error** | Emociones | Mismo `POST_ERROR_RECOVERY`, solo varía densidad de "harder" | densidad de trials difíciles | `em11_em15_notInImplemented()`; `postErrorRecovery_countedOnce()`; `em11_em15_noOwnContribution()` |
| **EM14 → EM4 Calibrador** | Emociones | Duplicado exacto (mismo símbolo + métrica) | nada (duplicado) | `em14_absentFromImplemented()`; `em14_doesNotEmitContribution()` |
| **EM7 → EM13** | Emociones | Mismo `REGULATION_STABILITY` (y EM13 a su vez degradado a feedback) | nada | `em7_absentFromImplemented()`; `regulationStability_notDoubleCounted()` |
| **EX02 Rotación → EX01 Supresión** | Creatividad | EX02 computa la misma fluidez sin clasificador categorial | clasificador categoría↔léxico (si se construye) o RT por idea | `ex02_notInImplemented_asSeparateFluency()`; `fluency_countedOnce_inCreativityProfile()` |

---

## Test canónico de fusión (plantilla reutilizable)

Todo `*_noProfileWrite()` / `*_doesNotEmitContribution()` sigue esta forma (KMP `commonTest`/`desktopTest`):

```kotlin
// Verifica que un clon fusionado NUNCA contribuye al perfil cognitivo como id separado.
class MergeContractTest {

    // (a) El clon no está expuesto como ejercicio jugable-y-puntuable independiente.
    @Test
    fun clone_notInImplementedRegistry() {
        // registry ⟺ implemented es la única fuente de verdad de "lo wired".
        assertFalse(ExerciseCatalog.implemented.contains(ExerciseId("focus-e5-conflicto")))
        // si existe el generador, debe vivir solo en domainReady/pendingUiIntegration, NUNCA en implemented.
        assertTrue(ExerciseCatalog.domainReady.contains(ExerciseId("focus-e5-conflicto")))
    }

    // (b) El clon no emite una ModuleContribution propia: la varianza la aporta SOLO el destino.
    @Test
    fun clone_doesNotEmitModuleContribution() {
        val session = runSyntheticSession(exerciseId = "focus-e5-conflicto", seed = 42)
        val contributions = profileAggregatorInputsFor(session)
        // El clon no aparece como fuente; CONGRUENCY_EFFECT solo entra una vez (vía E2).
        assertTrue(contributions.none { it.sourceExerciseId == "focus-e5-conflicto" })
    }

    // (c) Anti-doble-conteo: la métrica compartida se cuenta EXACTAMENTE una vez en el perfil.
    @Test
    fun sharedMetric_countedOnce_inProfile() {
        val profile = buildProfileFrom(listOf("focus-e2-filtro", "focus-e5-conflicto"))
        val congruencySources = profile.dimensionSources(Dimension.INHIBITION)
            .filter { it.metricKey == MetricKey.CONGRUENCY_EFFECT }
        assertEquals(1, congruencySources.size) // jamás 2 (sería doble conteo de varianza)
    }
}
```

**Por qué este test es la red de seguridad correcta:** el repo ya usa `ExerciseCatalogConsistencyTest`
para atar `registry ⟺ implemented` (rompe el build si un generador render-compatible se expone sin
renderer, o si un id se expone sin generador). El test de fusión extiende ese mismo principio al perfil:
un clon puede tener generador y tests de dominio (`domainReady`), pero **nunca** puede aparecer en
`implemented` ni emitir `ModuleContribution`. Así, "compila" y "tiene generador" siguen sin equivaler a
"actualiza el perfil".

---

## Impacto

El módulo **Emociones colapsa de 15 a 3 limpios** (EM1 Pausa, EM2 Post-error, EM3-tras-banco-validado).
El brain-profile deja de **contar la misma varianza dos veces**:

| Varianza | Ejercicios que la fabricaban | Tras fusión |
|---|---|---|
| Congruencia (`CONGRUENCY_EFFECT`) | E2, E5 (Enfoque) ×2; (E3-Vel, E6-Vel familia) | 1 fuente (E2 Enfoque + E6 Velocidad, ortogonalidad probada o merge) |
| Post-error (`POST_ERROR_RECOVERY`) | EM2, EM11, EM15 ×3 | 1 fuente (EM2) |
| Go/No-Go afectivo | EM1, EM8, EM12 ×3 | 1 fuente (EM1) |
| Visoespacial / change-detection | EX5, EX6 ×2 | 1 fuente (EX5 con modo) |
| Regulation stability | EM7, EM13 ×2 | 1 fuente (EM13 degradado a feedback) |
| Calibrador | EM4, EM14 ×2 | 1 fuente (EM4) |

Cada fila de la tabla anterior tiene su par de tests `*_notInImplemented()` + `*_doesNotEmitContribution()`,
de modo que el colapso es **verificable por build**, no solo documental. Toda fusión exige que el clon
**no** se exponga como ejercicio profile-updating separado.
