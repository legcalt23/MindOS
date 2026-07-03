# Naming Conventions

## Terminology
- **ALWAYS:** Bayesian Competitive Rating, Competitive Rating, Global Rating, Block Rating, Mode Rating.
- **NEVER:** ELO, Elo, elo, IQ, clinical, diagnostic, gambling, stake, bet, casino, bankroll, odds, "gana dinero", "cerebro élite".

## Kotlin
- Data classes: `PascalCase` (e.g., `PlayerRatingState`).
- Enums: `UPPER_SNAKE_CASE` values (e.g., `IntegrityGateState.REVIEW_REQUIRED`).
- Functions: `camelCase` (e.g., `calculateRatingUpdate`).
- Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_SIGMA_THRESHOLD`).
- Packages: lowercase dot-separated (e.g., `com.mindos.app.features.arena.rating`).

## Files
- Implementation plan files: `snake_case.md`.
- Test files: `SIM_XX_description.md`, `MET_XX_description.md`, `RT_XX_description.md`, `VAL_XX_description.md`.

## Config Versions
- BlendingConfig: `BLEND_CONFIG_V{major}.{minor}`
- ModeScalingFactor: `MODE_SCALE_V{major}.{minor}`
