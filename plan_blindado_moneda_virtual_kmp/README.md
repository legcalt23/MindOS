# Plan blindado de ejecución — Moneda virtual KMP

Fecha: 2026-05-24

Este paquete contiene un plan de ejecución por fases para implementar un sistema de moneda virtual/puntos en una app Kotlin Multiplatform con backend autoritativo, ledger append-only, antifraude, infraestructura preparada para pagos reales y TPV simulado temporal.

## Objetivo

Implementar una arquitectura de moneda virtual extremadamente resistente a:

- Manipulación del cliente.
- Falsificación de monedas.
- Replay attacks.
- Double spending.
- Race conditions.
- Reutilización de purchase tokens.
- Webhook spoofing.
- Abuso de promociones.
- Ataques al panel admin.
- Fallos de reconciliación.
- Errores de concurrencia.
- Fraude económico.

## Filosofía de ejecución

Cada prompt está diseñado para que Cloud Code / Claude Code / agente de coding ejecute **una única tarea concreta por tirada**.

No mezcles prompts. No pidas “implementa todo” salvo usando el prompt maestro de orquestación. La forma más segura es:

1. Abrir el repositorio.
2. Ejecutar los prompts en orden exacto.
3. Confirmar tests después de cada bloque.
4. No avanzar si hay tests rotos.
5. Ejecutar auditoría final.
6. Corregir hallazgos con los prompts de remediación.

## Estructura

```text
00_instrucciones/
01_plan_ejecucion/
02_prompts_por_fase/
03_checklists/
04_especificaciones_tecnicas/
05_plantillas/
```

## Resultado esperado

Al terminar:

- Backend con wallet server-authoritative.
- Ledger append-only.
- Snapshots reconstruibles.
- Idempotencia fuerte.
- API wallet.
- Compra simulada de 1, 10 y 100 puntos.
- Arquitectura preparada para Google Play, Apple IAP y Stripe.
- Pantalla nueva en Home llamada “Moneda”.
- Botones de compra: 1, 10 y 100 puntos.
- TPV simulado sin pago real.
- Antifraude inicial.
- Admin auditado.
- Tests de unidad, integración, concurrencia y seguridad.
- Auditoría final automatizada.
