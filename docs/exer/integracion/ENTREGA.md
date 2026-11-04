# EXER · Preparación e integración local

**Preparado:** una entrada `exer`, el paquete íntegro de 45 Markdown, el handoff de la auditoría y el paquete de cinco decisiones del CEO. **Alias conversacional `/exer`: configurado en `AGENTS.md` con aprobación del usuario; prueba en nueva interacción pendiente.**

## Entregables

- [SKILL.md](../SKILL.md): entrada breve que carga el cuerpo operativo y la reconciliación actual.
- [Estado y handoff](ESTADO_Y_HANDOFF_2026-09-15.md): repo, memoria, capacidades, límites y discrepancias de la auditoría.
- [Decisiones del CEO](DECISIONES_CEO.md): opciones, recomendación, costes, riesgos y gates de los cinco bloqueos.
- [Alias aprobado](PROPUESTA_ALIAS.md): bloque aditivo exacto aplicado a `AGENTS.md`.
- [Manifiesto de lectura](MANIFIESTO_LECTURA.json) y [snapshot inicial](SNAPSHOT_FUENTES.json): integridad y procedencia incremental; los cambios documentales de esta tarea constan en los resultados de checks.
- [Verificación](VERIFICACION.md) y [checkpoint](CHECKPOINT.md): resultados reales y continuidad.

## Cliente y activación

Se trabaja desde Codex desktop en Windows. La CLI localizada responde `codex-cli 0.137.0`; esa versión no acredita la versión del cliente desktop. Se observaron herramientas reales de ejecución, archivos, web y delegación. No se ha cambiado el modelo ni se certifica una selección concreta mediante texto: el paquete conserva GPT‑6 Astra como destinatario.

Se comprobó ausencia de una carpeta `exer` en las rutas de skills de proyecto y personales consultadas. Existen skills `create-cognitive-exercise`, que se conservaron. `docs/exer` es una preparación portable; no se presenta como ubicación automáticamente descubierta.

La documentación oficial consultada el 15 de septiembre describe `SKILL.md`, descubrimiento local en `.agents/skills` y activación explícita mediante selector/mención de skill. No establece un registro nativo arbitrario `/exer` para este cliente. [Build skills](https://learn.chatgpt.com/docs/build-skills).

Los custom prompts están deprecados y la sintaxis documentada es `/prompts:nombre`; no se adopta ese mecanismo como sustituto. [Custom Prompts](https://learn.chatgpt.com/docs/custom-prompts).

**Decisión recibida el 2026-09-15:** «Sí, añade el alias conversacional /exer». Se añadió el bloque aprobado a `AGENTS.md`. Falta probar una interacción real. El ZIP exige esa comprobación en `03_INSTALACION_Y_ACTIVACION.md` §4: «Prueba en una nueva interacción». No se ha registrado un comando nativo ni instalado una segunda copia como skill.

## Qué se actualizó realmente

Se conservaron las fuentes históricas y se añadió una vista factual del repo/auditoría disponibles. El índice Markdown de memoria enlaza la preparación, `AGENTS.md` incorpora el alias aprobado y el ledger registra la tarea. No se atribuye a esta sesión ninguna ejecución del motor ni resultado humano.

Graphify no pudo consultarse ni actualizarse: su lanzador falla y la instalación Python referenciada no existe. El reporte y la raíz guardada identifican un snapshot previo y la ruta antigua `MindOS`. Su reparación/configuración no forma parte de preparar EXER. Los hechos críticos se contrastaron directamente en archivos.

## Primera invocación para comprobar el alias

```text
/exer continuar
Recupera el checkpoint y el handoff de Enfoque. Prepara el brief de diseño
y compara familias bajo las decisiones vigentes; mantén las propuestas
del CEO pendientes hasta que responda. No implementes el producto.
```

Si el cliente intercepta `/exer`, registrar el resultado antes de acordar otra activación. También se puede solicitar por ruta: «Lee `docs/exer/SKILL.md` y continúa desde su checkpoint». Esto es una instrucción de lectura, no prueba de que exista un comando nativo.
