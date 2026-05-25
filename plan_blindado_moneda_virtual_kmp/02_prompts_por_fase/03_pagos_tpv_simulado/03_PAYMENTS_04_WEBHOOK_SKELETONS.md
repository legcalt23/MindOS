# Pagos 04 — Webhook skeletons seguros

## Objetivo

Dejar endpoints preparados sin abrir agujeros.

## Contexto obligatorio

Estás implementando un sistema de moneda virtual blindado para una app Kotlin Multiplatform con backend autoritativo.

Reglas absolutas:

- El cliente nunca es fuente de verdad.
- No aceptes saldo final desde cliente.
- No implementes pagos reales todavía.
- Usa TPV sandbox/simulado.
- Toda operación económica debe ser idempotente.
- Toda operación económica debe ser auditada.
- Ledger append-only.
- Tests obligatorios.

## Tarea única

Crea endpoints skeleton para `/webhooks/google-play`, `/webhooks/app-store`, `/webhooks/stripe` que validen firma/configuración cuando exista y fallen cerrado si no están configurados.

## Alcance permitido

Endpoints skeleton, provider events insert, tests.

## Fuera de alcance

Procesamiento real de pagos.

## Requisitos de seguridad

No aceptar webhooks sin verificación. Guardar eventos sandbox solo si firma/config lo permite.

## Tests mínimos

Tests webhook sin firma rechazado, duplicado dedupe.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
