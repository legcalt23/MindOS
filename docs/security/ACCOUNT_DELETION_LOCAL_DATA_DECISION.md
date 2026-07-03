# Account deletion: local data decision

Date: 2026-06-19  
Scope: Fase 0, P0-003 and P0-004

## Decision

Account deletion is coordinated as a recoverable operation. Before calling the server-authoritative deletion endpoint, the app persists the authenticated UID in a dedicated encrypted deletion journal. A successful operation must complete both remote deletion and local erasure before that journal is removed.

At startup and after authentication, the coordinator retries any pending operation. If no user is authenticated, it erases local data but retains the journal until the same account can complete the remote call. If a different account is active, it does not erase or delete either account.

## Local inventory and erasure policy

The following encrypted namespaces contain account-bound cognitive data and are erased:

- `mindboost_focus`
- `mindboost_memory`
- `mindboost_speed`
- `mindboost_logic`

These stores currently belong to the single active account and do not partition records by UID. The eraser therefore clears each complete namespace. This deliberately prefers safe over-deletion over leaving another user's cognitive history on a shared device.

The separate `account_deletion_journal` namespace contains only the pending UID. It is encrypted by the same platform secure-store implementation, is never sent to analytics or logs, and is retained only until both sides of the deletion complete.

## Fail-closed behavior

- Failure to persist the journal prevents remote deletion from starting.
- Initial remote failure leaves cognitive data and the journal intact for a retry.
- Remote success followed by local failure leaves the journal intact and blocks normal app hydration on the next startup until local erasure succeeds.
- Recovery never applies a pending UID to a different authenticated account.
- Coroutine cancellation is propagated and is not converted into a successful deletion.

## Residual risk

Firebase SDK-managed caches are outside the four MindBoost namespaces. Server deletion remains authoritative for cloud data, while this phase guarantees removal of the locally persisted cognitive datasets owned by MindBoost. Extending the inventory to future local stores is mandatory when a new account-bound namespace is introduced.
