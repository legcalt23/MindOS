# INFRA-04 · ItemBankValidation (banco grande, holdout, rotación, balanceo, inter-rater)

> Issue P0. Fuente: `AUDIT_V2_TECNICO.md` (§9 INFRA-4, backlog científico bancos),
> `AUDIT_V2_GROUND_TRUTH.md` (item banks).
> Leyenda: `hecho` · `inferido` · `por_confirmar` (`POR_CONFIRMAR_EN_REPO`). Rutas no verificadas →
> `ruta probable`.

---

## Título
INFRA-04 · Contrato `ItemBank` + `BankValidator` con gate de build

## Prioridad
P0 (bloqueante de claim de perfil para todo ejercicio profile-updating con banco).

## Objetivo
Que **ningún ejercicio que actualice perfil** sirva un banco pequeño, sin holdout activo o sin
balanceo (Invariante #e). Hoy (`hecho`): Logic 6 ítems fijos en orden fijo; Language 36 (6×6) fijos;
Emotions PAUSA(24)+SEPARATOR(40) con holdout `isEval` reservado a "Ola-4" que **nunca se sirve**
(`EM3SeparadorGenerator` filtra `{!it.isEval}`); Creativity 14 prompts; Decisiones procedural sin
banco autorado. **Cero discriminability psicométrica, cero inter-rater, cero culturalLoad, cero
exposure, cero validación empírica en todo el sistema.**

## Archivos probables (`ruta probable`, salvo anclas `hecho`)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/bank/ItemBank.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/bank/BankValidator.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/bank/ExposureTracker.kt` (NUEVO)
- Anclas existentes (`hecho`) a migrar al contrato:
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/domain/content/LogicContentProvider.kt`
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/**/data/bank/*Bank.kt`
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/content/EmotionContent.kt`
    (`SEPARATOR_ITEMS`, `PAUSA_WORDS`, `isEval`)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/creativity/**` (prompts)
- Gate de build (`hecho` patrón): `shared/build.gradle.kts` (espejo de `arenaTerminologyScanner`).

## Dependencias
- INFRA-01 `ScoreResult`: `BankIntegrity.{OK, SMALL_UNVALIDATED, HOLDOUT_EXHAUSTED, PROCEDURAL,
  CONTAMINATED, NOT_APPLICABLE}` ya modelado; un banco no-OK fuerza `BANK_CONTAMINATED`/degradación.
- INFRA-03 D5/D7: exposure tracking compartido (`priorExposures`, holdout servido).

## Estructura por ítem (metadatos obligatorios)
| Campo | Obligatorio en | Hoy | Nota |
|---|---|---|---|
| `id` | todos | `hecho` existe | estable, para exposure tracking |
| `difficultyLevel` (1..5) | todos | `hecho` existe | |
| `constructTag` | todos | `hecho` existe | TraitCode/LogicSkill/EmotionCategory |
| `partition` (`TRAINING`/`HOLDOUT`/`UNTRAINED`) | profile-updating | `por_confirmar` (solo `isEval` emotions, inactivo) | el HOLDOUT debe SERVIRSE en eval |
| `correctAnswerPosition` | bancos de elección | `hecho` existe (`correctAnswerId`) | objeto de balanceo |
| `interRaterAgreement` (≥0.80) | bancos de juicio | `absent` (`hecho`) | descartar ítem si <0.80 |
| `exposureCount` | profile-updating | `absent` (`hecho`) | `ExposureTracker`, no por ítem estático |
| `priorKnowledgeTag` (HIGH_PRIOR) | bancos textuales/trivia | `por_confirmar` | activa flag de conocimiento previo |
| `culturalLoad` | bancos textuales | `absent` (`hecho`) | balanceo cultura/género/etnia |

## Reglas del contrato
1. **Tamaño mínimo (`minBankSize`)** por tipo de ejercicio (ver doc 11). Bancos de juicio/elección
   `≥40-50`; perceptuales procedurales N/A (no memorizable como texto).
2. **Holdout SERVIDO**: la partición `HOLDOUT`/`UNTRAINED` debe servirse en evaluación; reservarla y
   no servirla (estado actual de emotions) cuenta como **holdout fantasma** y NO satisface el gate.
3. **Rotación**: `selectNext()` nunca repite ítems del holdout en la partición de práctica; rota
   cross-sesión; sin orden fijo memorizable.
4. **Balanceo de respuesta correcta**: la posición de la opción correcta distribuida (test chi² dentro
   de ±umbral); mata estrategia posicional ("siempre b" — `hecho`: Language E4 correcto="b" en 6/6).
5. **Inter-rater ≥0.80** para bancos de juicio; ítems con acuerdo <0.80 se excluyen.
6. **Exposure tracking** por usuario: `ExposureTracker` registra ítems servidos; alimenta D5/D7 y la
   detección de `HOLDOUT_EXHAUSTED`.

## Gate de build (build falla si…)
- un ejercicio **profile-updating** declara un banco con `size < minBankSize`, **o**
- sin partición `HOLDOUT` activa servida, **o**
- un banco de juicio sin `interRaterAgreement ≥ 0.80`, **o**
- posición de respuesta correcta no balanceada (chi² fuera de umbral).
> Un ejercicio NO profile-updating (minijuego/`calibrating` permanente) puede usar banco pequeño,
> pero entonces NO puede tener `updatesProfile=true` (cruza con INFRA-01).

## Criterios de aceptación
- Build falla con banco `< minBankSize` o holdout no servido en un ejercicio profile-updating.
- `selectNext()` nunca sirve un ítem de holdout en práctica (test).
- Partición UNTRAINED servible para la sonda de transferencia (Memoria).
- Distribución de posición correcta dentro de ±chi² (test).
- Inter-rater <0.80 ⇒ ítem rechazado del banco efectivo (test).

## Tests obligatorios
- "holdout nunca servido en práctica".
- "balanceo de posición correcta dentro de ±chi²".
- "banco < minSize en ejercicio profile-updating ⇒ build fail".
- "inter-rater < 0.80 ⇒ ítem excluido".
- "exposure tracking marca HOLDOUT_EXHAUSTED tras agotar pool".

## Riesgos
- Migrar bancos autorados al contrato puede romper tests deterministas existentes
  (`ClosedScoringEngineTest`, generadores). `inferido` → migrar por módulo, no en bloque.
- `minBankSize` sin datos de pilotaje es un número de partida (`por_confirmar`): calibrar con el
  estudio "curva de memorización por tamaño de banco" (doc 11 / backlog científico).
- Decisiones es procedural (`hecho`): aplica `PROCEDURAL`, no `minBankSize`, pero igualmente sin
  validación de contenido — no exige holdout textual pero sí `seed≠0`+rotación.

## Definition of Done
- Contrato `ItemBank`/`BankValidator`/`ExposureTracker` implementado.
- Gate de build enganchado a `check`; rojo bloquea merge.
- Bancos profile-updating migrados o explícitamente marcados no-profile.
- `:shared:desktopTest` + gate verdes.

## Comando/check de validación (`por_confirmar` nombres de tarea)
- `:shared:desktopTest` · `:shared:check` (gate de banco) · `:shared:detekt`.
