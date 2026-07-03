# Functions dependency hardening — 2026-06-19

## CRIT-001

Production dependencies were upgraded without `npm audit fix --force`:

| Package | Before | After |
|---|---:|---:|
| `firebase-admin` | `12.7.0` | `12.7.0` |
| `firebase-functions` | `4.9.0` | `5.1.1` |

`firebase-functions` 5.1.1 is the newest line that preserves the backend's current v1 callable and
background-trigger API and requires Admin 12.x. Functions 6+ requires a repository-wide v1
compatibility import or v2 trigger migration. Admin 14 requires Node 22. Those runtime migrations
are intentionally deferred to separately reviewed changes.

The regenerated lockfile removes the production HIGH findings present before the upgrade:

- `GHSA-5375-pq7m-f5r2` and `GHSA-99f4-grh7-6pcq` in `@grpc/grpc-js`;
- `GHSA-hmw2-7cc7-3qxx` in `form-data`;
- `GHSA-f38q-mgvj-vph7` and `GHSA-wcpc-wj8m-hjx6` in `protobufjs`.

The required regression gate is `npm run audit:deps`, followed by `npm run verify`, from
`functions/`. Rollback is the previous exact versions plus the matching lockfile; rollback must
not be used if it reintroduces a HIGH or CRITICAL production advisory.

The root override targets only `uuid` versions below `11.1.1` and pins their replacement to exact
`11.1.1` (no caret). It must not override `universal-analytics`' independent `uuid@^14` contract:
an unconditional override produces an invalid npm dependency tree. The Functions override remains
globally pinned because its production graph accepts that version. Removal review: 2026-09-30,
tracking item `P0-012`; remove an override only after its lockfile resolves a non-vulnerable
upstream graph without it.

Root emulator tooling is also pinned away from HIGH advisories using range-scoped, exact
replacements: `@grpc/grpc-js@1.14.4`, `form-data@4.0.6`, `hono@4.12.25`, and
`protobufjs@7.6.3`. These are dev-only transitive dependencies, but they execute in CI and local
rules gates. The emulator suites are the compatibility gate for these overrides. Moderate
`@opentelemetry/core`/`@google-cloud/pubsub` advisories remain in `firebase-tools`; no non-breaking
upstream remediation is currently offered by npm audit, and they are outside the deployed
Functions production graph.
