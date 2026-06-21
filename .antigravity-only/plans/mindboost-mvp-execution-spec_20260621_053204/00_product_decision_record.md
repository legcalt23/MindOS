# Product Decision Record (PDR)

**PDR ID:** PDR-20260621-001  
**Date:** 2026-06-21  
**Time:** 05:32:04  
**Status:** APPROVED  

---

## 1. Final MVP Decision

MindBoost will be built as an **offline-only, local-first Cognitive Readiness System (CRS)** for mobile (initially Android). The MVP scope is strictly limited to:
*   Sub-millisecond reaction-time operational self-assessment exercises: **E1 (Destello)** and **E6 (Multivía)**.
*   **AHS-lite:** A simple, deterministic layout-swapping mechanism to break muscle-memory heuristics.
*   **System Readiness Index (SRI):** A relative, within-user, same-device baseline scoring model.
*   **Safe Crisis Mode:** A brief (maximum 3 minutes), opt-in stress tolerance audit without sleep-deprivation triggers, punitive emotional feedback, or health risks.
*   **Local-only secure evidence logs:** Local data schemas (SQLCipher DB) with zero cloud synchronization.

---

## 2. Why Developers/Builders are the Beachhead Market

1.  **High-stakes, high-impact cognitive roles:** A single attentional lapse from a software builder can lead to system-wide outages, broken production deployments, or critical security vulnerabilities. The cost of a mistake is directly quantifiable.
2.  **High affinity for telemetry and logs:** Developers monitor server response times, latency, and CPU load. MindBoost treats the brain as another complex system, providing "attentional telemetry" (latencies in milliseconds, coefficients of variation) that directly appeals to their analytical mindset.
3.  **Low resistance to dark-mode/ monospace aesthetics:** Builders prefer clean, high-utility, functional dark-mode interfaces with zero cutesy gamification or vanity streaks.
4.  **Privacy advocates:** Developers value local-first, zero-cloud defaults, and are highly receptive to using a tool that stores 100% of its data locally and transparently.

---

## 3. Why Brain-Game Positioning is Rejected

*   **Commercial Saturation & Low Retention:** The market is flooded with casual brain games (Lumosity, Elevate, Peak) that compete on aesthetic novelty rather than scientific utility. These apps suffer from high long-term churn because they feel like toys.
*   **Scientific and Legal Controversies:** Proclaiming that playing puzzle games makes users "smarter" or "boosts IQ" has been repeatedly debunked and prosecuted by regulatory bodies (e.g., FTC actions).
*   **Focus on In-Game Heuristics:** Traditional brain games measure how well the user learns the game rules, not their raw cognitive capacity.

---

## 4. Why AI Runtime is Blocked

*   **Unnecessary Complexity:** Running local LLMs or complex neural networks on-device adds significant package size, battery drain, and latency variance.
*   **Lack of Auditability:** AI models are black boxes. A cognitive readiness index must be fully deterministic, auditable, and mathematical.
*   **Privacy Exposure:** Integrating third-party cloud APIs (OpenAI, Anthropic) breaks our zero-cloud promise and exposes highly sensitive user telemetry to remote servers.

---

## 5. Why Claims are Restricted

We enforce strict limitations on product messaging to prevent legal risks:
*   **No Medical/Clinical Claims:** MindBoost does not diagnose or treat ADHD, depression, anxiety, burnout, or dementia.
*   **No IQ Claims:** We make no claims about fluid intelligence enhancement.
*   **No Absolute Comparisons:** We do not compare different users' raw reaction times. All evaluations are within-user and same-device.
