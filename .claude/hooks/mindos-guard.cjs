#!/usr/bin/env node
'use strict';

/**
 * MindOS lightweight guard hook (non-blocking, warn-only).
 *
 * Ported/adapted from the ECC toolkit for MindOS's stack. It NEVER blocks a
 * tool call (always exits 0); it only surfaces warnings to the user
 * (systemMessage) and context to Claude (additionalContext).
 *
 * Wired in .claude/settings.json (all matchers call this same self-locating script):
 *   - PreToolUse  (Write|Edit|MultiEdit): config-protection + secret-scan
 *   - PreToolUse  (Read|Glob):            Graphify orientation reminder (source/docs)
 *   - PreToolUse  (Bash):                 Graphify orientation reminder (grep/find)
 *   - PostToolUse (Bash):                 gradle/detekt failure reminder
 *
 * Self-locating: invoked via `node -e "... require(CLAUDE_PROJECT_DIR/.claude/hooks/mindos-guard.cjs)"`
 * so it works under git worktrees and on any shell (pwsh/cmd/bash).
 */

const fs = require('fs');

function readInput() {
  try {
    return JSON.parse(fs.readFileSync(0, 'utf8') || '{}');
  } catch {
    return {};
  }
}

function emit(obj) {
  try {
    process.stdout.write(JSON.stringify(obj));
  } catch {
    /* ignore */
  }
}

const data = readInput();
const event = data.hook_event_name || '';
const tool = data.tool_name || '';
const ti = data.tool_input || {};

const notes = [];        // -> additionalContext (seen by Claude)
const userWarnings = []; // -> systemMessage (seen by the user)

// ─── Graphify orientation (shared by the Read/Glob and Bash-grep reminders) ──
// Ported from the two old python3 hooks that were a silent no-op on Windows
// (python3 = Microsoft Store stub). Node parse is cross-shell + worktree-safe.
const path = require('path');
const projectDir = process.env.CLAUDE_PROJECT_DIR || process.cwd();
function graphExists() {
  try {
    return fs.existsSync(path.join(projectDir, 'graphify-out', 'graph.json'));
  } catch {
    return false;
  }
}
const GRAPHIFY_NOTE =
  '📈 Graphify: existe `graphify-out/graph.json`. Orienta con `graphify query "<pregunta>"` ' +
  '(o `explain`/`path`/`affected`) antes de un Grep/Read amplio; lee fuente directa solo para ' +
  'editar/verificar líneas concretas. Regla completa en CLAUDE.md.';

// ─── PreToolUse: Write / Edit / MultiEdit ────────────────────────────────────
if (tool === 'Write' || tool === 'Edit' || tool === 'MultiEdit') {
  const filePath = String(ti.file_path || '').replace(/\\/g, '/');
  const base = filePath.split('/').pop() || '';

  // Reconstruct the content being written.
  let content = '';
  if (typeof ti.content === 'string') content += ti.content;
  if (typeof ti.new_string === 'string') content += '\n' + ti.new_string;
  if (Array.isArray(ti.edits)) {
    for (const e of ti.edits) {
      if (e && typeof e.new_string === 'string') content += '\n' + e.new_string;
    }
  }

  // 1) Config protection — security-critical files that should not be weakened.
  const PROTECTED = new Set([
    'detekt-baseline.xml', 'firestore.rules', 'storage.rules', 'firestore.indexes.json',
    'libs.versions.toml', 'google-services.json', 'GoogleService-Info.plist',
    'network_security_config.xml', 'backup_rules.xml', 'data_extraction_rules.xml',
    'firebase.json', '.firebaserc',
  ]);
  if (PROTECTED.has(base)) {
    notes.push(
      `⚠️ Config protegida: \`${base}\`. Antes de relajar una regla o silenciar un warning, ` +
      `arregla el código. Invariantes MindOS: los límites de \`firestore.rules\` deben ir en par ` +
      `con los \`.limit()\` del cliente; el baseline de detekt solo se regenera tras auditar ` +
      `UnusedParameter/UnusedPrivateProperty; signing/keystore nunca hardcodeados.`
    );
    userWarnings.push(`MindOS guard · config protegida: ${base}`);
  }

  // 2) Secret scan — high-confidence patterns only (warn, never block).
  //    Firebase CLIENT config (google-services.json / GoogleService-Info.plist) legitimately
  //    contains a public API key, so it is excluded to avoid false positives.
  const skipSecret = base === 'google-services.json' || base === 'GoogleService-Info.plist';
  if (!skipSecret && content) {
    const SECRETS = [
      [/-----BEGIN [A-Z ]*PRIVATE KEY-----/, 'clave privada PEM'],
      [/"private_key"\s*:\s*"-----BEGIN/, 'service account GCP'],
      [/AKIA[0-9A-Z]{16}/, 'AWS access key'],
      [/xox[baprs]-[0-9A-Za-z-]{10,}/, 'token Slack'],
      [/gh[pousr]_[0-9A-Za-z]{36,}/, 'token GitHub'],
      [/AIza[0-9A-Za-z_-]{35}/, 'Google API key'],
    ];
    const hits = [];
    for (const [re, label] of SECRETS) {
      if (re.test(content)) hits.push(label);
    }
    if (hits.length) {
      notes.push(
        `🔴 Posible secreto en \`${base}\`: ${hits.join(', ')}. NO escribas secretos en archivos ` +
        `versionados. Usa variables de entorno, \`gradle.properties\` (gitignored) o Secret Manager.`
      );
      userWarnings.push(`MindOS guard · posible secreto en ${base}: ${hits.join(', ')}`);
    }
  }
}

// ─── PreToolUse: Read / Glob of source or docs → Graphify orientation ────────
if (event === 'PreToolUse' && (tool === 'Read' || tool === 'Glob') && graphExists()) {
  const SRC_EXT = new Set([
    '.py', '.js', '.ts', '.tsx', '.jsx', '.astro', '.vue', '.svelte', '.go', '.rs', '.java',
    '.rb', '.c', '.h', '.cpp', '.hpp', '.cc', '.cs', '.kt', '.kts', '.swift', '.php', '.scala',
    '.lua', '.sh', '.md', '.rst', '.txt', '.mdx',
  ]);
  const raw = [ti.file_path, ti.pattern, ti.path]
    .map((v) => String(v || '').replace(/\\/g, '/').toLowerCase())
    .join(' ');
  if (!raw.includes('graphify-out/')) {
    const isSource = raw.split(/\s+/).some((token) => {
      const leaf = token.split('/').pop() || '';
      const dot = leaf.lastIndexOf('.');
      return dot > 0 && SRC_EXT.has(leaf.slice(dot));
    });
    if (isSource) notes.push(GRAPHIFY_NOTE);
  }
}

// ─── PreToolUse: Bash grep/find → Graphify orientation ───────────────────────
if (event === 'PreToolUse' && tool === 'Bash' && graphExists()) {
  const cmd = String(ti.command || '');
  if (/(^|[\s|&;(])(grep|rg|ripgrep|find|fd|ack|ag)(\s|$)/.test(cmd)) {
    notes.push(GRAPHIFY_NOTE);
  }
}

// ─── PostToolUse: Bash gradle/detekt failure reminder ────────────────────────
if (event === 'PostToolUse' && tool === 'Bash') {
  const cmd = String(ti.command || '');
  let resp = '';
  try {
    resp = typeof data.tool_response === 'string'
      ? data.tool_response
      : JSON.stringify(data.tool_response || '');
  } catch {
    /* ignore */
  }
  const isGradle = /gradlew|gradle\s/.test(cmd);
  const failed = /BUILD FAILED|Execution failed for task|Task .* FAILED/.test(resp);
  if (isGradle && failed) {
    if (/detekt/.test(cmd)) {
      notes.push(
        '🔧 detekt falló. Corrige el código y re-ejecuta — no regeneres el baseline para silenciar. ' +
        '`/kotlin-review` ayuda con findings de calidad.'
      );
    } else {
      notes.push(
        '🔧 Build de Gradle falló. `/gradle-build` o `/kotlin-build` aplican fixes incrementales ' +
        'mínimos (un error a la vez, verificando cada cambio).'
      );
    }
  }
}

// ─── Emit (non-blocking) ─────────────────────────────────────────────────────
if (notes.length) {
  const result = { continue: true };
  if (userWarnings.length) result.systemMessage = userWarnings.join(' | ');
  result.hookSpecificOutput = {
    hookEventName: event || 'PreToolUse',
    additionalContext: notes.join('\n'),
  };
  emit(result);
}

process.exit(0);
