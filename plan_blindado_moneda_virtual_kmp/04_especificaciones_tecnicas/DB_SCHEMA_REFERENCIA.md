# DB schema de referencia

Usar PostgreSQL. Adaptar a Flyway/Liquibase/Exposed/JOOQ según el repo.

## Tablas mínimas

- users
- wallets
- ledger_transactions
- ledger_entries
- balance_snapshots
- idempotency_keys
- purchases
- payment_provider_events
- fraud_signals
- audit_logs
- admin_actions
- outbox_events

## Constraints obligatorias

- `wallets.user_id UNIQUE`
- `ledger_transactions(wallet_id, idempotency_key) UNIQUE`
- `purchases(provider, provider_purchase_token_hash) UNIQUE`
- `purchases(provider, provider_transaction_id) UNIQUE`
- `payment_provider_events(provider, provider_event_id) UNIQUE`
- `ledger_entries.amount <> 0`
- `balance_snapshots.available_balance` consistente con buckets
- FK de purchase acreditada a ledger transaction
- FK de ledger entries a transaction y wallet

## Nota

Para MVP puedes usar single-wallet ledger. Para enterprise, modelar cuentas de plataforma con double-entry formal.
