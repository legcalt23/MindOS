# Checklist seguridad MVP

## Wallet/Ledger

- [ ] Saldo real solo backend.
- [ ] Ledger append-only.
- [ ] Snapshot reconstruible.
- [ ] Gasto con lock transaccional.
- [ ] Idempotency key obligatoria.
- [ ] Audit log por movimiento.
- [ ] Tests de double spending.

## Compras sandbox

- [ ] Sandbox provider server-side.
- [ ] Packs permitidos: 1, 10, 100.
- [ ] Purchase token/hash único.
- [ ] Verify idempotente.
- [ ] No pago real.
- [ ] Providers reales fallan cerrado.

## KMP

- [ ] Pantalla Moneda.
- [ ] Entrada desde Home.
- [ ] Saldo refrescado desde backend.
- [ ] No saldo local autoritativo.
- [ ] Errores manejados.

## Backend

- [ ] Rate limiting.
- [ ] Logging estructurado.
- [ ] No secrets en logs.
- [ ] Authz estricta.
- [ ] Error model seguro.

## Admin

- [ ] RBAC.
- [ ] Justificación.
- [ ] Maker/checker.
- [ ] Audit logs.
