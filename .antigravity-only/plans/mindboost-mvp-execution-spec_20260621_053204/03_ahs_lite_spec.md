# AHS-lite Specification: Heuristic Disruption Engine

---

## 1. Objective and Anti-Practice-Effect Logic

The **Adversarial Heuristic Saboteur (AHS) lite** is designed to break the motor muscle-memory that users build when playing reaction-time drills. 

In traditional games, users learn to map a stimulus (e.g., left arrow) to a motor coordinate (e.g., tap left thumb) without cognitive processing. By dynamically mutating the rule structure during active blocks, AHS forces the brain to disengage the automated basal ganglia loop and re-engage the prefrontal executive network, maintaining neuroplastic activation.

---

## 2. Rule-Switching Mechanics

In **E6 (Multivía)**, the standard layout places the "Left Target Box" on the left half of the screen and the "Right Target Box" on the right half.

*   **The Switch Event:** After a streak of $K$ correct trials ($K \in [6, 10]$ calculated dynamically based on baseline accuracy), the engine triggers a rule mutation.
*   **The Mutation (Box Swap):** The positions of the target boxes swap: the "Left Target Box" is rendered on the right side of the screen, and the "Right Target Box" is rendered on the left.
*   **User Task:** If the visual arrow points $\leftarrow$, the user must now tap the target box labeled "Left" (which is now physically on the right side of the screen).

---

## 3. Safe Difficulty Bounds

To prevent complete cognitive breakdown and ensure usability:
*   **No Multi-swaps:** Box locations can only swap on the horizontal axis. No vertical or diagonal shifts.
*   **Feedback Interval:** A rule mutation cannot occur more than once every 6 trials.
*   **Minimum Target Size:** The touch target boxes must remain at a minimum physical size ($>48\text{dp}$) to avoid motor precision errors.

---

## 4. User Frustration Safeguards

Because AHS-lite increases cognitive friction, it can cause user irritation. We implement the following safeguards:
*   **Visual Warning Cue (Highlight):** When a rule switch occurs, the borders of the swapped target boxes flash with a subtle blue highlight for 300ms during the transition, alert-indexing the change.
*   **Explanatory Micro-copy:** During onboarding, the interface states: *"When target borders flash, the mapping has mutated. This friction is normal and means your executive focus is working."*

---

## 5. Scoring Modifications under AHS

When a rule swap occurs, reaction times will naturally spike. We adjust scoring to reflect this:
*   **Switch-Trial Exclusions:** The first two trials immediately following a rule swap are **excluded** from calculating the daily baseline *speed* median (to avoid skewing raw latency metrics).
*   **Inhibition Score:** The accuracy of the user on the switch trial measures their cognitive inhibition capacity (the ability to suppress the automated motor habit).

---

## 6. What NOT to Implement Yet
*   **No Color Swaps:** Do not use color changes (e.g., blue arrow means red box) to avoid color-blindness accessibility conflicts.
*   **No Dynamic Deadlines:** Do not dynamically accelerate the trial timer during a rule swap block; keep the deadline constant at 2000ms.
*   **No Multi-staircase shifts:** Do not run concurrent staircases for mutated and normal states.
