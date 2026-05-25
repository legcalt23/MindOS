# Testing 02 — Concurrencia y double spending

## Objetivo

Romper race conditions antes que producción.

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

Añade tests de concurrencia: 100 gastos simultáneos contra saldo limitado, 100 verifies del mismo purchase sandbox, spend + refund/reversal si existe.

## Alcance permitido

Tests integración/concurrencia.

## Fuera de alcance

Refactors grandes.

## Requisitos de seguridad

Debe demostrar que no se gasta más del saldo ni se acredita dos veces.

## Tests mínimos

Ejecutar tests repetidos si es posible.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
