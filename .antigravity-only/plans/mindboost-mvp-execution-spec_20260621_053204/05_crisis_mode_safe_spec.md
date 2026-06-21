# Crisis Mode Specification: Safe Stress Tolerance Audit

---

## 1. Concept and Safe Adaptation Policy

**Crisis Mode** is renamed to **Stress Tolerance Audit** for consumer protection. It is a brief, opt-in sandbox designed to measure cognitive reliability under simulated pressure (auditory/visual noise and time compression). 

It is strictly a **stress-challenge sandbox**, not a sleep-deprivation induction system. We do not prompt or advise users to avoid sleep.

---

## 2. Session and Duration Limits

To ensure physical and mental safety:
*   **Maximum Duration:** The Stress Tolerance Audit is limited to a single block of **180 seconds (3 minutes)**. The block automatically terminates when the timer expires.
*   **Daily Cap:** Users are restricted to a maximum of **2 audits per day** to prevent fatigue and excessive stress.

---

## 3. Opt-in and Control Mechanics

*   **No Auto-Trigger:** The app never triggers a Stress Tolerance Audit automatically. The user must navigate to the option and tap **Begin Stress Audit**.
*   **Emergency Stop:** A highly visible **Exit Audit** button is present on the screen at all times during the test. Pressing it immediately stops the drill, discards the session, and returns the user to the dashboard.

---

## 4. Stress Intensity Levels

The audit scales stress parameters across three levels, increasing difficulty progressively:

```
[Level 1: 0 - 60s]    -->    [Level 2: 61 - 120s]  -->    [Level 3: 121 - 180s]
- Deadline: 1500ms           - Deadline: 1000ms           - Deadline: 800ms
- No visual noise            - Light border flicker       - High-speed border flash
- Low tone error sound       - Steady alarm pulse         - Rapid pitch alert sound
```

*   **Visual Noise:** Subtle, low-contrast border flashes on the edge of the screen when errors occur. No strobe lighting or rapid screen-wide flashes (safeguard for photosensitive users).
*   **Auditory Stress:** A steady metronome click or synthesized alert pulse. Users must be able to mute audio via their standard hardware buttons.

---

## 5. Prohibition of Punitive Feedback

We reject negative or manipulative feedback:
*   No textual insults, alerts of failure, or anxiety-inducing copy (e.g., "You are failing").
*   Feedback is strictly structural: a short visual indicator (red box border) on errors, and a score tally at the end showing *Accuracy %* and *Mean Latency*.

---

## 6. Contraindication Copy and Consent

Before launching the Stress Tolerance Audit, the user must review and accept the following text:
> **Stress Tolerance Audit Consent**  
> *"This drill uses rapid visual changes, high-frequency alert sounds, and strict time limits to test cognitive consistency under load. Do not participate if you have a history of seizures, photosensitivity, high anxiety, cardiac conditions, or sensory sensitivity. You may exit the audit at any time by pressing the Exit button."*
