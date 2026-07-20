# Teoría de tokens (referencia agnóstica)

Los 4 `.md` de esta carpeta vienen del skill `design-system` de ui-ux-pro-max. Son **teoría de
arquitectura de tokens**, framework-agnóstica. Úsalos como referencia conceptual, NO como algo a
ejecutar: los ejemplos usan sintaxis CSS (`--color-x`, `var(...)`) solo como notación — **en este
repo los tokens son Kotlin**, no CSS. No crees archivos CSS ni `design-tokens.json`.

## La arquitectura de 3 capas → cómo vive YA en MindOS

| Capa (teoría) | Qué es | Dónde vive en el repo |
|---|---|---|
| **Primitive** | Valores crudos (hex, dp, ms) | Los literales `Color(0xFF…)` dentro de `design/AppColors.kt` y las escalas en `AppSpacing`/`AppShapes`/`AppElevation`/`AppAnimation` |
| **Semantic** | Rol, no apariencia (`primary`, `surface`, `error`, acento por-dominio) | Los `val` nombrados de `AppColors` (`NeuGreen`=marca, `Surface`, `Error`, `FocusSignal`…) y `MindOSTheme`/`NeuColorScheme` |
| **Component** | Tokens específicos de un componente | Constantes locales del componente (p. ej. los de `PhotoUploadLoader`, `AppTextField`) que referencian la capa semántica |

Regla del repo ya vigente: `Color(0xFF…)` solo en `AppColors.kt`/`MindOSTheme.kt`. Eso ES la
frontera primitive→semantic. La lección accionable de esta teoría: cuando definas un token nuevo,
nómbralo por **rol** (semántico), no por color, y que los componentes consuman el rol — nunca el
primitive directo. El ADN de Molvex (SKILL.md §2) es la capa semántica de marca sobre esto.
