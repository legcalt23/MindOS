# V1_09 — Detección de lo Inverosímil (Rareza)

**Módulo:** Creatividad · **Verdict:** REBUILD · **Prioridad:** P1 · **Profile-update:** **solo-calibrando** · **TransferLabel:** NEAR_TASK_PRACTICE / `TASK_ONLY`

> **Mapa hecho / inferido / por_confirmar.** Este ejercicio (EX06 del audit) es **SOLO DISEÑO**: el
> código real de Creatividad tiene 2 ejercicios (EX01, EX02), loop batch-submit, scorer de 2 señales
> (`fluency`/`elaboration`), y la **Metric Integrity Layer BLOQUEADA** (`scrutinize()` siempre devuelve
> `Blocked`). Ground-truth `04_8_Creatividad_audit.md` EX06. **[HECHO]** = verificado en auditoría;
> **[INFERIDO]** = diseño coherente; **[POR_CONFIRMAR_EN_REPO]** = símbolo/ruta a verificar. Rutas
> probables bajo `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/creativity/`.

> **Invariantes transversales que este ejercicio repite (no negociables):**
> - **null != 0.** `rarityScore` con cobertura de banco baja ⇒ `CALIBRATING`, JAMÁS `0.0`. La UI pinta
>   `—`/"calibrando", nunca un cero como "poca originalidad".
> - **Cero claims de far-transfer, NI para negarlos.** Nada de "eres más creativo/innovador"; tampoco la
>   negación. `rarityScore` NUNCA se muestra como "originalidad" absoluta. Etiqueta obligatoria: "proxy
>   interno, no diagnóstico".
> - **Anti-gaming solo cuenta si está implementado + testeado con adversarios.** Hoy el único anti-cheat
>   real es `elapsed<0`; la capa de integridad está **BLOQUEADA por diseño**. Los detectores de este
>   ejercicio (plausibilidad mata rareza vacía, near-duplicate, banco oculto) son **diseño hasta estar
>   implementados + testeados con `KeywordStuffer`/perfil-absurdo**; mientras tanto, NO son cobertura.
> - **Banco pequeño / no-validado BLOQUEA perfil.** El banco de frecuencias es el **punto único de
>   fallo**: sin cobertura auditada declarada, `CALIBRATING`. Es la razón principal del `solo-calibrando`.
> - **Enfoque/Velocidad sin profile-update hasta timebase+jank reales.** No aplica RT aquí (loop batch,
>   `RT_IRRELEVANT`); pero la misma doctrina de honestidad aplica: sin validación empírica del banco, no
>   hay `VALID`.

---

## 1. Objetivo cognitivo realista

Práctica de **originalidad como rareza relativa a un banco**, con **restricción de plausibilidad**. Es el
diseño de creatividad **más defendible sin IA**: el **absurdo bien formado NO gana**. NO mide "creatividad
general" ni promete ser más innovador. Operacionalización honesta de "rareza" como **cola de frecuencia**,
no originalidad absoluta.

| Sub-constructo | Operacionalización | Al perfil |
|---|---|---|
| Rareza relativa | `media(1 − freq_rel)` contra banco auditado | **solo-calibrando** (banco no validado) |
| Plausibilidad | pertenencia a banco-de-válidas | gate de "el absurdo no gana" |
| Cobertura de cola | fracción de cola usada | si baja ⇒ `CALIBRATING` |
| Fluidez/elaboración | distintas / palabras-por-idea | descriptivo, fuera del eje |

---

## 2. Métrica primaria

`rarityScore × plausibilityRate`, donde:
- `rarityScore = media(1 − freq_rel)` contra un **banco de frecuencias auditado**;
- `plausibilityRate` prioriza el **banco de respuestas válidas** sobre heurística.

Es un **proxy, no una medida absoluta de originalidad**. La multiplicación por plausibilidad es la clave
anti-absurdo: **rareza vacía no puntúa**.

```kotlin
// POR_CONFIRMAR_EN_REPO: RarityScorer (creativity/domain/scoring/) — Metric Integrity hoy BLOCKED
fun scoreRarity(resp: RarityResponse, bank: FrequencyBank): CreativityScoreResult {
    // GATE 0 — cobertura de banco auditada declarada : null != 0
    if (bank.coverage < MIN_COVERAGE) {
        return CreativityScoreResult.calibrating(reason = BANK_COVERAGE_LOW)   // rarityScore = null
    }
    val valid = resp.ideas.filter { idea ->
        // plausibilidad: banco-de-válidas PRIORITARIO sobre heurística
        bank.isInValidSet(idea) || (heuristicPlausible(idea) && /* heurística = provisional NO puntuable */ false)
    }
    val deduped = rejectNearDuplicates(valid)                  // overlap léxico, no solo distinct()
    if (deduped.size < MIN_VALID) {
        return CreativityScoreResult.calibrating(reason = INSUFFICIENT_VALID)
    }
    val rarity = deduped.map { 1.0 - bank.relativeFrequency(it) }.average()   // [0,1]
    val plausibility = deduped.size.toDouble() / resp.ideas.size             // [0,1]
    val practice = 0.5 * rarity * plausibility +
                   0.3 * norm(deduped.size) +                                // validRareCount
                   0.2 * coverageOfTail(deduped, bank)
    // SIEMPRE solo-calibrando en v1: señal exploratoria, nunca VALID hasta auditar banco
    return CreativityScoreResult.calibratingSignal(
        signal = Signal(key = RARITY_PROXY, value = practice, unit = RATIO_0_1),
        label = "proxy interno, no diagnóstico",
    )
}
```

---

## 3. Métricas secundarias

Descriptivas / gating; ninguna actualiza perfil en v1:

| Métrica | Definición | Rol |
|---|---|---|
| `coverageOfTail` | cobertura de cola del banco usada | si baja ⇒ `CALIBRATING` |
| `absurdosRechazados` | tasa de absurdos filtrados | salud anti-gaming |
| `validRareCount` | nº de ideas válidas y raras | componente de score |
| `plausibilityRate` | válidas / total | gate "absurdo no gana" |
| `fluency` / `elaboration` | distintas / palabras-por-idea | descriptivo, fuera del eje rareza |

---

## 4. Gates de calidad (precondición de listado)

1. **Banco de frecuencias auditado** con **cobertura declarada**; si la cobertura es baja ⇒
   `CALIBRATING` (no puntuar como válido). El banco es el punto único de fallo.
2. **Plausibilidad prioriza el banco de válidas** sobre heurística (la heurística da rareza provisional
   **NO puntuable**).
3. **Verificado contra perfil absurdo**: un generador de respuestas raras-pero-absurdas debe puntuar
   **bajo** en test (la multiplicación `rarity × plausibility` lo garantiza).
4. **Rotación dura + banco oculto** (anti-memorización): feedback solo binario; la cola nunca se revela.
5. **`minBankSize`** para evitar familiaridad; **near-duplicate rejection** (overlap léxico, no solo
   `distinct()`).
6. **`CreativityMetricIntegrityLayer` deja de estar BLOCKED [POR IMPLEMENTAR]** y gana detectores reales
   (near-duplicate/overlap, banco oculto) testeados con perfiles sintéticos adversarios.

**Tabla de estados (jugable ≠ actualiza-perfil):**

| Estado | Disparador | Jugable | updatesProfile | Copy |
|---|---|---|---|---|
| `CALIBRATING` (default v1) | siempre (banco no validado empíricamente) | ✅ | ❌ | "proxy interno, no diagnóstico" (`—`) |
| `BANK_CONTAMINATED` | cobertura baja · holdout agotado | ✅ | ❌ | "estimación no fiable: contenido repetido" |
| `SUSPECTED_GAMING` | KeywordStuffer / basura-rara (cuando el detector exista) | ✅ | ❌ | neutro, sin acusar |

> **`VALID` no existe en v1.** Solo pasa a `VALID` tras **auditar la cobertura del banco** y **verificar
> contra el perfil absurdo**; nunca muestra "originalidad" como número absoluto.

---

## 5. Anti-gaming

| Ataque | Mecanismo | Detector (estado real) |
|---|---|---|
| `KeywordStuffer` / basura-rara | rareza alta, plausibilidad baja | `rarityScore × plausibilityRate` ⇒ el absurdo no gana — **[POR IMPLEMENTAR]** (Integrity BLOCKED) |
| `BankMemorizer` | memoriza la cola si se revela | **banco oculto + rotación dura** — **[POR IMPLEMENTAR]** |
| `NearDuplicateFlooder` | sinónimos/casi-duplicados como ideas "distintas" | **near-duplicate rejection** (overlap, no `distinct()`) — **[POR IMPLEMENTAR]** |
| `BankReveal` | intenta filtrar el banco | feedback solo binario; cola no expuesta — **[INFERIDO]** |

> **Regla #2 explícita:** hoy `CreativityResponseValidator` solo chequea `elapsed<0`/vacío/`minIdeas`, y la
> Metric Integrity Layer está **BLOQUEADA por diseño** (`CreativityAiLayers` "would score anti-gaming
> signals. BLOCKED"). TODOS los detectores de arriba son **diseño hasta estar implementados + testeados**
> con `KeywordStuffer` y el perfil-absurdo; por eso el ejercicio es REBUILD y `solo-calibrando`.

---

## 6. Banco / generador

Banco de **frecuencias léxicas/conceptuales auditado** (cobertura declarada) + **banco de respuestas
válidas** (para plausibilidad). `minBankSize` para evitar familiaridad. **Es el punto único de fallo**
(limitación honesta): si la cobertura o las frecuencias están sesgadas, toda la métrica es ruido.
Dependencia cultural/léxica ⇒ **declarar mono-idioma**. Holdout implícito (cola oculta); rotación dura de
prompts; inter-rater sobre plausibilidad. **[POR_CONFIRMAR_EN_REPO]** `EX06_FREQUENCY_BANK` con lookup
determinista de `freq_rel` — no existe hoy.

---

## 7. UI / copy permitido

- "Práctica de fluidez/rareza en esta tarea"
- "Respuestas poco comunes pero plausibles"
- "Rareza respecto a respuestas comunes, no medida de originalidad absoluta"
- "Calibrando — proxy interno, no diagnóstico" · medidor común/raro

---

## 8. Copy prohibido (bloquear en copy-scanner)

"eres más creativo/innovador", "tu originalidad", "creatividad general", "pensamiento lateral",
"desbloquea tu mente", "pensamiento original" como medida absoluta, cualquier far-transfer (vida
real/innovación/IQ) **y su negación**. NUNCA llamar `rarityScore` "originalidad". **[POR_CONFIRMAR_EN_REPO]**
no existe copy-scanner para `creativity/**`: crear `creativityTerminologyScanner` espejo de los de arena/
emotions, enganchado a `check`, es parte del DoD.

---

## 9. Criterio EXACTO para actualizar perfil

**Solo-calibrando en v1**: la calibración del banco de frecuencias **no está validada empíricamente**, así
que entra como **señal exploratoria** que **no actualiza perfil**. Pasa a `VALID` SOLO tras: (a) auditar la
cobertura del banco (declarada y suficiente); (b) verificar contra el **perfil absurdo** (rareza vacía no
puntúa); (c) tener la Metric Integrity Layer **desbloqueada** con near-duplicate + banco oculto testeados.
**Nunca** muestra "originalidad" como número absoluto. Creatividad permanece `CALIBRATING` en el perfil
global (correcto hoy) hasta tener señales con holdout.

---

## 10. Tests obligatorios (enumerados)

**Unit (`RarityScorerTest`):**
1. `rarityScore_equalsMeanOfOneMinusRelFreq`
2. `plausibility_prioritizesValidBank_overHeuristic`
3. `lowCoverage_returnsCalibrating`
4. `nullNeverZero_calibratingHasNullRarity`
5. `rarityTimesPlausibility_isPrimaryProduct`
6. `heuristicOnly_isProvisionalNotScorable`

**Property-based (Kotest `Arb`):**
7. `forAll_anyResponse_rarityScoreInZeroToOne`
8. `forAll_anyResponse_plausibilityRateInZeroToOne`

**Adversarial (`RarityAdversarialTest`):**
9. `absurdProfile_highRarityLowPlausibility_scoresLow` (plausibilidad mata la rareza vacía)
10. `keywordStuffer_garbageRare_doesNotScore`
11. `nearDuplicates_notCountedAsDistinctIdeas`
12. `bankMemorizer_hiddenBankPlusRotation_breaksStrategy`

**Integración (`RarityIntegrationTest`):**
13. `hiddenBank_notLeakedInFeedback`
14. `rotation_effective_acrossSessions`
15. `calibrating_whenCoverageLow`
16. `profileNeverUpdated_inV1_alwaysCalibrating` (invariante `solo-calibrando`)

**Copy / safety:**
17. `copyScanner_blocksOriginalidad_andFarTransfer`
18. `feedback_carriesProxyNotDiagnosticLabel`

---

## 11. Riesgo residual

El **banco de frecuencias es el punto único de fallo**: si su cobertura o las frecuencias están sesgadas,
toda la métrica es ruido (mitiga: cobertura declarada + auditoría + `CALIBRATING` si baja). **Dependencia
cultural/léxica** ⇒ declarar mono-idioma. **Plausibilidad heurística débil**: un absurdo bien formado
podría pasar el filtro heurístico ⇒ por eso la plausibilidad prioriza el **banco de válidas** y la
heurística no puntúa. **Rareza léxica ≠ originalidad semántica** (declarado, pero el usuario puede leerlo
como capacidad ⇒ etiqueta "proxy, no diagnóstico" obligatoria). Por todo esto entra **solo-calibrando**.

---

## 12. Definition of Done

- **Banco de frecuencias auditado** (cobertura declarada) implementado (tests 1-3, 15).
- **Plausibilidad prioriza banco-de-válidas**; `rarityScore × plausibilityRate` **verificado contra
  perfil absurdo** (tests 2, 9).
- **Metric Integrity Layer desbloqueada** con near-duplicate + banco oculto + rotación, testeada con
  `KeywordStuffer`/`BankMemorizer` (tests 10, 11, 12, 13, 14).
- Consume `INFRA_04` (banco oculto + rotación), `INFRA_01` (ScoreResult/estados), `INFRA_03` (banco) —
  **[POR_CONFIRMAR_EN_REPO]**.
- copy-scanner VERDE + etiqueta "proxy, no diagnóstico" (tests 17, 18); **`solo-calibrando` invariante**
  (test 16); compila ambos targets; Detekt VERDE.
