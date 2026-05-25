# Ledger 08 — Reconciliación snapshot vs ledger

## Objetivo

Detectar descuadres contables.

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

Implementa job/comando interno que recalcula saldo desde ledger y compara con balance_snapshots, registrando alertas/audit logs ante mismatch.

## Alcance permitido

Job, servicio, tests.

## Fuera de alcance

Auto-corregir producción sin bandera explícita.

## Requisitos de seguridad

Por defecto solo detectar. La corrección debe requerir modo explícito y auditarse.

## Tests mínimos

Tests de match y mismatch.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
