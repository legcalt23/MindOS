# SigmaUpdate

## 1. Purpose
Reduces sigma (uncertainty) after valid sessions. CRITICAL: if smurfSuspicion == HIGH, sigma is KEPT HIGH or INCREASED to allow rapid recalibration. Never collapse sigma prematurely.

## 2. Scope
Phase 03 Rating Core Engine.

## 3. Non-Goals
Production deployment. Live matchmaking.

## 4. Inputs
Phase 02 Data Model. Module III v1.1.1 Algorithm spec.

## 5. Outputs
fun updateSigma(currentSigma: Double, finalMultiplier: Double, smurfSuspicion: SmurfSuspicionLevel): Double

## 6. Data Model / Interfaces
fun updateSigma(currentSigma: Double, finalMultiplier: Double, smurfSuspicion: SmurfSuspicionLevel): Double

## 7. Implementation Steps
1. Implement SigmaUpdate.
2. Write unit tests with synthetic data.
3. Verify audit log generation.

## 8. Acceptance Criteria
SigmaUpdate passes all unit tests. No self-report/Premium inputs.

## 9. Tests
Synthetic data unit test for SigmaUpdate.

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
SigmaUpdate implemented, tested, guards verified.
