#!/usr/bin/env node
// Per-block ARCHITECTURE-AWARE adapters (V2d Phase 2 + Phase 3 corrections C3/C5/C6).
//
// V2c assumed every block had ONE wiredFile and ONE rendererFile and substring-probed them. MindBoost has
// EIGHT different architectures, so those probes read the wrong file and emitted false negatives that were
// indistinguishable from real absence (MB-PORTFOLIO-0013). Each block declares its own strategy here.
//
// EPISTEMIC CONTRACT
//   VERIFIED_PRESENT  a SEMANTIC BINDING for this axis was observed — i.e. the id sits in a construct that
//                     actually means something for the axis (registry entry, when-branch, enum member,
//                     declaration...). A bare code token is NOT enough (C5): it yields UNRESOLVED with
//                     reason CODE_TOKEN_ONLY_NO_SEMANTIC_BINDING.
//   VERIFIED_ABSENT   requires exhaustive:true AND an exhaustivenessEvidenceId naming a closed-world source
//                     set (C6). No strategy currently qualifies → VERIFIED_ABSENT_ALLOWED_STRATEGIES = 0.
//   UNRESOLVED        the strategy cannot resolve this architecture, or a declared source is missing.
//   NOT_APPLICABLE    the axis has no meaning for this block.
//
// AXES (C3): blockReachability and exerciseRoute are DIFFERENT FACTS and are never conflated. Block
// reachability (root screen exists + hub references the block) is context only; it can never stand in for
// per-exercise route evidence.
import { hasCodeToken, semanticBinding } from './kotlin-code-scan.mjs';

export const ADAPTERS_VERSION = '3.0.0';
export const P = 'shared/src/commonMain/kotlin/com/mindos/app/features/mindboost';
export const HUB_FILE = `${P}/presentation/ui/MindBoostHubScreen.kt`;
export const S = { PRESENT: 'VERIFIED_PRESENT', ABSENT: 'VERIFIED_ABSENT', UNRESOLVED: 'UNRESOLVED', NA: 'NOT_APPLICABLE' };

// Constructs that can legitimately accredit each axis. An id found in any OTHER construct — or in none —
// cannot accredit that axis.
export const AXIS_CONSTRUCTS = {
  declaration:   ['enum-member', 'property-declaration', 'type-declaration', 'collection-entry:listOf', 'collection-entry:setOf', 'collection-entry:mapOf'],
  mechanic:      ['collection-entry:listOf', 'collection-entry:setOf', 'collection-entry:mapOf', 'map-entry', 'when-branch-or-lambda', 'type-declaration'],
  generator:     ['type-declaration'],
  renderer:      ['when-branch-or-lambda', 'map-entry', 'collection-entry:listOf', 'collection-entry:mapOf'],
  exerciseRoute: ['when-branch-or-lambda', 'map-entry'],
};

// Probes a declared file set for a SEMANTIC BINDING acceptable to `axis`.
const bindingIn = (files, label, axis, exhaustive = false, exhaustivenessEvidenceId = null) => ({
  strategyId: `semantic-binding-in:${label}`, exhaustive, exhaustivenessEvidenceId, files, axis,
  probe: (id, texts) => {
    const missing = files.filter((f) => texts[f] == null);
    if (missing.length) return { state: S.UNRESOLVED, reason: `mechanism source missing: ${missing.join(', ')}` };
    let tokenOnlyIn = null;
    for (const f of files) {
      const b = semanticBinding(texts[f], id);
      if (!b) continue;
      if (b.construct && AXIS_CONSTRUCTS[axis].includes(b.construct)) {
        return { state: S.PRESENT, symbol: id, evidencePath: f, construct: b.construct, matchRange: b.matchRange,
          reason: `${axis} accredited by construct "${b.construct}" in ${label}` };
      }
      if (b.tokenOnly || b.construct) tokenOnlyIn ??= { f, construct: b.construct };
    }
    if (tokenOnlyIn) return { state: S.UNRESOLVED, reasonCode: 'CODE_TOKEN_ONLY_NO_SEMANTIC_BINDING',
      reason: `CODE_TOKEN_ONLY_NO_SEMANTIC_BINDING: the id appears in ${tokenOnlyIn.f.split('/').pop()}${tokenOnlyIn.construct ? ` as "${tokenOnlyIn.construct}"` : ' as a bare code token'}, which is not a construct that can accredit ${axis} (accepted: ${AXIS_CONSTRUCTS[axis].join(', ')})` };
    return { state: S.UNRESOLVED, reason: `no code token for this id in ${label}; the strategy is NOT exhaustive over this architecture, so absence is NOT proven` };
  },
});

// The id is wired via its OWN file (E1VigiaGenerator.kt, E1DestelloScreen.kt). Non-exhaustive: we cannot
// enumerate every legal filename/nesting a dispatch could use, so a miss is UNRESOLVED.
const perIdFile = (dir, suffixRe, label) => ({
  strategyId: `per-id-file:${label}`, exhaustive: false, exhaustivenessEvidenceId: null, files: [], axis: 'generator',
  probe: (id, _texts, ctx) => {
    const stem = (id.match(/^([A-Za-z]+\d+)_/) || [])[1];
    if (!stem) return { state: S.UNRESOLVED, reason: 'id carries no parseable numeric stem; cannot map to a per-id file' };
    const re = new RegExp(`^${stem}[A-Za-z0-9]*${suffixRe}\\.kt$`, 'i');
    const search = [dir, ...ctx.listDir(dir).filter((f) => ctx.isDir(`${dir}/${f}`)).map((f) => `${dir}/${f}`)];
    for (const d of search) {
      const hit = ctx.listDir(d).find((f) => re.test(f));
      if (hit) return { state: S.PRESENT, symbol: hit.replace(/\.kt$/, ''), evidencePath: `${d}/${hit}`,
        construct: 'type-declaration', matchRange: null, reason: `per-id ${label} file declares a type named for this id` };
    }
    return { state: S.UNRESOLVED, reason: `no file matching ${stem}*${suffixRe}.kt in ${dir} or its immediate subdirectories; strategy is not exhaustive over filenames or nesting depth, so absence is NOT proven` };
  },
});

const naAxis = (axis, why) => ({ strategyId: `n/a:${why}`, exhaustive: false, exhaustivenessEvidenceId: null, files: [], axis,
  probe: () => ({ state: S.NA, reason: why }) });

// BLOCK-LEVEL context ONLY. C3: this is NOT exerciseRoute and can never substitute for it.
const blockReach = (rootFile, hubKeyword) => ({
  strategyId: 'block-reachability:root-exists+hub-references-block', exhaustive: false, exhaustivenessEvidenceId: null,
  files: [rootFile, HUB_FILE], axis: 'blockReachability',
  probe: (_id, texts) => {
    if (texts[rootFile] == null) return { state: S.UNRESOLVED, reason: `root screen source missing: ${rootFile}` };
    if (texts[HUB_FILE] == null) return { state: S.UNRESOLVED, reason: `hub source missing: ${HUB_FILE}` };
    if (!texts[HUB_FILE].includes(hubKeyword))
      return { state: S.UNRESOLVED, reason: `hub does not reference "${hubKeyword}" (hub labels are UI strings; a miss does not prove the block unreachable)` };
    return { state: S.PRESENT, symbol: hubKeyword, evidencePath: rootFile, construct: 'block-context', matchRange: null,
      reason: 'BLOCK context only: the root screen exists and the hub references the block. This says NOTHING about whether any given exercise is reachable inside the block (see exerciseRoute).' };
  },
});

export const ADAPTERS = {
  FOCUS: {
    label: 'Enfoque', strategyVersion: ADAPTERS_VERSION,
    declaration: { ...bindingIn([`${P}/focus/domain/catalog/ExerciseCatalog.kt`], 'focus catalog', 'declaration'),
      enumerate: (texts, scan) => scan.codeTokens(texts[`${P}/focus/domain/catalog/ExerciseCatalog.kt`], /\bE\d+_[A-Z][A-Z0-9_]*\b/g) },
    mechanic: bindingIn([`${P}/focus/domain/exercise/FocusExerciseGenerators.kt`, `${P}/focus/di/FocusGraph.kt`], 'focus generator registry + graph', 'mechanic'),
    generator: perIdFile(`${P}/focus/domain/exercise`, 'Generator', 'focus generator'),
    renderer: { ...naAxis('renderer', 'FocusSessionScreen is a state-driven renderer dispatching on FocusFieldState, never on an exercise id — per-id renderer dispatch is not decidable by token probing; it needs call-graph resolution through FocusSessionStateHolder'),
      strategyId: 'state-driven-renderer:not-id-addressable',
      probe: () => ({ state: S.UNRESOLVED, reason: 'FocusSessionScreen dispatches on FocusFieldState, never on an exercise id; per-id renderer dispatch needs call-graph resolution through FocusSessionStateHolder' }) },
    exerciseRoute: bindingIn([`${P}/focus/presentation/FocusHomeStateHolder.kt`, `${P}/focus/presentation/ui/FocusRootScreen.kt`], 'focus home state holder + root screen', 'exerciseRoute'),
    blockReachability: blockReach(`${P}/focus/presentation/ui/FocusRootScreen.kt`, 'Enfoque'),
    limitations: [
      'renderer is structurally UNRESOLVED: the renderer is state-driven and not id-addressable.',
      'mechanic/exerciseRoute require a semantic binding; a bare code token is rejected.',
      'blockReachability is context only and never substitutes for exerciseRoute.',
    ],
  },
  SPEED: {
    label: 'Velocidad', strategyVersion: ADAPTERS_VERSION,
    declaration: { ...bindingIn([`${P}/speed/domain/SpeedEnums.kt`], 'speed enums', 'declaration'),
      enumerate: (texts, scan) => scan.codeTokens(texts[`${P}/speed/domain/SpeedEnums.kt`], /\bE\d+_[A-Z][A-Z0-9_]*\b/g) },
    mechanic: bindingIn([`${P}/speed/domain/exercise/SpeedPromptGenerators.kt`, `${P}/speed/presentation/SpeedSessionStateHolder.kt`], 'speed prompt generators + session state holder', 'mechanic'),
    generator: perIdFile(`${P}/speed/presentation`, 'Screen', 'speed per-exercise screen'),
    renderer: bindingIn([`${P}/speed/presentation/ui/SpeedExerciseScreen.kt`], 'speed exercise screen', 'renderer'),
    exerciseRoute: bindingIn([`${P}/speed/presentation/SpeedSessionStateHolder.kt`, `${P}/speed/presentation/ui/SpeedExerciseScreen.kt`], 'speed state holder + exercise screen', 'exerciseRoute'),
    blockReachability: blockReach(`${P}/speed/presentation/ui/SpeedRootScreen.kt`, 'Velocidad'),
    limitations: [
      'two dispatch patterns coexist (shared prompt generators; per-exercise screens one directory down). Neither probe is exhaustive.',
    ],
  },
  LOGIC: {
    label: 'Logica', strategyVersion: ADAPTERS_VERSION,
    declaration: { ...bindingIn([`${P}/logic/domain/model/LogicModels.kt`], 'logic models', 'declaration'),
      enumerate: (texts, scan) => scan.enumMembers(texts[`${P}/logic/domain/model/LogicModels.kt`], 'LogicExerciseType') },
    mechanic: bindingIn([`${P}/logic/domain/content/LogicContentProvider.kt`], 'logic content provider', 'mechanic'),
    generator: naAxis('generator', 'Logic has no per-id generator file; content is served by a shared provider'),
    renderer: bindingIn([`${P}/logic/presentation/ui/LogicRootScreen.kt`], 'logic root screen', 'renderer'),
    exerciseRoute: bindingIn([`${P}/logic/presentation/ui/LogicRootScreen.kt`], 'logic root screen', 'exerciseRoute'),
    blockReachability: blockReach(`${P}/logic/presentation/ui/LogicRootScreen.kt`, 'gica'),
    limitations: ['root screen doubles as renderer AND route source: those axes may share one observation (evidenceDependency=SHARED).'],
  },
  LANG: {
    label: 'Lenguaje', strategyVersion: ADAPTERS_VERSION,
    declaration: { ...bindingIn([`${P}/language/presentation/exercises/LanguageExerciseCatalog.kt`], 'language catalog', 'declaration'),
      enumerate: (texts, scan) => [...scan.codeTokens(texts[`${P}/language/presentation/exercises/LanguageExerciseCatalog.kt`], /\b[A-Z][A-Za-z]+Bank\b/g), 'mixed_session'] },
    mechanic: { strategyId: 'per-bank-file', exhaustive: false, exhaustivenessEvidenceId: null, files: [], axis: 'mechanic',
      probe: (id, _t, ctx) => {
        if (id === 'mixed_session') return { state: S.NA, reason: 'mixed_session is a composite over the six banks, not a bank with its own mechanic file' };
        const dir = `${P}/language/presentation/exercises`;
        const hit = ctx.listDir(dir).find((f) => f === `${id}.kt`);
        return hit ? { state: S.PRESENT, symbol: id, evidencePath: `${dir}/${hit}`, construct: 'type-declaration', matchRange: null, reason: 'bank file declares an object named for this id' }
                   : { state: S.UNRESOLVED, reason: `no ${id}.kt in ${dir}; strategy not exhaustive over legal locations` };
      } },
    generator: naAxis('generator', 'Language uses banks, not generators'),
    renderer: bindingIn([`${P}/language/presentation/ui/LanguageRootScreen.kt`], 'language root screen', 'renderer'),
    exerciseRoute: bindingIn([`${P}/language/presentation/ui/LanguageRootScreen.kt`], 'language root screen', 'exerciseRoute'),
    blockReachability: blockReach(`${P}/language/presentation/ui/LanguageRootScreen.kt`, 'Lenguaje'),
    limitations: ['root screen doubles as renderer AND route source (evidenceDependency=SHARED).'],
  },
  MEMORY: {
    label: 'Memoria', strategyVersion: ADAPTERS_VERSION,
    declaration: { ...bindingIn([`${P}/memory/domain/catalog/MemoryExerciseCatalog.kt`], 'memory catalog', 'declaration'),
      enumerate: (texts, scan) => scan.codeTokens(texts[`${P}/memory/domain/catalog/MemoryExerciseCatalog.kt`], /\bEX\d+_[A-Z][A-Z0-9_]*\b/g) },
    mechanic: bindingIn([`${P}/memory/di/MemoryGraph.kt`], 'memory graph', 'mechanic'),
    generator: perIdFile(`${P}/memory/domain/exercise`, 'Generator', 'memory generator'),
    renderer: bindingIn([`${P}/memory/presentation/ui/MemorySessionScreen.kt`], 'memory session screen', 'renderer'),
    exerciseRoute: bindingIn([`${P}/memory/presentation/ui/MemoryRootScreen.kt`], 'memory root screen', 'exerciseRoute'),
    blockReachability: blockReach(`${P}/memory/presentation/ui/MemoryRootScreen.kt`, 'Memoria'),
    limitations: [
      'the catalog is the DECLARATION source, so it is deliberately excluded from the mechanic file set: reusing it would make declaration and mechanic one observation wearing two labels.',
      'generator files are PascalCase (Ex1SpanGenerator) while ids are SCREAMING_SNAKE; the stem match is case-insensitive and not exhaustive.',
    ],
  },
  DECIDE: {
    label: 'Decisiones', strategyVersion: ADAPTERS_VERSION,
    declaration: { ...bindingIn([`${P}/decisiones/presentation/DecisionsFlowStateHolder.kt`], 'decisions flow state holder', 'declaration'),
      enumerate: (texts, scan) => scan.enumMembers(texts[`${P}/decisiones/presentation/DecisionsFlowStateHolder.kt`], 'DecisionsFlowStage') },
    mechanic: bindingIn([`${P}/decisiones/presentation/ui/DecisionsRootScreen.kt`], 'decisions root screen', 'mechanic'),
    generator: naAxis('generator', 'Decisiones models stages of one flow, not independent generators'),
    renderer: bindingIn([`${P}/decisiones/presentation/ui/DecisionsRootScreen.kt`], 'decisions root screen', 'renderer'),
    exerciseRoute: bindingIn([`${P}/decisiones/presentation/ui/DecisionsRootScreen.kt`], 'decisions root screen', 'exerciseRoute'),
    blockReachability: blockReach(`${P}/decisiones/presentation/ui/DecisionsRootScreen.kt`, 'Decisiones'),
    limitations: [
      'DecisionsFlowStage enumerates FLOW STAGES (CheckIn/BayesArena/Reversibility/Result), not exercises. Treating them as exercises is an editorial modelling decision, not a code fact.',
      'one file serves mechanic, renderer and route → those axes share one observation (evidenceDependency=SHARED).',
    ],
  },
  EMOTION: {
    label: 'Emociones', strategyVersion: ADAPTERS_VERSION,
    declaration: { ...bindingIn([`${P}/emotions/domain/catalog/EmotionCatalogIds.kt`], 'emotion catalog ids', 'declaration'),
      enumerate: (texts, scan) => scan.codeTokens(texts[`${P}/emotions/domain/catalog/EmotionCatalogIds.kt`], /\bEM\d+_[A-Z][A-Z0-9_]*\b/g) },
    mechanic: bindingIn([`${P}/emotions/domain/catalog/EmotionExerciseCatalog.kt`, `${P}/emotions/domain/exercise/EmotionExerciseGenerators.kt`], 'emotion catalog + generators', 'mechanic'),
    generator: perIdFile(`${P}/emotions/domain/exercise`, 'Generator', 'emotion generator'),
    renderer: bindingIn([`${P}/emotions/presentation/ui/EmotionSessionScreen.kt`, `${P}/emotions/presentation/EmotionSessionStateHolder.kt`], 'emotion session screen + state holder', 'renderer'),
    exerciseRoute: bindingIn([`${P}/emotions/presentation/EmotionSessionStateHolder.kt`], 'emotion session state holder', 'exerciseRoute'),
    blockReachability: blockReach(`${P}/emotions/presentation/ui/EmotionRootScreen.kt`, 'Emociones'),
    limitations: ['the generator registry keys on generator CLASS names; the id→class link is a numeric-stem convention, not a resolved reference.'],
  },
  CREATE: {
    label: 'Creatividad', strategyVersion: ADAPTERS_VERSION,
    declaration: { ...bindingIn([`${P}/creativity/domain/exercise/CreativityExerciseCatalog.kt`], 'creativity catalog', 'declaration'),
      enumerate: (texts, scan) => scan.codeTokens(texts[`${P}/creativity/domain/exercise/CreativityExerciseCatalog.kt`], /\bEX\d+_[A-Z][A-Z0-9_]*\b/g) },
    mechanic: bindingIn([`${P}/creativity/domain/exercise/CreativityExerciseEngine.kt`], 'creativity engine', 'mechanic'),
    generator: naAxis('generator', 'Creatividad uses a shared engine, not per-id generators'),
    renderer: bindingIn([`${P}/creativity/presentation/ui/CreativityRootScreen.kt`], 'creativity root screen', 'renderer'),
    exerciseRoute: bindingIn([`${P}/creativity/presentation/ui/CreativityRootScreen.kt`], 'creativity root screen', 'exerciseRoute'),
    blockReachability: blockReach(`${P}/creativity/presentation/ui/CreativityRootScreen.kt`, 'Creatividad'),
    limitations: [
      'the catalog is the DECLARATION source and is excluded from the mechanic set to keep the two axes independent.',
      'root screen doubles as renderer AND route source (evidenceDependency=SHARED).',
    ],
  },
};

// Axes recorded per id. blockReachability is CONTEXT and is deliberately NOT a required axis (C3).
export const AXES = ['declaration', 'mechanic', 'generator', 'renderer', 'exerciseRoute', 'blockReachability'];
export const CONTEXT_AXES = ['blockReachability'];
