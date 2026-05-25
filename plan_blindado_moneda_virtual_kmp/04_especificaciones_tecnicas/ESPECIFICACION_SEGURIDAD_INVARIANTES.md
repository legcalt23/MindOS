# Especificación de invariantes de seguridad

## Invariantes absolutos

1. El cliente nunca es fuente de verdad del saldo.
2. El saldo visible debe derivarse de `balance_snapshots`, que debe poder reconstruirse desde `ledger_entries`.
3. Todo movimiento económico tiene `ledger_transaction`.
4. Todo movimiento económico tiene al menos una `ledger_entry`.
5. El ledger es append-only.
6. Un purchase token solo puede acreditarse una vez.
7. Un webhook de proveedor solo puede procesarse una vez.
8. Una idempotency key no puede reutilizarse con un body diferente.
9. Ningún endpoint público puede aceptar `userId` objetivo arbitrario para modificar saldo.
10. Los endpoints admin requieren RBAC, auditoría y justificación.
11. Los refunds generan transacciones reversas, no edición histórica.
12. Las operaciones de gasto deben bloquear wallet/snapshot o usar optimistic locking seguro.
13. No puede existir gasto confirmado sin saldo suficiente salvo política explícita de deuda.
14. No puede existir compra acreditada sin purchase verificado.
15. Los logs no deben contener purchase tokens completos, secrets ni PII innecesaria.

## Invariantes testeables

- `snapshot.available == sum(ledger confirmed)`
- `purchase(provider, token_hash)` es único.
- `provider_event(provider, event_id)` es único.
- `ledger_transactions(wallet_id, idempotency_key)` es único.
- 100 gastos simultáneos no pueden gastar más que el saldo.
- 100 verificaciones simultáneas del mismo token acreditan una sola vez.
