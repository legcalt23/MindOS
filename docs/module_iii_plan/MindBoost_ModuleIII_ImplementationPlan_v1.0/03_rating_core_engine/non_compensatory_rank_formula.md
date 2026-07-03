# NonCompensatoryRankFormula

## 1. Purpose
GlobalRatingMu = HarmonicMean(block_mus) × FloorPenalty(min_block_mu) × ConfidenceGate(min_sample_size). If ANY block < floor, Elite is blocked. Specialist badges awarded for individual block excellence.

## 2. Scope
Phase 03 Rating Core Engine.

## 3. Non-Goals
Production deployment. Live matchmaking.

## 4. Inputs
Phase 02 Data Model. Module III v1.1.1 Algorithm spec.

## 5. Outputs
fun calculateGlobalRank(blocks: Map<String, BlockRatingState>): GlobalRankResult

## 6. Data Model / Interfaces
fun calculateGlobalRank(blocks: Map<String, BlockRatingState>): GlobalRankResult

## 7. Implementation Steps
1. Implement NonCompensatoryRankFormula.
2. Write unit tests with synthetic data.
3. Verify audit log generation.

## 8. Acceptance Criteria
NonCompensatoryRankFormula passes all unit tests. No self-report/Premium inputs.

## 9. Tests
Synthetic data unit test for NonCompensatoryRankFormula.

## 10. Dependencies
Phase 02 Data Model.

## 11. Risks
Mathematical error propagation.

## 12. Rollback / Failure Handling
Fix formula. Re-run simulation.

## 13. Owner
Data Science

## 14. Blocking Phase
BLOCKING_BEFORE_OFFLINE_SIMULATION_BUILD

## 15. Definition of Done
NonCompensatoryRankFormula implemented, tested, guards verified.
