# Decisión: modificación concurrente del working tree durante la Fase 2 (loader migration)

**Fecha:** 2026-07-01
**Contexto:** Migración del sistema de carga global (Fase 2) — swap de loaders de pantalla
completa a `AppBarsLoader`.

## Hecho observado

Durante la ejecución de la Fase 2, un proceso EXTERNO (no este agente) modificó el working
tree en paralelo:

- **15:05** — el directorio `.claude/scripts/` fue vaciado; desapareció `budget-gate.mjs`
  (archivo NO trackeado en git; no recuperable con `git checkout`).
- **15:35** — `shared/src/commonMain/kotlin/com/mindos/app/design/components/AppTextField.kt`
  (archivo NO trackeado, NO en el baseline de Detekt) fue editado; su complejidad ciclomática
  cruzó 15 → 16, provocando el ÚNICO fallo de `:shared:detekt` (`CyclomaticComplexMethod` en
  `AppTextField:191`).

Evidencia: Detekt pasó limpio al final de la Fase 1; `AppTextField` no estaba entre los
findings. Su `mtime` (15:35) cae DENTRO de esta sesión, en un archivo que este agente nunca
tocó. Coincide con el `n8n/Vibe-Kanban agent-runner` documentado, que opera este repo de forma
concurrente.

## Decisión

1. **NO modificar `AppTextField.kt`** para "arreglar" el fallo de Detekt. Razones:
   - No es un archivo de esta tarea (migración de loaders) ni fue creado/editado por este agente.
   - Está siendo editado por otro proceso AHORA MISMO; un edit nuestro podría colisionar con el
     suyo o enmascarar la modificación concurrente.
   - El principio operativo aplica: no sobrescribir un objetivo que no creé y que contradice el
     estado esperado (verde en Fase 1) — se reporta, no se procede.
2. **Las 5 migraciones de la Fase 2 quedan completas y verificadas** de forma independiente al
   finding externo: compilan en Desktop y Android y pasan `:shared:desktopTest`. Los 5 archivos
   migrados están limpios en Detekt (el único finding es el externo de `AppTextField`).
3. **Riesgo pendiente (documentado, no resuelto por diseño):** el gate `:shared:detekt` está en
   ROJO por el `AppTextField` externo. No se puede dejar verde sin tocar un archivo ajeno en
   edición concurrente.

## Recomendación al responsable

- Verificar/pausar el `n8n/Vibe-Kanban agent-runner` (o la sesión concurrente) que edita este
  worktree, para evitar corrupción de estado y colisiones de merge.
- Una vez controlado el proceso concurrente, `AppTextField.AppTextField()` (complejidad 16) se
  desbloquea trivialmente con `@Suppress("CyclomaticComplexMethod")` localizado + comentario
  (idéntico patrón al `@Suppress("LongMethod")` que ya tiene), o partiendo la derivación de
  estado en un helper. NO se hace aquí por la razón (1).
