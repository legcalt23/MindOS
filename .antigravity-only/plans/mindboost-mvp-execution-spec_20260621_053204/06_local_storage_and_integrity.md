# Local Storage and Integrity Specification

---

## 1. Local Data Model (Room DB Schema)

The MVP database (implemented in SQLite via Room) consists of three primary tables:

```
+---------------------+        +--------------------+        +--------------------+
|  SessionRecord      |        |  BlockRecord       |        |  TrialRecord       |
|  - sessionId (UUID) |1 ---- *|  - blockId (UUID)  |1 ---- *|  - trialId (UUID)  |
|  - startUptimeNanos |        |  - sessionId (UUID)|        |  - blockId (UUID)  |
|  - endUptimeNanos   |        |  - exerciseType    |        |  - latencyNanos    |
|  - sriPercent       |        |  - difficultyState |        |  - isCorrect       |
|  - baselineRefUuid  |        |  - blockMetrics    |        |  - stimulusNanos   |
|                     |        |                    |        |  - isValid         |
+---------------------+        +--------------------+        +--------------------+
```

### SQLite Column Definitions:
*   `TrialRecord`:
    *   `trialId` (TEXT, PRIMARY KEY)
    *   `blockId` (TEXT, FOREIGN KEY)
    *   `latencyNanos` (INTEGER)
    *   `isCorrect` (INTEGER, boolean flag)
    *   `stimulusNanos` (INTEGER)
    *   `isValid` (INTEGER)
    *   `checksum` (TEXT)

---

## 2. Data Retention Boundaries

### What Data is Stored:
*   Raw timing values (in nanoseconds) and correctness for completed trials.
*   Baseline metrics (rolling 7-day average models).
*   System Readiness Index scores.
*   Self-reported fatigue logs (values from $1$ to $5$ selected during daily check).

### What Data is NEVER Stored:
*   No camera frame data or gaze coordinates.
*   No keystroke text (no actual words or symbols typed outside active matching drills).
*   No details of other running apps or browser histories.
*   No GPS location logs.

---

## 3. Checksum & Integrity Plan

To introduce tamper-evident basic integrity checks that detect trivial or accidental local data modification (note: this does not protect against an attacker with full control of the device):
1.  **Row Checksum:** When a `TrialRecord` is inserted, the engine calculates a SHA-256 hash:
    $$\text{Checksum} = \text{SHA-256}(\text{trialId} + \text{blockId} + \text{latencyNanos} + \text{isCorrect} + \text{SecretKey})$$
2.  **Verify Loop:** During daily startup, the engine queries the active blocks and recalculates all hashes. If any checksum mismatch is detected, the database is flagged as corrupted, and the compromised scores are excluded from the baseline.

---

## 4. Encryption Recommendations

*   **Phase 1 (MVP Local Offline):**
    *   *Storage:* Plain SQLite or standard Room DB files. Encryption is deferred to Phase 2 to simplify debugging and baseline validation during alpha builds.
    *   *Key Storage:* Not required for MVP.
*   **Phase 2 (Release):**
    *   *Storage:* Integrate **SQLCipher** for database-wide file encryption.
    *   *Key Storage:* Key is generated locally using `SecureRandom` and stored in the Android KeyStore system.

---

## 5. Export and Purge Protocols

*   **CSV Export:** A simple button on the Settings screen allows the user to export their entire trial history into a plaintext CSV file.
    *   *Format:* `SessionID, ExerciseType, TrialIndex, LatencyMs, IsCorrect, SRI`
*   **Purge Data:** A button labeled **Delete All Local Data**. Clicking it executes a database drop, calls SQLite's vacuum command, and purges the shared preferences file containing baselines.
