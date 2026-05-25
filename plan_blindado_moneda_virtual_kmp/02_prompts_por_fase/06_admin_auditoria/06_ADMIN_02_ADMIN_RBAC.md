# Admin 02 — RBAC base

## Objetivo

Proteger endpoints administrativos.

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

Implementa base de RBAC admin con roles, permisos, middleware y tests. Si ya existe auth admin, intégralo.

## Alcance permitido

RBAC/middleware/tests.

## Fuera de alcance

Ajustes económicos todavía.

## Requisitos de seguridad

Deny by default. 2FA/IP allowlist como hooks/config aunque no esté proveedor final.

## Tests mínimos

Tests acceso denegado/permitido.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
