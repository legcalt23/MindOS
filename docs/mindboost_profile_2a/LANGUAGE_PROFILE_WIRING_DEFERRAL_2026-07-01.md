# Decisión: NO cablear Lenguaje al perfil cognitivo en Fase 1 (diferido)

- **Fecha:** 2026-07-01
- **Estado:** ACEPTADA (diferir)
- **Ámbito:** MindBoost · Lenguaje → dimensión VERBAL del brain-profile
- **Tipo:** decisión de eficacia/honestidad + integridad de banco (MVP Fase 1, Prompt 3)

## Contexto

La Fase 1 (Prompt 3) proponía "cablear el store local de Lenguaje" para que la dimensión
VERBAL dejara de estar permanentemente en `CALIBRATING`. La auditoría estimó esto como un
cambio de bajo coste ("1 línea"). **Esa estimación era incorrecta.** Al trazar el flujo real:

1. **El lado de lectura ya existe y es honesto.** `LanguageProfileSource`
   (`profile/data/adapter/LanguageProfileSource.kt`) consume `EvidenceRepository` +
   `SessionSummaryRepository`, gatea por `EvidenceState.E1_TRAINED_IMPROVEMENT`, marca todo
   como `preliminary=true` y devuelve `null` si no hay evidencia medible → CALIBRATING. Correcto.

2. **Los repos locales cifrados existen.** `LocalEvidenceRepository` /
   `LocalSessionSummaryRepository` (`language/data/local/LocalLanguageRepositories.kt`)
   persisten sobre el `SecureKeyValueStore` (namespace `mindboost_language`, ya declarado en
   `MindBoostStorageNamespaces`). Minimizan (solo abstracciones/resúmenes, nunca texto crudo).

3. **PERO la sesión de Lenguaje NO persiste nada en producción.**
   `LanguageRootScreen` construye `LanguageSessionStateHolder(userId, menuMode = true)` — sin
   store ni repositorio. El holder acumula `packets = mutableListOf<EvidencePacket>()`
   **solo en memoria** (línea ~70) y `backToMenu` los descarta. Es una pantalla auto-descrita
   como "Provisional, minimal screen … demo".

4. **El contenido es un banco DEMO no validado.** Los ítems provienen de `LanguageDemoBank` /
   `DemoItem`. Alimentar el perfil desde un banco no validado contradice la doctrina de
   integridad de banco del propio repo (INFRA-04: los bancos placeholder/demo son
   `SMALL_UNVALIDATED` y el `ScoreStateMachine` los BLOQUEA del perfil; cf. aprendizaje
   2026-06-30 "generador placeholder degenerado → SMALL_UNVALIDATED, no PROCEDURAL").

## Decisión

**Se DIFIERE el cableado de Lenguaje al perfil.** No se hace en Fase 1.

Razones (cualquiera de ellas basta):

- **No-op:** cablear solo el lado de lectura (pasar los `Local*Repository` en
  `RememberBrainProfileStateHolder` en vez de `null`) leería un store vacío → `contribution()`
  devuelve `null` → VERBAL sigue CALIBRATING. No cambia nada observable; sería wiring muerto.
- **Riesgo de fabricación:** hacerlo "funcionar" exige persistir evidencia desde un banco DEMO
  no validado, lo que fabricaría señal de perfil desde contenido sin validar — exactamente el
  antipatrón que la doctrina de bancos prohíbe.
- **Es feature, no wiring:** convertir el holder demo en uno que persista (inyectar repos +
  store, `append` por respuesta, `save` de `SessionSummary` en DONE, plumbing del store desde
  el composable) es una construcción de feature con implicaciones de privacidad (nueva
  superficie de persistencia de datos sensibles) y de eficacia — no una tarea de "wiring" de
  Fase 1.

## Prerrequisitos para cablearlo (fase futura)

1. Un **banco de contenido de Lenguaje validado** (no `LanguageDemoBank`), que pase el
   `BankValidationGateTest` / contrato de integridad de banco.
2. **Persistencia en producción**: inyectar `LocalEvidenceRepository`/`LocalSessionSummaryRepository`
   (namespace `mindboost_language`) en `LanguageSessionStateHolder`; `append` de cada
   `EvidencePacket` gated y `save` del `SessionSummary` al terminar.
3. Revisión de **`efficacy-guardian`** de que la evidencia persistida es honesta y gated, y de
   `privacy-security` sobre la nueva persistencia.
4. Solo entonces, pasar los repos reales (en vez de `null`) en `RememberBrainProfileStateHolder`.

## Verificación

Sin cambios de código en esta fase → sin tests asociados (N/A). El lado de lectura ya está
cubierto por los tests existentes de `LanguageProfileSource`. Esta decisión revierte/corrige
la estimación "bajo coste" de la auditoría (§9) para Lenguaje.
