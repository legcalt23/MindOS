#!/usr/bin/env node
// V2d PHASE 4 — builds the Markdown from the DERIVED MODEL (not from the CSV).
// The model must be regenerated first (`node scripts/portfolio/derive-portfolio-model.mjs`); a stale model
// is caught by the validator's DERIVED_MODEL_FRESHNESS_GATE rather than silently published.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { parseCsv } from './csv.mjs';
import { renderPortfolio } from './render.mjs';
import { computeAll } from './metrics.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const modelPath = join(root, 'PORTFOLIO_DERIVED_MODEL.json');
if (!existsSync(modelPath)) { console.error('REFUSING: PORTFOLIO_DERIVED_MODEL.json missing — run derive-portfolio-model.mjs first'); process.exit(1); }
const model = JSON.parse(readFileSync(modelPath, 'utf8'));
const aux = parseCsv(readFileSync(join(root, 'MINDBOOST_EXERCISE_AUXILIARIES.csv'), 'utf8'));
writeFileSync(join(root, 'MINDBOOST_EXERCISE_PORTFOLIO.md'), renderPortfolio(model, aux), 'utf8');
console.log('BUILT MD from DERIVED MODEL:', JSON.stringify(computeAll(Object.values(model.perSlot))));
