# Decisión Go/No-Go — ¿Iniciar Post-MVP? → **NO-GO (bloqueado)**

> **Fecha:** 2026-06-13 · **Estado:** Post-MVP **BLOQUEADO**. MVP-3 código-completo y verde en
> simulación, pero **no validado físicamente** (gate §15 ABIERTO).
> **Documento hermano:** [`PHYSICAL_VALIDATION_GATE.md`](PHYSICAL_VALIDATION_GATE.md).
>
> **Actualización 2026-06-14/15:** P1 (API 36) y P2 (API 30) validados **end-to-end en emulador**
> (DebugExport real: P1 `NANOS_NATIVE`/`LOW`, P2 `MILLIS_DERIVED`/`MODERATE`, ambos `rtNanos>0`
> con aritmética verificada). El gate §15 sigue **ABIERTO (2/4)**: faltan **P3 (gama media/baja,
> jitter)** y **P4 (90/120 Hz)**, que **requieren hardware real** y **no son emulables** (un AVD no
> reproduce jitter de hardware ni el Choreographer a alto refresco; ejecutarlos en emulador
> fabricaría un PASS falso → prohibido por la condición #5). **La decisión NO-GO se mantiene** sin
> cambios — §3 exige los 4 perfiles antes de progresar a Post-MVP. **No queda ningún paso ejecutable
> sin hardware.**

Este registro documenta la decisión exigida por la condición #5 del protocolo de fase
("si aparece un riesgo de seguridad, detente y documenta la decisión") ante la instrucción
"ejecuta la siguiente fase completa del plan".

---

## Contexto

La única fase posterior a MVP-3 (Hito 3) definida en el plan es **Post-MVP** (plan línea 59):

> *"Clasificación psicométrica en los 7 perfiles cognitivos avanzados, batería de 16 ejercicios
> y microprotocolos de transferencia funcional."*

No existe una "fase con prompts" intermedia: entre MVP-3-preliminar y MVP-3-autoritativo solo
está la **ventana de validación física §15**, que es un procedimiento de laboratorio
hardware-dependiente (no código), ya empaquetado en `PHYSICAL_VALIDATION_GATE.md`.

## Decisión: NO iniciar Post-MVP todavía

### Razón 1 — Regla de secuenciación del propio plan (§3, línea 44)

> *"El desarrollo se realiza por capas acumulativas de complejidad, donde **cada hito debe ser
> validado sintética y físicamente antes de progresar al siguiente**."*

- MVP-3 validado **sintéticamente**: ✅ (131 tests speed / 970 `:shared:desktopTest` verdes).
- MVP-3 validado **físicamente**: ⛔ (gate §15 ABIERTO — sin hardware ejecutado).

El plan **prohíbe** progresar a Post-MVP hasta cerrar la validación física de MVP-3. Misma
lógica que el "Go/No-Go" del Día 10 (línea 555: *"No se permite iniciar la implementación de
[la siguiente fase] hasta haber completado con éxito la validación física"*).

### Razón 2 — Riesgo de seguridad / claims (condición #5)

Post-MVP = **clasificación psicométrica en 7 perfiles cognitivos**. Construirla ahora la
asienta sobre un score que sigue siendo **`preliminary = true`** (no calibrado, sin norma,
físicamente no validado). Esto choca con:

- **§9** — prohibición de claims psicométricos / diagnósticos.
- **Acceptance #11** — *"MVP-3 no confirma 7 perfiles prematuramente; los 7 perfiles son
  Post-MVP."*
- **CLAUDE.md / boundaries** — Mindboost no es clínico/diagnóstico; "perfil cognitivo funcional",
  no diagnóstico.

Emitir 7 perfiles "autoritativos" desde datos preliminares es exactamente el fallo que el
rótulo "preliminar" y el gate físico existen para evitar.

### Razón 3 — Condiciones #6 y #7 del protocolo

- #6: "No avances a la siguiente fase." Iniciar Post-MVP **es** avanzar de fase.
- #7: "No implementes tareas que pertenezcan a fases futuras." Cualquier sub-parte de Post-MVP
  (7 perfiles, 16 ejercicios, transferencia) es fase futura respecto a MVP-3.

### Razón 4 — Gate explícito de autorización

El informe de MVP-3 y la memoria de proyecto marcaron Post-MVP como **"no iniciar sin OK del
usuario"**. No hay tal OK registrado para arrancar Post-MVP.

## Qué SÍ se puede hacer ahora (sin violar el protocolo)

1. **Ejecutar el gate §15** (lo hace el usuario con hardware): seguir
   `PHYSICAL_VALIDATION_GATE.md`. Es el único camino que desbloquea Post-MVP.
2. **Validación parcial por emulador** (P1 API 34+, P2 API <34) para verificar la ruta de
   precisión `NANOS_NATIVE` vs `MILLIS_DERIVED` y la sanidad de `rtNanos`; deja P3 (jitter) y
   P4 (alto Hz) pendientes de hardware. Validación **parcial**, no cierra el gate.
3. **Mantener el estado actual** (MVP-3 preliminar) — ya es seguro y enviable.

## Qué desbloquea Post-MVP

Cerrar el gate §15 con PASS (los 4 perfiles, o su desviación documentada) → calibrar el score y
retirar el flag `preliminary` (ver §7 de `PHYSICAL_VALIDATION_GATE.md`) → **entonces** el usuario
puede autorizar Post-MVP, que arrancaría con su propia secuencia hito-a-hito y su propia
validación sintética+física.

## Reversibilidad

Decisión **reversible**: si el usuario decide explícitamente asumir el riesgo y arrancar Post-MVP
en modo claramente experimental/no-autoritativo, puede hacerlo — pero debe ser una decisión suya
informada, no un avance por defecto. Este registro quedaría como contexto de esa decisión.
