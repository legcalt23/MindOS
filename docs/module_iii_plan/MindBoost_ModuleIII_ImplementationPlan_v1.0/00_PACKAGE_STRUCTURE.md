# Package Structure

```
com.mindos.app.features.arena/
├── rating/
│   ├── model/          # Data classes: PlayerRatingState, BlockRatingState, etc.
│   ├── engine/         # RatingUpdateAlgorithm, BlendingConfig, NonCompensatoryRank
│   ├── decay/          # RankDecay, SeasonReset
│   └── rollback/       # RollbackEngine
├── comparability/
│   ├── model/          # ComparabilityDecision, ComparabilityState
│   └── engine/         # DeviceNormalizer, LatencyThresholdModel
├── measurement/
│   ├── model/          # MeasurementConfidenceDecision
│   └── engine/         # SEMHandler, ReliabilityEstimator
├── integrity/
│   ├── model/          # IntegrityGateDecision, IntegrityState
│   ├── engine/         # SmurfDetector, BoostingDetector, CollusionGraph
│   └── review/         # PostReviewResolution, AppealManager
├── session/
│   ├── model/          # SessionResult, SessionEnvironmentSnapshot, SessionValidityDecision
│   └── explanation/    # ExplanationContract, SafeCopyMapper
├── evidence/
│   ├── model/          # EvidenceLedgerState, MotorEligibility
│   ├── engine/         # LedgerGate, LedgerExpiryChecker
│   └── motors/         # MOT_FOC_01, MOT_MEM_01, MOT_SPD_01
├── matchmaking/
│   ├── model/          # MatchmakingRequest, QueueConfig
│   └── engine/         # QueueExpansion, HardBoundaries, SmurfQueue
├── trials/
│   ├── model/          # TrialAttempt, TrialResult
│   └── engine/         # AscensionTrialsAlgorithm, AntiCoachingControls
├── simulation/
│   ├── generators/     # PopulationGen, SkillGen, DeviceGen, AbuseGen
│   ├── runner/         # SimulationRunner, RatingUpdateRunner
│   ├── metrics/        # AcceptanceMetricCalculator
│   └── export/         # DashboardExporter
├── audit/
│   ├── model/          # AuditLogEntry
│   └── engine/         # AuditLogger
└── config/
    ├── FeatureFlags.kt
    ├── RatingConfig.kt
    └── TerminologyScanner.kt
```
