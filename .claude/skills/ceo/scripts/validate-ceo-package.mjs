#!/usr/bin/env node
// validate-ceo-package.mjs — deterministic structural linter for a /ceo execution package.
// Zero dependencies (Node >=18, ESM). Enforces what the JSON schema cannot:
// acyclic DAG, cross-references, real-entity existence (no hallucinated agents/commands/paths),
// write-set serialization, requirement traceability, verifiable gates, destructive-op & placeholder bans.
//
// Usage:
//   node validate-ceo-package.mjs <package.json>      Validate a package. Exit 1 on FAIL.
//   node validate-ceo-package.mjs <package.json> --json   Machine-readable report.
//   node validate-ceo-package.mjs --selftest          Run embedded good/bad fixtures. Exit 1 if the linter misbehaves.
//
// The package is DATA. This linter never executes anything from it; it only inspects structure.

import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SUPPORTED_SCHEMA_MAJOR = 1;
const SUPPORTED_SCHEMA = '1.0.0';

const scriptDir = dirname(fileURLToPath(import.meta.url));           // .claude/skills/ceo/scripts
const claudeDir = resolve(scriptDir, '..', '..', '..');             // .claude
const repoRoot = resolve(claudeDir, '..');                          // repo root

function discover(dir) {
  const p = join(claudeDir, dir);
  if (!existsSync(p)) return new Set();
  return new Set(readdirSync(p).filter(f => f.endsWith('.md')).map(f => f.slice(0, -3)));
}
const AGENTS = discover('agents');
const COMMANDS = discover('commands');
const OWNER_ALLOW = new Set(['inline', 'main-session', 'self', 'founder', 'ceo']);

const DESTRUCTIVE = /git\s+reset\s+--hard|git\s+clean|push\s+--force|--force-with-lease|--force\b|\brm\s+-rf\b|checkout\s+--\s|reset\s+--hard/i;
const PLACEHOLDER = /\bTODO\b|\bTBD\b|\bXXX\b|\bFIXME\b|\[FALTA|<[a-z][a-z0-9_ /-]{1,40}>|placeholder|añadir contexto aquí/i;

const majorOf = v => String(v || '').split('.')[0];

// ---- core validation -------------------------------------------------------
function validate(pkg) {
  const errors = [];
  const warnings = [];
  const E = (code, msg, where) => errors.push({ code, msg, where });
  const W = (code, msg, where) => warnings.push({ code, msg, where });

  const meta = pkg.meta || {};
  const mode = pkg.mode;
  const nodes = Array.isArray(pkg.nodes) ? pkg.nodes : [];
  const reqs = Array.isArray(pkg.requirements) ? pkg.requirements : [];
  const externals = new Set(Array.isArray(pkg.external_artifacts) ? pkg.external_artifacts : []);
  const nodeById = new Map();

  // meta / version
  if (!meta.schema_version) E('missing_schema_version', 'meta.schema_version is required', 'meta');
  else if (majorOf(meta.schema_version) !== String(SUPPORTED_SCHEMA_MAJOR))
    E('version_incompatible', `schema_version ${meta.schema_version} incompatible with validator ${SUPPORTED_SCHEMA} (migration required)`, 'meta');
  if (!meta.skill_version) W('missing_skill_version', 'meta.skill_version recommended', 'meta');

  if (!['directo', 'profesional', 'enterprise', 'critico'].includes(mode))
    E('invalid_mode', `mode must be one of directo|profesional|enterprise|critico (got ${JSON.stringify(mode)})`, 'mode');
  if (!pkg.request || !pkg.request.original_text) E('missing_request', 'request.original_text is required', 'request');
  if (!nodes.length) E('no_nodes', 'at least one node is required', 'nodes');

  // node identity
  for (const n of nodes) {
    if (!n.id) { E('node_no_id', 'node missing id', JSON.stringify(n).slice(0, 60)); continue; }
    if (nodeById.has(n.id)) E('duplicate_node_id', `duplicate node id ${n.id}`, n.id);
    nodeById.set(n.id, n);
  }
  const NODE_TYPES = new Set(['DISCOVERY', 'RESEARCH', 'DECISION', 'SPECIFICATION', 'IMPLEMENTATION', 'MIGRATION', 'TEST', 'REVIEW', 'RED_TEAM', 'INTEGRATION', 'GATE', 'DOCUMENTATION']);
  for (const n of nodes) {
    if (!n.type || !NODE_TYPES.has(n.type)) E('bad_node_type', `node ${n.id}: invalid type ${JSON.stringify(n.type)}`, n.id);
    if (!n.objective) E('node_no_objective', `node ${n.id}: objective required`, n.id);
    if (!n.owner) E('node_no_owner', `node ${n.id}: owner required`, n.id);
    if (!Array.isArray(n.acceptance_criteria) || !n.acceptance_criteria.length) E('node_no_acceptance', `node ${n.id}: acceptance_criteria required`, n.id);
    // exit conditions expected on non-trivial nodes
    if (!(Array.isArray(n.exit_conditions) && n.exit_conditions.length)) W('node_no_exit', `node ${n.id}: exit_conditions recommended`, n.id);
    // dependency refs
    for (const d of n.dependencies || []) if (!nodeById.has(d)) E('dangling_dependency', `node ${n.id}: dependency ${d} does not exist`, n.id);
    // hallucinated agents
    for (const who of [n.owner, n.agent]) {
      if (who && !OWNER_ALLOW.has(who) && !AGENTS.has(who)) E('hallucinated_agent', `node ${n.id}: agent/owner '${who}' not found in .claude/agents`, n.id);
    }
    // hallucinated commands
    for (const c of n.commands || []) {
      const name = String(c).replace(/^\//, '');
      if (!COMMANDS.has(name)) E('hallucinated_command', `node ${n.id}: command '${c}' not found in .claude/commands`, n.id);
    }
    // gates must be verifiable
    if (['GATE', 'REVIEW', 'RED_TEAM', 'TEST'].includes(n.type)) {
      const hasV = (Array.isArray(n.verification) && n.verification.length) || (Array.isArray(n.evidence_required) && n.evidence_required.length);
      if (!hasV) E('unverifiable_gate', `node ${n.id} (${n.type}): needs verification or evidence_required`, n.id);
    }
    // rollback for risky writers
    const writes = Array.isArray(n.write_set) && n.write_set.length;
    const risky = ['high', 'critical'].includes(n.risk) || ['IMPLEMENTATION', 'MIGRATION'].includes(n.type);
    if (writes && risky && !n.rollback) {
      if (mode === 'critico') E('missing_rollback', `node ${n.id}: rollback required for risky writer in critico mode`, n.id);
      else W('missing_rollback', `node ${n.id}: rollback recommended for risky writer`, n.id);
    }
    // turn/exit bound for delegated agents (no infinite loops)
    if (n.agent && !OWNER_ALLOW.has(n.agent) && !n.max_turns && !(Array.isArray(n.exit_conditions) && n.exit_conditions.length))
      W('no_turn_bound', `node ${n.id}: delegated agent without max_turns or exit_conditions`, n.id);
  }

  // cycle detection (deps: dep -> node)
  let cycles = 0;
  const color = new Map();
  const cyclePath = [];
  const dfs = (id) => {
    color.set(id, 1); cyclePath.push(id);
    for (const d of (nodeById.get(id)?.dependencies || [])) {
      if (!nodeById.has(d)) continue;
      if (color.get(d) === 1) { cycles++; E('cycle', `dependency cycle: ${[...cyclePath, d].join(' -> ')}`, id); }
      else if (!color.get(d)) dfs(d);
    }
    color.set(id, 2); cyclePath.pop();
  };
  for (const n of nodes) if (n.id && !color.get(n.id)) dfs(n.id);

  // reachability (transitive deps) for write-conflict + parallel checks
  const reaches = (a, b, seen = new Set()) => {
    if (a === b) return true;
    for (const d of (nodeById.get(a)?.dependencies || [])) {
      if (seen.has(d)) continue; seen.add(d);
      if (d === b || reaches(d, b, seen)) return true;
    }
    return false;
  };
  const related = (a, b) => reaches(a, b) || reaches(b, a);

  // write-set conflicts (unordered concurrent writers to the same path)
  let writeConflicts = 0;
  const withWrites = nodes.filter(n => Array.isArray(n.write_set) && n.write_set.length);
  for (let i = 0; i < withWrites.length; i++) for (let j = i + 1; j < withWrites.length; j++) {
    const a = withWrites[i], b = withWrites[j];
    const overlap = a.write_set.filter(p => b.write_set.includes(p));
    if (!overlap.length) continue;
    const sameGroup = a.parallel_group && a.parallel_group === b.parallel_group;
    if (sameGroup || !related(a.id, b.id)) {
      writeConflicts++;
      E('write_conflict', `nodes ${a.id} & ${b.id} both write [${overlap.join(', ')}] without ordering (serialize or split ownership)`, `${a.id},${b.id}`);
    }
  }

  // parallel group must not contain a dependency relation
  const groups = new Map();
  for (const n of nodes) if (n.parallel_group) (groups.get(n.parallel_group) || groups.set(n.parallel_group, []).get(n.parallel_group)).push(n.id);
  for (const [g, ids] of groups) for (let i = 0; i < ids.length; i++) for (let j = i + 1; j < ids.length; j++)
    if (related(ids[i], ids[j])) E('invalid_parallel', `parallel_group '${g}': ${ids[i]} and ${ids[j]} have a dependency relation`, g);

  // artifact flow
  const produced = new Set();
  for (const n of nodes) for (const o of n.output_artifacts || []) produced.add(o);
  const consumed = new Set();
  for (const n of nodes) for (const inp of n.input_artifacts || []) consumed.add(inp);
  let unresolvedInputs = 0;
  for (const n of nodes) for (const inp of n.input_artifacts || []) {
    if (!produced.has(inp) && !externals.has(inp)) { unresolvedInputs++; E('unresolved_input', `node ${n.id}: input '${inp}' has no producer and is not in external_artifacts`, n.id); }
  }
  let orphanOutputs = 0;
  for (const n of nodes) for (const o of n.output_artifacts || []) {
    if (!consumed.has(o) && !['INTEGRATION', 'DOCUMENTATION'].includes(n.type) && n.handoff_to !== 'final') { orphanOutputs++; W('orphan_output', `node ${n.id}: output '${o}' has no consumer (mark final or wire a consumer)`, n.id); }
  }

  // requirement traceability
  let traced = 0;
  const TERMINAL = new Set(['out_of_scope', 'deferred', 'blocked', 'done']);
  for (const r of reqs) {
    if (!r.id) { E('req_no_id', 'requirement missing id', ''); continue; }
    const material = r.material !== false && (r.material === true || ['P0', 'P1'].includes(r.priority) || ['functional', 'invariant'].includes(r.type) || r.material === undefined);
    if (r.execution_node) {
      if (!nodeById.has(r.execution_node)) E('req_dangling_node', `requirement ${r.id}: execution_node ${r.execution_node} does not exist`, r.id);
      else traced++;
    } else if (material && !TERMINAL.has(r.status)) {
      E('untraced_requirement', `requirement ${r.id} is material but has no execution_node and status='${r.status}'`, r.id);
    }
    if (r.verification_gate) {
      const g = nodeById.get(r.verification_gate);
      if (!g) E('req_dangling_gate', `requirement ${r.id}: verification_gate ${r.verification_gate} does not exist`, r.id);
      else if (!['GATE', 'TEST', 'REVIEW', 'RED_TEAM'].includes(g.type)) E('bad_verification_gate', `requirement ${r.id}: verification_gate ${r.verification_gate} is type ${g.type}, expected GATE/TEST/REVIEW`, r.id);
    }
  }

  // integrator for multi-flow enterprise/critico
  const leaves = nodes.filter(n => !nodes.some(m => (m.dependencies || []).includes(n.id)));
  const hasIntegrator = nodes.some(n => n.type === 'INTEGRATION') || (pkg.integrator && pkg.integrator.present);
  if (['enterprise', 'critico'].includes(mode) && leaves.length > 1 && !hasIntegrator)
    E('missing_integrator', `mode ${mode} with ${leaves.length} terminal flows but no INTEGRATION node`, 'nodes');

  // referenced paths existence
  let hallucinatedPaths = 0;
  for (const rp of pkg.referenced_paths || []) {
    if (rp.must_exist && !existsSync(join(repoRoot, rp.path))) { hallucinatedPaths++; E('hallucinated_path', `referenced_paths: '${rp.path}' marked must_exist but not found in repo`, rp.path); }
  }

  // deep string scans: placeholders + destructive ops
  let placeholders = 0, destructive = 0;
  const scan = (val, path, inAcceptance) => {
    if (typeof val === 'string') {
      if (PLACEHOLDER.test(val)) { placeholders++; (inAcceptance ? E : W)('placeholder', `unresolved placeholder in ${path}: "${val.slice(0, 60)}"`, path); }
      if (/verification|rollback|tools|resume_instructions/.test(path) && DESTRUCTIVE.test(val)) { destructive++; E('destructive_op', `destructive/irreversible op in ${path}: "${val.slice(0, 60)}"`, path); }
    } else if (Array.isArray(val)) val.forEach((v, i) => scan(v, `${path}[${i}]`, inAcceptance));
    else if (val && typeof val === 'object') for (const k of Object.keys(val)) scan(val[k], `${path}.${k}`, inAcceptance || /acceptance_criteria|verification/.test(k));
  };
  scan(pkg, '$', false);

  // proportionality: directo must stay lean
  if (mode === 'directo') {
    if (nodes.length > 1) W('overengineered_direct', `directo mode with ${nodes.length} nodes (expected 1)`, 'mode');
    if (nodes.some(n => n.agent && !OWNER_ALLOW.has(n.agent))) W('overengineered_direct', 'directo mode delegates to a subagent (expected inline)', 'mode');
    if ((pkg.reviews || []).length || hasIntegrator) W('overengineered_direct', 'directo mode has reviews/integrator', 'mode');
  }

  const metrics = {
    requirements_total: reqs.length,
    requirements_traced: traced,
    graph_nodes: nodes.length,
    unresolved_inputs: unresolvedInputs,
    orphan_outputs: orphanOutputs,
    cycles,
    write_conflicts: writeConflicts,
    hallucinated_entities: errors.filter(e => e.code === 'hallucinated_agent' || e.code === 'hallucinated_command' || e.code === 'hallucinated_path').length,
    unverifiable_gates: errors.filter(e => e.code === 'unverifiable_gate').length,
    unresolved_placeholders: placeholders,
    destructive_ops: destructive,
    context_budget: (pkg.context_items || []).length,
  };
  return { schema_version: meta.schema_version || null, status: errors.length ? 'FAIL' : 'PASS', errors, warnings, metrics };
}

// ---- rendering -------------------------------------------------------------
function render(r) {
  const lines = [];
  lines.push(`validation:`);
  lines.push(`  schema_version: ${r.schema_version}`);
  lines.push(`  status: ${r.status}`);
  lines.push(`  errors: ${r.errors.length}`);
  for (const e of r.errors) lines.push(`    - [${e.code}] ${e.msg}`);
  lines.push(`  warnings: ${r.warnings.length}`);
  for (const w of r.warnings) lines.push(`    - [${w.code}] ${w.msg}`);
  lines.push(`  metrics:`);
  for (const [k, v] of Object.entries(r.metrics)) lines.push(`    ${k}: ${v}`);
  return lines.join('\n');
}

// ---- fixtures + selftest ---------------------------------------------------
function goodFixture() {
  return {
    meta: { schema_version: '1.0.0', skill_version: '2.0.0', repository_commit: 'deadbeef' },
    request: { original_text: 'add exercise X end to end' },
    mode: 'enterprise',
    external_artifacts: ['founder_request', 'repo_state'],
    requirements: [
      { id: 'R1', original_text: 'exercise playable', type: 'functional', priority: 'P1', status: 'traced', execution_node: 'impl_engine', verification_gate: 'gate_out' },
      { id: 'R2', original_text: 'honest scoring null!=0', type: 'invariant', priority: 'P0', status: 'traced', execution_node: 'impl_engine', verification_gate: 'gate_out' },
    ],
    nodes: [
      { id: 'disco', type: 'DISCOVERY', objective: 'map current code', owner: 'inline', input_artifacts: ['repo_state'], output_artifacts: ['findings'], acceptance_criteria: ['files listed'], exit_conditions: ['map produced'] },
      { id: 'impl_engine', type: 'IMPLEMENTATION', objective: 'scoring engine', owner: 'cognitive-engine-dev', agent: 'cognitive-engine-dev', dependencies: ['disco'], input_artifacts: ['findings'], output_artifacts: ['engine_code'], write_set: ['shared/a/Engine.kt'], parallel_group: 'build', acceptance_criteria: ['deterministic'], verification: [':shared:desktopTest'], exit_conditions: ['tests green'], rollback: 'revert Engine.kt', max_turns: 20 },
      { id: 'impl_ui', type: 'IMPLEMENTATION', objective: 'screen', owner: 'compose-ui-dev', agent: 'compose-ui-dev', dependencies: ['disco'], input_artifacts: ['findings'], output_artifacts: ['ui_code'], write_set: ['shared/a/Screen.kt'], parallel_group: 'build', acceptance_criteria: ['renders'], verification: [':androidApp:compileDebugKotlin'], exit_conditions: ['compiles'], rollback: 'revert Screen.kt', max_turns: 20 },
      { id: 'gate_out', type: 'GATE', objective: 'exit gate', owner: 'diff-auditor', agent: 'diff-auditor', dependencies: ['impl_engine', 'impl_ui'], input_artifacts: ['engine_code', 'ui_code'], output_artifacts: ['verdict'], acceptance_criteria: ['no P0/P1'], verification: ['efficacy-guardian + diff-auditor on final diff'], evidence_required: ['diff', 'test output'], exit_conditions: ['APTO'], handoff_to: 'final' },
    ],
    integrator: { present: true, node: 'gate_out' },
    referenced_paths: [{ path: '.claude/skills/ceo/SKILL.md', must_exist: true }],
  };
}

function badFixture() {
  return {
    meta: { schema_version: '9.9.9' }, // version_incompatible + missing skill_version
    request: { original_text: 'do stuff' },
    mode: 'enterprise',
    requirements: [
      { id: 'RX', original_text: 'must ship', type: 'functional', priority: 'P0', status: 'in_scope' }, // untraced_requirement
    ],
    nodes: [
      { id: 'a', type: 'IMPLEMENTATION', objective: 'x', owner: 'inline', dependencies: ['b'], write_set: ['same.kt'], parallel_group: 'g', acceptance_criteria: ['<TBD>'] }, // placeholder_in_acceptance, cycle, write_conflict
      { id: 'b', type: 'IMPLEMENTATION', objective: 'y', owner: 'nonexistent-agent', agent: 'nonexistent-agent', dependencies: ['a'], write_set: ['same.kt'], parallel_group: 'g', acceptance_criteria: ['ok'], rollback: 'git reset --hard HEAD~1' }, // hallucinated_agent, cycle, write_conflict, destructive_op
      { id: 'g', type: 'GATE', objective: 'gate', owner: 'inline', acceptance_criteria: ['done'] }, // unverifiable_gate
    ],
    referenced_paths: [{ path: 'does/not/exist.kt', must_exist: true }], // hallucinated_path
  };
}

function selftest() {
  const good = validate(goodFixture());
  const bad = validate(badFixture());
  const problems = [];
  if (good.status !== 'PASS') problems.push(`good fixture should PASS, got ${good.status}: ${good.errors.map(e => e.code).join(',')}`);
  const need = ['version_incompatible', 'untraced_requirement', 'placeholder', 'cycle', 'write_conflict', 'hallucinated_agent', 'destructive_op', 'unverifiable_gate', 'hallucinated_path'];
  const got = new Set(bad.errors.map(e => e.code));
  for (const c of need) if (!got.has(c)) problems.push(`bad fixture missing expected error: ${c}`);
  if (bad.status !== 'FAIL') problems.push('bad fixture should FAIL');

  console.log('SELFTEST — good fixture:');
  console.log(render(good));
  console.log('\nSELFTEST — bad fixture:');
  console.log(render(bad));
  console.log('\n' + (problems.length ? 'SELFTEST FAIL:\n  - ' + problems.join('\n  - ') : 'SELFTEST OK: linter behaves as specified.'));
  console.log(`registry: ${AGENTS.size} agents, ${COMMANDS.size} commands discovered from ${claudeDir}`);
  return problems.length === 0;
}

// ---- CLI -------------------------------------------------------------------
const args = process.argv.slice(2);
if (args.includes('--selftest')) {
  process.exit(selftest() ? 0 : 1);
} else if (args[0] && !args[0].startsWith('--')) {
  let pkg;
  try { pkg = JSON.parse(readFileSync(args[0], 'utf8')); }
  catch (e) { console.error(`cannot read/parse ${args[0]}: ${e.message}`); process.exit(2); }
  const r = validate(pkg);
  console.log(args.includes('--json') ? JSON.stringify(r, null, 2) : render(r));
  process.exit(r.status === 'PASS' ? 0 : 1);
} else {
  console.error('usage: node validate-ceo-package.mjs <package.json> [--json] | --selftest');
  process.exit(2);
}

export { validate };
