# Ledger 03 — Idempotency service

## Objetivo

Evitar replay y duplicados.

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

Implementa servicio de idempotencia por scope/key/requestHash con estados in_progress/completed/failed y reutilización segura de respuestas.

## Alcance permitido

Servicio, repositorio si falta, tests.

## Fuera de alcance

Wallet spend o purchases completos.

## Requisitos de seguridad

Misma key + body distinto debe devolver conflicto. Misma key + body igual debe devolver resultado previo.

## Tests mínimos

Tests de conflicto, replay y concurrencia.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
