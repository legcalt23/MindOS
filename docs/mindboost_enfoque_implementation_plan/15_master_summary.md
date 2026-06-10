# 15 · Resumen Maestro

## Qué es esto

Plan de implementación completo y quirúrgico para **MindBoost · Enfoque**, un
sistema de **entrenamiento de control atencional** medible, adaptativo,
periodizado y honesto, dentro del proyecto KMP MindOS. **Aún no hay código de
producción**; esto es la hoja de ruta ejecutable.

## Arquitectura recomendada (en una frase)

Feature aislada en `:shared/features/mindboost/focus` siguiendo Clean Architecture
del repo (domain/data/presentation), con **dominio y motores 100% Kotlin puro**
(métricas, adaptación, ejercicios), persistencia **local-first cifrada**,
inyección por **`FocusGraph`**, presentación con **StateHolder**, integración por
**un único `HomeDestination.MindBoost`**, y **cero dependencias nuevas**.

## Jerarquía de construcción (no negociable)

```
dominio → métricas → adaptación → evaluación → E1 (slice) → núcleo
→ sesiones/periodización → personalización → avanzados → reportes → validación
```

Nunca empezar por UI, minijuegos ni gamificación. El sistema mide antes de jugar
y nunca afirma mejora real fuera de los **probes no entrenados** (RCI).

## Las 5 garantías de honestidad (restricciones duras)

1. **RT mediano + RT-CV** (no media); lapsos y fatiga siempre cuentan.
2. **Separación training / evaluation / calibration**; adaptación solo en training.
3. **Probes reservados disjuntos**; nunca se entrenan; son la única evidencia fuerte.
4. **Dificultad = vector multieje**, no velocidad sola; se mueve un eje por vez.
5. **Sin transferencia lejana prometida**; toda métrica con incertidumbre visible.

## Fases (resumen)

| Fase | Entregable | Estado |
|------|-----------|--------|
| 0 | Auditoría de arquitectura | ✅ (doc 01) |
| 1 | Dominio puro | ⏳ pendiente |
| 2 | Motor de métricas | ⏳ |
| 3 | Motor adaptativo | ⏳ |
| 4 | Baseline/evaluación | ⏳ |
| 5 | Vertical slice E1 (lazo completo + UI mínima) | ⏳ |
| 6 | Núcleo E2–E5 | ⏳ |
| 7 | Sesiones/periodización | ⏳ |
| 8 | Personalización | ⏳ |
| 9 | Avanzados E6–E13 | ⏳ |
| 10 | Reportes y transferencia | ⏳ |
| 11 | Validación fuerte | ⏳ |

## Prioridades

1. **Medición correcta** (F2) — sin esto, todo lo demás es decorado.
2. **Adaptación segura** (F3) — fatiga/impulsividad sobre progresión.
3. **Slice E1 end-to-end** (F5) — prueba que el lazo entero funciona.
4. Todo lo demás se construye sobre esos tres pilares.

## Primeros 5 pasos concretos

1. Crear el esqueleto de paquetes `features/mindboost/focus/{domain,data,
   presentation}` en `commonMain` (vacío, compilando).
2. **Implementar Fase 1 (dominio)** con el **Prompt 1** de la doc 13.
3. Verificar `:shared:compileKotlinDesktop` + `:shared:desktopTest` verdes y
   cero imports de Compose/Firebase en el paquete.
4. **Implementar Fase 2 (métricas)** con el **Prompt 2**, con tests de vectores
   conocidos (incluido SSRT del paper).
5. **Implementar Fase 3 (adaptación)** con el **Prompt 3**, con la tabla de
   decisión y el test anti-oscilación.

## Siguiente instrucción ideal para empezar

> **"Implementa la Fase 1 siguiendo `/docs/mindboost_enfoque_implementation_plan`
> (usa el Prompt 1 de la doc 13). No avances a la Fase 2 hasta que el dominio
> compile en desktop, los value objects validen invariantes y no haya imports de
> Compose/Firebase en el paquete."**

## Dudas críticas a resolver antes de F5/F9

Ver doc 14 §5. Las dos más bloqueantes:
- **¿Local-only en MVP o cloud sync desde el inicio?** (recomendado: local-only).
- **¿iOS corre ejercicios desde F5 o se difiere con stubs?** (recomendado: stubs,
  igual que el resto de actuals iOS del repo).

## Documentos del plan

`00`–`15` en esta carpeta. Empieza por `00_README_index.md`, luego `01` y `02`,
y ejecuta con `13`.
