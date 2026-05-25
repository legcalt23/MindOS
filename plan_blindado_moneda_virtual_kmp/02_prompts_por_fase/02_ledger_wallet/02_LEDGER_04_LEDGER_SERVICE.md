# Ledger 04 — Ledger append-only

## Objetivo

Crear núcleo contable.

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

Implementa LedgerService para crear transacciones confirmadas, entries append-only, reversos y actualización de snapshot dentro de una transacción.

## Alcance permitido

Servicio y tests.

## Fuera de alcance

Endpoints API o UI.

## Requisitos de seguridad

No debe existir método para mutar entries históricas. Todo reversal crea nueva transacción.

## Tests mínimos

Tests de saldo derivado, snapshot, reverso, amount cero rechazado.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
