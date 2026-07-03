// @ts-check
/**
 * paths.mjs — resolve every path the memory engine uses, robustly across
 * git worktrees and shells. Honors CLAUDE_PROJECT_DIR (set by Claude Code)
 * and falls back to walking up for a `.git` marker. Always returns
 * forward-slash POSIX paths internally; uses node:path for filesystem ops.
 *
 * INVARIANT (from the design pre-mortem): the index lives at a PER-WORKTREE,
 * gitignored location (.claude/.memory). It is a disposable cache rebuilt from
 * the Markdown source of truth, never a shared instance written by racing
 * worktrees.
 */

import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url)); // .claude/memory/lib

/** Walk up from a start dir until a `.git` entry is found. */
function findRepoRoot(start) {
  let dir = start;
  for (let i = 0; i < 40; i++) {
    if (fs.existsSync(path.join(dir, '.git'))) return dir;
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  return null;
}

/** Absolute repo root (worktree-aware). */
export function repoRoot() {
  const env = process.env.CLAUDE_PROJECT_DIR;
  if (env && fs.existsSync(env)) return path.resolve(env);
  // HERE is <root>/.claude/memory/lib → climb three to the worktree root.
  const guess = path.resolve(HERE, '..', '..', '..');
  if (fs.existsSync(path.join(guess, '.git'))) return guess;
  return findRepoRoot(HERE) || guess;
}

/** Join under the repo root with POSIX-safe segments. */
function underRoot(...segments) {
  return path.join(repoRoot(), ...segments);
}

export const ENGINE_DIR = path.resolve(HERE, '..'); // .claude/memory

export function configPath() {
  return path.join(ENGINE_DIR, 'config.json');
}

/** Source-of-truth tree (git-versioned). */
export function memoryDir() {
  return underRoot('docs', 'memory');
}
export function entriesDir() {
  return path.join(memoryDir(), 'entries');
}
export function templatesDir() {
  return path.join(memoryDir(), '_templates');
}
export function indexMarkdownPath() {
  return path.join(memoryDir(), 'MEMORY_INDEX.md');
}

/** Runtime, gitignored, per-worktree. */
export function indexDir(cfg) {
  const rel = cfg?.paths?.indexDir || '.claude/.memory';
  return underRoot(...rel.split('/'));
}
export function indexDbPath(cfg) {
  return path.join(indexDir(cfg), 'index.db');
}
export function manifestPath(cfg) {
  return path.join(indexDir(cfg), 'manifest.json');
}
export function auditDir(cfg) {
  return path.join(indexDir(cfg), 'audit');
}
export function modelCacheDir(cfg) {
  return path.join(indexDir(cfg), 'models');
}

/** The repo's existing secret scanner — reused by the redaction guard. */
export function scanSecretsPath() {
  return underRoot('functions', 'scripts', 'scan-secrets.js');
}

/** Make a path relative to the repo root with forward slashes (for citations). */
export function relToRoot(absPath) {
  return path.relative(repoRoot(), absPath).split(path.sep).join('/');
}

/** Ensure a directory exists (recursive). */
export function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

export { os };
