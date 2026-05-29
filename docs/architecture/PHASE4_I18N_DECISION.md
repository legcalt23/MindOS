# Decisión — Internacionalización (T4.1)

**Fecha:** 2026-05-29
**Estado:** ⏸️ DIFERIDA por alcance de producto — español-only es el estado **intencional** de v1.
**Disparador:** Fase 4, tarea T4.1.

---

## Estado actual (verificado)

- `LOCALIZATION_PARITY_REPORT.md` documenta explícitamente: **"Language: Spanish (es) only"** y
  "Future multi-language support would require migration to compose-resources".
- Toda la UI está en español, en código Kotlin (parte centralizada en `design/AppStrings.kt`,
  parte aún hardcodeada en pantallas).
- **Paridad iOS/Android es 100%** porque ambas plataformas leen los mismos strings de commonMain
  (no hay divergencia que arreglar).

## Por qué NO se ejecuta la migración i18n en esta fase

1. **No es requisito de v1.** Español-only es una decisión de producto intencional, no un bug.
   Forzar i18n ahora contradice el diseño documentado.
2. **Es XL y de alto riesgo sin red.** Migrar a `compose.components.resources` toca **todas** las
   pantallas (incl. god-screens de 1000+ líneas sin tests UI). Editar a ciegas viola las
   condiciones #3/#4 (verificación tras cada cambio).
3. **Decisión de producto pendiente:** ¿multi-idioma entra en el roadmap? ¿qué idiomas?

## Plan de migración (cuando se apruebe multi-idioma)

1. Activar `compose.components.resources` (el proyecto ya genera `generateComposeResClass`).
2. Crear `shared/src/commonMain/composeResources/values/strings.xml` (es como base/default) y
   `values-en/strings.xml`, etc.
3. Migrar `AppStrings` → `Res.string.*` + `stringResource(...)`, pantalla por pantalla, **detrás
   de tests UI** (dependencia de Fase 5 T5.3) para detectar regresiones de texto/layout.
4. iOS: `Localizable.strings` por idioma (cuando iOS entre en alcance — hoy aplazado).
5. Mensajes de error de dominio (`AuthDiagnostics`, catch blocks): mover a recursos también.

## Deuda relacionada (no bloqueante)

El report indica que muchos strings siguen hardcodeados en pantallas en vez de en `AppStrings`.
Centralizarlos es prerrequisito barato de la i18n futura, pero también toca god-screens → se
agrupa con el troceado de T2.5 (Fase 5-gated) para hacerse con cobertura de tests.

## Acción

- **No se migra i18n en Fase 4.** Español-only permanece como diseño de v1.
- T4.1 queda como tarea de roadmap con plan listo, gated por (a) decisión de producto multi-idioma
  y (b) tests UI de Fase 5.
