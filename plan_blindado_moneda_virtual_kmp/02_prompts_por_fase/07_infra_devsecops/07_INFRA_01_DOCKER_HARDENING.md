# Infra 01 — Docker hardening

## Objetivo

Endurecer contenedores.

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

Revisa Dockerfiles/compose y aplica hardening: non-root, minimal base, no secrets baked, healthchecks, read-only cuando posible.

## Alcance permitido

Docker/config/docs/tests smoke.

## Fuera de alcance

Terraform cloud completo.

## Requisitos de seguridad

No introducir secretos. Separar dev/prod config.

## Tests mínimos

Build de imagen y smoke test.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
