#!/usr/bin/env node
// Deterministic STATIC code-inventory extractor (B). Reads the real MindBoost sources and emits, per
// block, the set of DECLARED code ids (from the enum/catalog), the WIRED ids (registry/implemented),
// and reachability facts (root screen, renderer, hub reference) — with source paths + SHA-256. This is
// the reconciliation authority the validator compares the CSV against. NO runtime execution → the
// RUNTIME_PLAYABILITY dimension stays NOT_RUN. Run: node scripts/portfolio/extract-code-inventory.mjs
import { readFileSync, existsSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const P = 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost';
const rd = (rel) => existsSync(join(root, rel)) ? readFileSync(join(root, rel), 'utf8') : null;
const sha = (rel) => existsSync(join(root, rel)) ? createHash('sha256').update(readFileSync(join(root, rel))).digest('hex') : null;
const uniq = (a) => [...new Set(a)];
const tokens = (text, re) => text ? uniq((text.match(re) || [])) : [];
function enumMembers(text, name) {
  if (!text) return [];
  const m = text.match(new RegExp(`enum class\\s+${name}\\s*(?:\\([^)]*\\))?\\s*\\{([\\s\\S]*?)\\}`));
  if (!m) return [];
  return uniq(m[1].split(/[,\n]/).map((s) => s.trim().replace(/\(.*$/, '').trim()).filter((s) => /^[A-Za-z][A-Za-z0-9_]*$/.test(s)));
}
function listOfBlock(text, anchor) { // extract ids inside `<anchor> ... listOf( ... )`
  if (!text) return null;
  const i = text.indexOf(anchor); if (i < 0) return null;
  const j = text.indexOf('listOf(', i); if (j < 0) return null;
  let depth = 0, k = j + 6, out = '';
  for (; k < text.length; k++) { const c = text[k]; if (c === '(') depth++; else if (c === ')') { depth--; if (depth === 0) break; } out += c; }
  return out;
}

const BLOCKS = {
  FOCUS: {
    label: 'Enfoque', declFile: `${P}/focus/domain/catalog/ExerciseCatalog.kt`,
    declRe: /\bE\d+_[A-Z][A-Z0-9_]*\b/g, wiredFile: `${P}/focus/domain/catalog/ExerciseCatalog.kt`, wiredAnchor: 'implemented',
    rootFile: `${P}/focus/presentation/ui/FocusRootScreen.kt`, rendererFile: `${P}/focus/presentation/ui/FocusSessionScreen.kt`,
    registrySymbol: 'FocusExerciseGenerators', hubKeyword: 'Enfoque',
  },
  SPEED: {
    label: 'Velocidad', declFile: `${P}/speed/domain/SpeedEnums.kt`, declRe: /\bE\d+_[A-Z][A-Z0-9_]*\b/g,
    wiredFile: `${P}/speed/domain/exercise/SpeedPromptGenerators.kt`, rootFile: `${P}/speed/presentation/ui/SpeedRootScreen.kt`,
    rendererFile: `${P}/speed/presentation/ui/SpeedExerciseScreen.kt`, registrySymbol: 'SpeedPromptGenerators', hubKeyword: 'Velocidad',
  },
  LOGIC: {
    label: 'Logica', declFile: `${P}/logic/domain/model/LogicModels.kt`, declEnum: 'LogicExerciseType',
    wiredFile: `${P}/logic/domain/content/LogicContentProvider.kt`, rootFile: `${P}/logic/presentation/ui/LogicRootScreen.kt`,
    rendererFile: `${P}/logic/presentation/ui/LogicRootScreen.kt`, registrySymbol: 'LogicExerciseType', hubKeyword: 'gica',
  },
  LANG: {
    label: 'Lenguaje', declFile: `${P}/language/presentation/exercises/LanguageExerciseCatalog.kt`,
    declRe: /\b[A-Z][A-Za-z]+Bank\b/g, wiredFile: `${P}/language/presentation/exercises/LanguageExerciseCatalog.kt`,
    rootFile: `${P}/language/presentation/ui/LanguageRootScreen.kt`, rendererFile: `${P}/language/presentation/ui/LanguageRootScreen.kt`,
    registrySymbol: 'LanguageExerciseCatalog', hubKeyword: 'Lenguaje', extraDeclared: ['mixed_session'],
  },
  MEMORY: {
    label: 'Memoria', declFile: `${P}/memory/domain/catalog/MemoryExerciseCatalog.kt`, declRe: /\bEX\d+_[A-Z][A-Z0-9_]*\b/g,
    wiredAnchor: 'implemented', wiredFile: `${P}/memory/domain/catalog/MemoryExerciseCatalog.kt`,
    rootFile: `${P}/memory/presentation/ui/MemoryRootScreen.kt`, rendererFile: `${P}/memory/presentation/ui/MemorySessionScreen.kt`,
    registrySymbol: 'MemoryExerciseCatalog', hubKeyword: 'Memoria',
  },
  DECIDE: {
    label: 'Decisiones', declFile: `${P}/decisiones/presentation/DecisionsFlowStateHolder.kt`, declEnum: 'DecisionsFlowStage',
    wiredFile: `${P}/decisiones/presentation/ui/DecisionsRootScreen.kt`, rootFile: `${P}/decisiones/presentation/ui/DecisionsRootScreen.kt`,
    rendererFile: `${P}/decisiones/presentation/ui/DecisionsRootScreen.kt`, registrySymbol: 'DecisionsFlowStage', hubKeyword: 'Decisiones',
  },
  EMOTION: {
    label: 'Emociones', declFile: `${P}/emotions/domain/catalog/EmotionCatalogIds.kt`, declRe: /\bEM\d+_[A-Z][A-Z0-9_]*\b/g,
    wiredFile: `${P}/emotions/domain/exercise/EmotionExerciseGenerators.kt`, wiredMode: 'numPrefix',
    rootFile: `${P}/emotions/presentation/ui/EmotionRootScreen.kt`,
    rendererFile: `${P}/emotions/presentation/ui/EmotionSessionScreen.kt`, registrySymbol: 'EmotionExerciseGenerators', hubKeyword: 'Emociones',
  },
  CREATE: {
    label: 'Creatividad', declFile: `${P}/creativity/domain/exercise/CreativityExerciseCatalog.kt`, declRe: /\bEX0\d_[A-Z][A-Z0-9_]*\b/g,
    wiredAnchor: 'implemented', wiredFile: `${P}/creativity/domain/exercise/CreativityExerciseCatalog.kt`,
    rootFile: `${P}/creativity/presentation/ui/CreativityRootScreen.kt`, rendererFile: `${P}/creativity/presentation/ui/CreativityRootScreen.kt`,
    registrySymbol: 'CreativityExerciseCatalog', hubKeyword: 'Creatividad',
  },
};
const HUB = `${P}/presentation/ui/MindBoostHubScreen.kt`;
const hubText = rd(HUB);
// Non-exercise infrastructure ids that are legitimately NOT target/aux exercises.
const ALLOWLIST_INFRA = ['Result'];

const out = {
  artifact: 'MindBoost portfolio code inventory (static)', generatedUtc: new Date().toISOString(),
  baseCommit: (() => { try { return execSync('git rev-parse HEAD', { cwd: root }).toString().trim(); } catch { return null; } })(),
  hubFile: HUB, hubSha: sha(HUB), allowlistInfra: ALLOWLIST_INFRA, blocks: {}, observedCodeIds: [], perId: {},
};

for (const [key, b] of Object.entries(BLOCKS)) {
  const declText = rd(b.declFile);
  let declaredIds = b.declEnum ? enumMembers(declText, b.declEnum) : tokens(declText, b.declRe);
  if (b.extraDeclared) declaredIds = uniq([...declaredIds, ...b.extraDeclared]);
  const wiredText = rd(b.wiredFile);
  let wiredIds;
  if (b.wiredAnchor) { const blockTxt = listOfBlock(wiredText, b.wiredAnchor); wiredIds = blockTxt ? declaredIds.filter((id) => blockTxt.includes(id)) : []; }
  else if (b.wiredMode === 'numPrefix') { // registry uses generator CLASS names (e.g. EM1PausaGenerator); match by id's alnum prefix
    wiredIds = declaredIds.filter((id) => { const m = id.match(/^([A-Z]+\d+)_/); return m && wiredText && new RegExp(`\\b${m[1]}[A-Za-z]*(Generator|Bank)\\b`).test(wiredText); });
  } else wiredIds = declaredIds.filter((id) => wiredText && new RegExp(`\\b${id}\\b`).test(wiredText));
  const rendererText = rd(b.rendererFile);
  const rec = {
    label: b.label, declFile: b.declFile, declSha: sha(b.declFile), declaredIds,
    wiredFile: b.wiredFile, wiredSha: sha(b.wiredFile), wiredIds,
    rootFile: b.rootFile, rootExists: existsSync(join(root, b.rootFile)), rootSha: sha(b.rootFile),
    rendererFile: b.rendererFile, rendererExists: existsSync(join(root, b.rendererFile)),
    rendererDispatchesRegistry: !!(rendererText && rendererText.includes(b.registrySymbol)),
    hubReferencesBlock: !!(hubText && hubText.includes(b.hubKeyword)),
  };
  out.blocks[key] = rec;
  out.observedCodeIds.push(...declaredIds);
  for (const id of declaredIds) {
    out.perId[id] = {
      block: key, declarationFound: true, catalogMembership: true,
      generatorRegistered: wiredIds.includes(id),
      rendererReference: !!(rendererText && new RegExp(`\\b${id}\\b`).test(rendererText)),
      rendererDispatchesRegistry: rec.rendererDispatchesRegistry,
      rootExists: rec.rootExists, hubReferencesBlock: rec.hubReferencesBlock,
      sourcePath: b.declFile, sourceSha: rec.declSha,
    };
  }
}
out.observedCodeIds = uniq(out.observedCodeIds);
writeFileSync(join(root, 'PORTFOLIO_CODE_INVENTORY.json'), JSON.stringify(out, null, 2) + '\n', 'utf8');
console.log(`CODE INVENTORY: ${out.observedCodeIds.length} observed code ids across ${Object.keys(out.blocks).length} blocks`);
for (const [k, b] of Object.entries(out.blocks)) console.log(`  ${k}: declared=${b.declaredIds.length} wired=${b.wiredIds.length} root=${b.rootExists} rendererDispatch=${b.rendererDispatchesRegistry} hub=${b.hubReferencesBlock}`);
