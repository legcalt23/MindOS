#!/usr/bin/env node
// Generates PORTFOLIO_SOURCE_MANIFEST.json: base commit, UTC timestamp, full git status, platform, and
// per-source-file {relative path, SHA-256, tracked/untracked/modified, inspection method, what was
// extracted}. A dirty-worktree snapshot is only "hash-identifiable", NOT reconstructable from HEAD alone.
import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { execSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const git = (c) => { try { return execSync(c, { cwd: root, encoding: 'utf8' }).trim(); } catch { return ''; } };

const SOURCES = [
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/catalog/ExerciseCatalog.kt', extracted: 'Focus enum E1_VIGIA..E13_PRESION (13) + implemented list', method: 'Grep enum + Read' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/SpeedEnums.kt', extracted: 'SpeedExerciseId E1..E8 (8)', method: 'Grep enum' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/domain/model/LogicModels.kt', extracted: 'LogicExerciseType (5 types)', method: 'Read enum' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/presentation/ui/LogicRootScreen.kt', extracted: 'render branches / claimChoices per type', method: 'Grep' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/domain/content/LogicContentProvider.kt', extracted: 'content items per type', method: 'Grep' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/presentation/exercises/LanguageExerciseCatalog.kt', extracted: '6 standalone + mixedSession composite', method: 'Read (prior)' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/catalog/MemoryExerciseCatalog.kt', extracted: 'EX1..EX16 (16) + implemented=3', method: 'Grep + Read (prior)' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/DecisionsFlowStateHolder.kt', extracted: 'DecisionsFlowStage {CheckIn,BayesArena,Reversibility(hidden),Result}; includeReversibility=false', method: 'Grep' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/catalog/EmotionCatalogIds.kt', extracted: 'EmotionExerciseId EM1..EM15 (15)', method: 'Read' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/catalog/EmotionExerciseCatalog.kt', extracted: '15 definitions', method: 'Read' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/exercise/EmotionExerciseGenerators.kt', extracted: 'registry (6 wired: EM1,2,3,8,10,11)', method: 'Read' },
  { path: 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/creativity/domain/exercise/CreativityExerciseCatalog.kt', extracted: 'CreativityExerciseId (2 defined+wired)', method: 'Read' },
  { path: 'docs/mindboost_exercise_design/INDEX.md', extracted: 'design catalog (66 real exercises across 8 blocks)', method: 'Read' },
  { path: 'docs/context-pack/09_DECISION_LOG.md', extracted: 'CAT-001..CAT-012 (CAT-005 taken; denominator authority)', method: 'Grep' },
];

const statusPorcelain = git('git status --porcelain=v2');
const shortStatus = git('git status --short');
const trackedSet = new Set(git('git ls-files').split('\n'));

const sources = SOURCES.map((s) => {
  const abs = join(root, s.path);
  if (!existsSync(abs)) return { ...s, status: 'MISSING', sha256: null };
  const sha256 = createHash('sha256').update(readFileSync(abs)).digest('hex');
  const tracked = trackedSet.has(s.path);
  const modified = new RegExp(`\\s${s.path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`, 'm').test(shortStatus)
    || shortStatus.split('\n').some((l) => l.includes(s.path));
  return { path: s.path, sha256, tracked: tracked ? 'TRACKED' : 'UNTRACKED', modified: modified ? 'MODIFIED' : 'CLEAN', method: s.method, extracted: s.extracted, status: 'PRESENT' };
});

const manifest = {
  artifact: 'MINDBOOST portfolio 121 V2 source manifest',
  snapshotKind: 'hash-identifiable dirty snapshot (NOT reconstructable from HEAD alone — working tree is dirty with untracked/foreign WIP)',
  baseCommit: git('git rev-parse HEAD'),
  generatedUtc: new Date().toISOString(),
  platform: `${process.platform} node ${process.version}`,
  gitStatusShort: shortStatus.split('\n').filter(Boolean),
  worktreeDirty: shortStatus.trim().length > 0,
  sources,
  outputs: [
    'MINDBOOST_EXERCISE_PORTFOLIO.csv', 'MINDBOOST_EXERCISE_AUXILIARIES.csv', 'MINDBOOST_EXERCISE_PORTFOLIO.md',
  ].map((p) => existsSync(join(root, p)) ? { path: p, sha256: createHash('sha256').update(readFileSync(join(root, p))).digest('hex') } : { path: p, status: 'MISSING' }),
  limitation: 'Deliverable files are UNTRACKED; the manifest identifies the snapshot by hash, not by a reproducible commit.',
};
writeFileSync(join(root, 'PORTFOLIO_SOURCE_MANIFEST.json'), JSON.stringify(manifest, null, 2) + '\n', 'utf8');
console.log(`MANIFEST written: ${sources.length} sources, baseCommit ${manifest.baseCommit.slice(0, 12)}, dirty=${manifest.worktreeDirty}`);
for (const s of sources) console.log(`  ${s.status === 'MISSING' ? 'MISSING ' : (s.sha256 || '').slice(0, 12)} ${s.tracked || ''} ${s.modified || ''} ${s.path}`);
