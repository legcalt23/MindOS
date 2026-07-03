# Workflows de seguridad agentic

## 1. Auditoria defensiva A1

1. El Orchestrator valida objetivo, alcance, exclusiones y stop conditions.
2. Selecciona un lider y como maximo los consultores necesarios, sin delegacion recursiva.
3. Cada auditor opera en solo lectura y devuelve findings mas receipt.
4. El Orchestrator valida receipts, elimina duplicados y separa evidencia de hipotesis.
5. Security Lead prioriza riesgo. Documentation Sanitizer comprueba DLP antes de devolver el informe.

Salida: findings `confirmed`, `needs_review` o `not_actionable`; nunca una afirmacion sin referencia o proof gap.

## 2. Remediacion A2 con segregacion

1. Un finding validado recibe aprobacion humana con rutas, limites y pruebas.
2. El Remediator valida el task envelope y adquiere el writer lock.
3. Ejecuta `secret-path-guard` antes de cada ruta y aplica un cambio minimo.
4. Ejecuta solo pruebas locales aprobadas y genera hashes de evidencia.
5. Libera el lock mediante registro append-only y entrega receipt con `result=needs_review`.
6. Security QA Verifier revisa diff y pruebas en solo lectura.
7. Security Lead decide: aceptar, devolver al Remediator con nueva aprobacion, o aceptar riesgo humano.

Stop inmediato: lock ocupado, ruta no aprobada, candidato a secreto, salida del workspace, mutacion inesperada, test destructivo o necesidad de red.

## 3. Incidente defensivo

1. Incident Commander define severidad, reloj, propietario y canales autorizados.
2. Blue Team y Forensics trabajan sobre copias aprobadas y metadatos minimizados.
3. No se borran artefactos ni se alteran originales; se registran hashes y cadena de custodia.
4. Las acciones de contencion que cambien estado requieren aprobacion humana separada y no se ejecutan desde este paquete.
5. El informe distingue observacion, inferencia, decision y riesgo residual.

## 4. Dual-use A3 en laboratorio

1. Verificar propietario, activos, fechas, objetivo, tecnicas prohibidas e impacto maximo usando la plantilla RoE.
2. Obtener dos aprobaciones humanas distintas y trazables.
3. Usar laboratorio desechable sin egress, sin credenciales reales y con datos sinteticos.
4. Ejecutar solo validadores no destructivos incluidos en el alcance.
5. Detener ante cualquier ruta a terceros, persistencia, exfiltracion, evasion o indisponibilidad.

## 5. Handoff y receipts

- Cada agente recibe solo el envelope y evidencias necesarias.
- Cada handoff incluye un receipt validado; no se transfieren prompts crudos.
- `evidence_hash` identifica un artefacto local aprobado, no sustituye la revision humana.
- Un receipt invalido, incompleto o contradictorio bloquea el workflow.
- Ningun agente puede cambiar su nivel de autonomia o ampliar el alcance recibido.

## 6. Reintentos y fallo cerrado

Un reintento reutiliza el mismo `task_id`, conserva la limitacion anterior y añade evidencia. Tres fallos del mismo control requieren detener la automatizacion y devolver el bloqueo al propietario; nunca se relajan guardrails para completar una tarea.
