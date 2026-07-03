# Mindboost Agent Operating Manual

## Recommended workflow for a new feature

1. Chief Architect designs the system.
2. Cognitive Science validates mechanism and claims.
3. Product UX designs the user journey.
4. Assessment/Diagnostics defines baselines and scoring if needed.
5. Personalization defines adaptation rules.
6. Backend implements data/API/business logic.
7. Frontend implements screens/components.
8. Data Analytics defines events and dashboards.
9. Security/Privacy reviews data handling.
10. QA/Safety gives final verdict.

## Best first feature

Build `Daily Mindboost`:

- 8–10 minute daily cognitive training session
- 3 micro-exercises
- adaptive difficulty
- post-session summary
- AI coach feedback
- progress against user's own baseline

## Default acceptance criteria

A feature can be accepted only if it has:

- cognitive target
- training mechanic
- scoring
- adaptation
- user feedback
- analytics
- tests
- safety review
- privacy review
- verification steps

## Recommended invocation

```text
Use mindboost-chief-architect to design the feature.
Then use mindboost-cognitive-science, mindboost-product-ux, mindboost-personalization-engine and mindboost-qa-safety to review it.
Synthesize their results into a final implementation plan.
```
