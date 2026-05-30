# Fase 2 — Tareas diferidas por verificabilidad

**Fecha:** 2026-05-29
**Regla aplicada:** condiciones de ejecución #3/#4 — "tras cada prompt ejecuta tests; si fallan, detente".

Dos piezas de Fase 2 son **de presentación pura sobre composables sin cobertura de tests** y no
son verificables en el entorno actual (Windows: sin emulador Android; `desktopTest` no renderiza
estas pantallas). Editarlas a ciegas introduce riesgo de regresión que no puedo validar. Se
difieren con dependencia explícita del andamiaje de tests UI de Fase 5 (T5.3).

---

## T2.5 — Trocear god screens (DIFERIDA)

**Estado:** gated por T5.3 (tests instrumentados / UI de las pantallas afectadas).

**Pantallas objetivo (líneas):**
- `PublishingUseCases.kt` (1159) — *no es UI; es dominio: se puede trocear con tests unitarios, candidato de mayor prioridad y SÍ verificable.*
- `ProfileScreen.kt` (1135)
- `CajoneraScreen.kt` (1118)
- `GlobalFeedScreen.kt` (1099)
- `WavesChatUserInfoScreen.kt` (1000)
- `HomeScreen.kt` (875)

**Por qué se difiere la parte UI:** no existen tests de Compose para estas pantallas; el único
check disponible (compilación) no detecta regresiones de recomposición, estado o layout. La
secuencia correcta es: 1) escribir tests UI (Fase 5), 2) trocear bajo red de seguridad.

**Decomposición recomendada (cuando se desbloquee):**
- Extraer subcomposables por sección visual + un state holder por pantalla (patrón ya usado por
  `ProfileStateHolder`).
- Mover lógica no-UI (validación, mapeo, orquestación) fuera del composable a funciones puras
  testeables.

**Excepción ejecutable ahora:** `PublishingUseCases.kt` es lógica de dominio (no UI) con tests
existentes (`PublishingPipelineTest`). Su troceado SÍ es verificable y podría abordarse sin
esperar a Fase 5 — se deja anotado como quick-win de arquitectura.

---

## T2.4 — Banner de verificación de email: cableado en UI (DIFERIDO)

**Estado:** la capa de repositorio está HECHA y testeada en Fase 2 (auto-envío en registro +
`sendEmailVerification()` para reenvío + `reloadCurrentUser()`). Falta solo la pieza visual.

**Por qué se difiere el banner:** insertarlo requiere editar `HomeScreen.kt` (875 líneas, sin
tests). Es un cambio aditivo de bajo riesgo, pero no verificable visualmente aquí. Se cablea junto
con el troceado/tests de `HomeScreen` (T5.3).

**Pendiente de producto:** decidir si la verificación de email **gatea** acciones (publicar, DM,
comprar) o es solo un banner no bloqueante. La implementación actual NO gatea (no bloquea a
usuarios), que es el comportamiento seguro por defecto.
