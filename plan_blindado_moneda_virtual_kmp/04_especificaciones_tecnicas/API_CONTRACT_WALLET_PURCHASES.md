# API contract — Wallet y compras sandbox

## GET /wallet/balance

Auth: User JWT.

Response:

```json
{
  "availableBalance": 100,
  "breakdown": {
    "purchased": 100,
    "promotional": 0,
    "earned": 0
  },
  "walletStatus": "active",
  "version": 1,
  "serverTime": "2026-05-24T00:00:00Z"
}
```

## GET /wallet/transactions

Auth: User JWT.

Query:

- `limit`
- `cursor`

## POST /wallet/spend

Auth: User JWT.

Headers:

- `Idempotency-Key`

Request:

```json
{
  "itemId": "feature_unlock_x",
  "quantity": 1
}
```

El cliente no envía precio final.

## POST /purchases/sandbox/initiate

Auth: User JWT.

Headers:

- `Idempotency-Key`

Request:

```json
{
  "packId": "coins_10"
}
```

Packs permitidos durante etapa TPV no real:

- `coins_1`
- `coins_10`
- `coins_100`

Response:

```json
{
  "sandboxPurchaseId": "uuid",
  "provider": "sandbox",
  "status": "verified",
  "coins": 10
}
```

## POST /purchases/sandbox/verify

Auth: User JWT.

Headers:

- `Idempotency-Key`

Request:

```json
{
  "sandboxPurchaseId": "uuid"
}
```

Debe acreditar monedas mediante ledger.

## Futuro

Mantener contratos preparados para:

- `POST /purchases/google-play/verify`
- `POST /purchases/app-store/verify`
- `POST /purchases/stripe/checkout`
- `POST /webhooks/google-play`
- `POST /webhooks/app-store`
- `POST /webhooks/stripe`
