# Auditoría final 02 — Remediación

## Objetivo

Corregir hallazgos críticos/altos.

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

Lee `docs/security/final-audit-report.md` y corrige todos los hallazgos críticos y altos. Añade tests de regresión por cada hallazgo.

## Alcance permitido

Fixes y tests.

## Fuera de alcance

Nuevas features.

## Requisitos de seguridad

No cerrar hallazgo sin test o justificación técnica.

## Tests mínimos

Ejecutar suite completa.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
