# Language Evidence Integrity Hardening

- **Fecha:** 2026-07-03
- **Estado:** IMPLEMENTADO
- **Ámbito:** MindBoost · Lenguaje → dimensión VERBAL del brain-profile
- **Tipo:** blindaje de integridad de scoring (NO avance de medición oficial)

## Qué es esto (y qué NO es)

Este slice **no** saca VERBAL de `CALIBRATING` ni cablea Lenguaje al perfil. Es un **guard
defensivo**: garantiza que el sistema **nunca** pueda derivar un score/rango/Global Score oficial de
Lenguaje mientras el contenido sea un banco demo/no validado o su provenance sea un stub. Reafirma la
decisión [`LANGUAGE_PROFILE_WIRING_DEFERRAL_2026-07-01.md`](LANGUAGE_PROFILE_WIRING_DEFERRAL_2026-07-01.md):
VERBAL permanece CALIBRATING hasta que exista un banco validado + provenance real.

Motivo de producto: **MindBoost mide progreso real o no mide.** Persistir evidencia de
`LanguageDemoBank` (3 ítems, "Provisional… Replace with the Content OS") y dejar que
`LanguageProfileSource` la use como señal sería **fabricar medición** — el antipatrón que la doctrina
de integridad de banco (INFRA-04, `BankIntegrity.SMALL_UNVALIDATED` → bloqueado del perfil) prohíbe.

## El gate

`LanguageProfileSource` (boundary `profile → core`) es la **única autoridad de elegibilidad de
perfil** para Lenguaje. No existe una bandera `profileEligible` persistida (sería una segunda fuente
de verdad sin escritor real). La elegibilidad se **deriva** en el boundary a partir de:

1. **`contentIntegrity: BankIntegrity`** — default `SMALL_UNVALIDATED` (el banco vivo es demo).
2. **`provenanceValidated: Boolean`** — default `false` (evaluator/rubric/projector son `local-stub`).
3. **Evidencia mínima suficiente** — al menos un trait que superó `E0_CALIBRATING`, o señal de sesión.

`evaluate(userId): LanguageEvidenceDecision` es puro y testeable. Prioridad de bloqueo (el primer
match gana):

| # | Condición | Resultado |
|---|-----------|-----------|
| 1 | `contentIntegrity.blocksProfileEligibility()` | `Blocked(SMALL_UNVALIDATED_BANK)` |
| 2 | `!provenanceValidated` | `Blocked(STUB_PROVENANCE)` |
| 3 | sin traits medidos y sin sesiones | `Blocked(INSUFFICIENT_VALID_EVIDENCE)` |
| 4 | resto | `Eligible(contribution)` |

`contribution()` mapea `Blocked → null` → VERBAL queda CALIBRATING → **cero impacto** en score
oficial, rango o Global Score. Con los defaults de hoy el resultado real es
`Blocked(SMALL_UNVALIDATED_BANK)` (el primer bloqueo vivo es el banco demo).

### Fuente única de verdad del set bloqueante

`BankIntegrity.blocksProfileEligibility()` (en `core/scoring/ScoreTypes.kt`) es el **único** lugar
que define el set `{CONTAMINATED, HOLDOUT_EXHAUSTED, SMALL_UNVALIDATED}`. `ScoreStateMachine`
(`bankContaminated`) y este gate delegan ahí — el set no puede bifurcarse.

## Privacidad

`SessionSummary` ya es privacy-by-design: solo **agregados** (counts, tallies por trait/error), nunca
respuestas crudas ni texto libre. Este slice **no persiste** nada nuevo (el holder demo sigue sin
store) y **no** añade superficie de datos sensibles.

## Prerrequisitos para elevar VERBAL a medición oficial (fase futura)

Cuando se implemente persistencia real (feature, no wiring — requiere review de `efficacy-guardian` +
`privacy-security`), **cada sesión/evidencia debe guardar suficiente metadata para derivar
elegibilidad de forma auditable**, preferiblemente estructurada (ids/versiones/hashes, nunca texto
libre):

- **bank id + bank version** del contenido servido.
- **`BankIntegrity`** del banco en el momento de la captura.
- **evaluator version** (hoy `local-stub-1`).
- **rubric / scoring version** (hoy `local-stub-rubric-1`).
- **projector version** (hoy `v6-projector-1`, con `thetaMean` hardcodeado).
- **provenance validada** (flag derivado de que evaluator+rubric+projector son modelos reales, no stubs).
- (futuro) **estado de anomalías / anti-gaming** si aplica.

Solo con un banco `BankIntegrity.OK` **y** provenance real (`provenanceValidated = true`) el gate
deja pasar la contribución — y aun así seguirá `preliminary` hasta calibración cruzada (DC-2/DC-3).

Dos condiciones adicionales que la revisión de salida (efficacy-guardian + privacy-security) fijó como
requisitos de esa fase futura:

- **`provenanceValidated` debe DERIVARSE, no ser un literal.** El flag NO puede ponerse `true` en el
  call-site "a mano": debe computarse a partir de la metadata persistida (bank id/version + versiones de
  evaluator/rubric/projector, comparadas contra el registro de versiones no-stub), y `thetaMean` debe
  dejar de estar hardcodeado a 0.0 en el projector antes de permitir el path elegible en producción.
- **Cobertura de borrado de cuenta.** Cuando se persista la metadata de elegibilidad, su namespace debe
  entrar en `MindBoostStorageNamespaces.AccountBound` y en el `SecureNamespaceUserDataEraser` (con su pin
  en `AccountBoundErasureCoverageTest`), igual que todo dato por-usuario (GDPR Art. 17; cf. 2026-06-28).

## Verificación

- `LanguageProfileSourceTest` (desktopTest): defaults bloquean banco demo aun con evidencia E3;
  banco válido + provenance stub → `STUB_PROVENANCE`; banco + provenance válidos sin evidencia →
  `INSUFFICIENT_VALID_EVIDENCE`; los tests de matemática declaran banco OK + provenance real.
- Test end-to-end: un repo cifrado real (`LocalSessionSummaryRepository` sobre
  `InMemorySecureKeyValueStore`) con **5 sesiones guardadas** + evidencia E3 → `contribution()` null →
  agregador deja VERBAL `CALIBRATING` con `value == null` y `overall.compositeIndex == null`
  (Lenguaje nunca entra al Global Score).
