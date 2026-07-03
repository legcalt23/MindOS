#!/usr/bin/env bash
# graphify-refresh.sh
# Rebuild the Graphify structural-memory graph for this repository.
# Code-only, AST-based: NO LLM, NO API key, NO external/paid calls. Idempotent.
#
# Usage:  .claude/scripts/graphify-refresh.sh
# Runs from anywhere; it cd's to the repo root (this script lives in
# <repo-root>/.claude/scripts/). No personal paths, no secrets.
set -euo pipefail

# --- locate repo root (script lives in <root>/.claude/scripts/) ---------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

# --- ensure graphify is available ---------------------------------------------
# uv installs the CLI shim to ~/.local/bin; make sure it is on PATH.
export PATH="$HOME/.local/bin:$PATH"
if ! command -v graphify >/dev/null 2>&1; then
  echo "ERROR: 'graphify' is not installed or not on PATH." >&2
  echo "  Install it with:  uv tool install graphifyy   (or: pipx install graphifyy)" >&2
  echo "  Verify with:      graphify --version" >&2
  exit 1
fi

# --- keep the build strictly local/AST: neutralize every LLM backend ----------
# Only GEMINI_API_KEY / GOOGLE_API_KEY would trigger semantic doc extraction;
# clear all provider vars so a refresh can never make an external/paid call.
unset GEMINI_API_KEY GOOGLE_API_KEY GOOGLE_APPLICATION_CREDENTIALS \
      ANTHROPIC_API_KEY ANTHROPIC_BASE_URL OPENAI_API_KEY OPENAI_BASE_URL 2>/dev/null || true

echo "==> Refreshing Graphify graph (code-only, no LLM) at: $REPO_ROOT"

# --- prefer 'update' (incremental, no LLM); fall back to 'extract' ------------
if graphify --help 2>/dev/null | grep -q 'update <path>'; then
  graphify update .
else
  # Fallback for older CLIs without 'update'. NOTE: on a corpus that contains
  # docs/images this headless path may require an LLM API key. --out . writes
  # into ./graphify-out/ (graphify appends the graphify-out/ segment itself).
  graphify extract . --out .
fi

# --- verify required artifacts exist and are non-empty ------------------------
GRAPH="graphify-out/graph.json"
REPORT="graphify-out/GRAPH_REPORT.md"
missing=0
for f in "$GRAPH" "$REPORT"; do
  if [ ! -s "$f" ]; then
    echo "ERROR: missing or empty artifact: $f" >&2
    missing=1
  fi
done
if [ "$missing" -ne 0 ]; then
  echo "Refresh did NOT produce the expected artifacts." >&2
  exit 2
fi

# --- final summary ------------------------------------------------------------
GRAPH_SIZE="$(wc -c < "$GRAPH" | tr -d ' ')"
NODES_LINE="$(grep -m1 -E 'nodes .*edges .*communities' "$REPORT" 2>/dev/null || true)"
BUILT_FROM="$(grep -m1 -E 'Built from commit' "$REPORT" 2>/dev/null || true)"
echo
echo "==> Graph refreshed successfully."
echo "    graph.json : ${GRAPH_SIZE} bytes"
[ -n "$NODES_LINE" ] && echo "    summary    : ${NODES_LINE#- }"
[ -n "$BUILT_FROM" ] && echo "    ${BUILT_FROM#- }"
echo "    report     : $REPORT"
echo "    Query it with:  graphify query \"<question>\""
