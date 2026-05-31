# Release Process

## Stages

1. Development
2. Verification
3. Staging
4. Production

## Development

- Keep changes scoped to one ZIP/domain when possible.
- Update docs and templates in the same change as behavior.
- Add or update tests before requesting release approval.

## Verification

Mandatory commands:

```bash
./gradlew verifyProductionRepository
./gradlew allTests
```

Required checks:

- Tests passing.
- Security review complete for security-sensitive changes.
- Performance validation for matcher/executor hot-path changes.
- Observability impact reviewed for new outcomes, metrics, logs, or audit events.
- Production launch checklist updated if rollout assumptions changed.

## Staging

- Deploy with a unique appVersion/release tag.
- Confirm metrics include `appVersion`, `platform`, and `commandType`.
- Verify SLO dashboards and alert policy receive samples.
- Run rollback rehearsal for any high-impact change.

## Production

- Roll out gradually when the application layer supports phased deployment.
- Monitor `MatchRate`, `UnknownCommandRate`, `ActionExecutionRate`, `ValidationFailureRate`, and p95 latency.
- Stop rollout on critical alerts or repeated unknown command growth.

## Rollback

- Revert to the previous known-good appVersion.
- Keep requestId/sessionId correlation for incident reconstruction.
- File a postmortem for SEV1/SEV2 incidents.
