# Session Bootstrap Required

This document explains the mandatory bootstrapping check required for all subagent reviews.

## Why Bootstrap is Mandatory
All subagents in the Founder Council Extended are **SESSION_REAL** and **NOT PERSISTENT**. They are registered dynamically in the current memory session using the `define_subagent` tool.

When you start a new conversation session, the registry is empty. If you attempt to run a review mode without bootstrapping, the platform will return an error:
`subagent "[agent_id]" not found or not allowed to be invoked.`

---

## How to Initialize the Session

Before running any review, copy and paste this command:

```text
Bootstrap the Founder Council Extended: Call define_subagent for all 36 agents listed in BOOTSTRAP_FOUNDER_COUNCIL.md to prepare the session registry.
```

If registration fails or cannot be run, mock simulation is forbidden. The execution must fail closed immediately, returning a final status of **INCONCLUSIVE_BLOCKED**.

