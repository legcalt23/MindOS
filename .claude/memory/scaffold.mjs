#!/usr/bin/env node
// @ts-check
/**
 * scaffold.mjs — (re)generate the 15 memory-type templates into
 * docs/memory/_templates/. Idempotent: safe to re-run. Templates are the
 * starting point a human or Claude fills in for `memory add --file`.
 *
 *   node .claude/memory/scaffold.mjs
 */

import fs from 'node:fs';
import path from 'node:path';
import { MEMORY_TYPES, TYPE_DESCRIPTIONS, todayIso } from './lib/schema.mjs';
import { serializeFrontmatter } from './lib/frontmatter.mjs';
import { templatesDir, ensureDir, relToRoot } from './lib/paths.mjs';

/** Per-type body skeletons. Types not listed get a generic body. */
const BODIES = {
  PRODUCT_VISION: '## Vision\n\n## Value proposition\n\n## Principles\n\n## Boundaries (what this is NOT)',
  ARCHITECTURE: '## Overview\n\n## Modules & boundaries\n\n## Data flow\n\n## Key invariants\n\n## Related\n- [source](../../...)',
  ADR: '**Status:** proposed  ·  **Date:** ' + todayIso() + '\n\n## Context\n\n## Decision\n\n## Alternatives considered\n- A — why not.\n- B — why not.\n\n## Consequences\n\n## Security & privacy',
  FEATURE_SPEC: '## Goal\n\n## Scope (in / out)\n\n## Behavior\n\n## Acceptance criteria\n- [ ] …\n\n## Analytics events\n\n## Risks',
  ROADMAP: '## Now\n\n## Next\n\n## Later\n\n## Explicitly out of scope (and why)',
  BUG_REPORT: '**Status:** open  ·  **Severity:** medium\n\n## Symptom\n\n## Root cause\n\n## Fix / workaround\n\n## Affected files\n- `path/to/file`',
  SESSION_SUMMARY: '## What changed\n\n## Decisions\n\n## Files touched\n\n## Verification\n\n## Next steps\n- …',
  CODING_STANDARD: '## Rule\n\n## Why\n\n## How to apply\n\n## Example\n\n## Related learnings',
  SECURITY_NOTE: '## Decision / threat\n\n## Control\n\n## Boundary (what must never cross)\n\n## Verification',
  PROMPT_PATTERN: '## When to use\n\n## The prompt\n\n```\n…\n```\n\n## Why it works here',
  RESEARCH_NOTE: '## Question\n\n## Findings\n\n## Recommendation\n\n## Sources\n- https://…',
  USER_PERSONA: '## Who (no real PII)\n\n## Goals\n\n## Frustrations\n\n## How MindBoost helps',
  API_CONTRACT: '## Endpoint / callable\n\n## Inputs\n\n## Outputs\n\n## Errors\n\n## Auth & limits',
  DATA_MODEL: '## Entity\n\n## Fields\n\n## Invariants\n\n## Relationships\n\n## Source of truth',
  DEPLOYMENT_NOTE: '## Environment\n\n## Procedure\n\n## Rollback\n\n## Verification',
};

function templateFor(type) {
  const data = {
    id: `${type.toLowerCase().replace(/_/g, '-')}-REPLACE-ME`,
    type,
    title: 'REPLACE ME',
    tags: [],
    source: null,
    importance: 'medium',
    created_at: todayIso(),
    updated_at: todayIso(),
    version: 1,
    encrypted: false,
    access_level: 'internal',
    metadata: {},
  };
  const header = `<!-- TEMPLATE: ${type} — ${TYPE_DESCRIPTIONS[type]}\n`
    + '     Fill the frontmatter + body, then: node .claude/memory/cli.mjs add --type '
    + type + ' --title "…" --file <this>.md (or copy into entries/). -->\n';
  return serializeFrontmatter(data, header + (BODIES[type] || '## Notes'));
}

function main() {
  const dir = ensureDir(templatesDir());
  let n = 0;
  for (const type of MEMORY_TYPES) {
    const file = path.join(dir, `${type}.md`);
    fs.writeFileSync(file, templateFor(type), 'utf8');
    n++;
  }
  process.stdout.write(`✓ wrote ${n} templates to ${relToRoot(dir)}\n`);
}

main();
