/**
 * infra-firebase-config.test.ts
 *
 * Smoke test for firebase.json — the Firebase-stack equivalent of validating
 * a docker-compose.yml. We verify:
 *   - The file is valid JSON
 *   - Required top-level sections exist (firestore, storage, functions, emulators)
 *   - All four required emulators (auth, firestore, functions, storage) are configured
 *   - Function deploy pre-hooks include the secret scan, typecheck, and tests
 *   - The functions ignore list keeps secrets out of the deploy bundle
 */

import * as fs from 'fs';
import * as path from 'path';

const FIREBASE_JSON_PATH = path.resolve(__dirname, '..', '..', 'firebase.json');

interface FirebaseConfig {
  firestore?: { rules?: string; indexes?: string };
  storage?: { rules?: string };
  functions?: Array<{
    source: string;
    ignore?: string[];
    predeploy?: string[];
  }>;
  emulators?: {
    auth?: { port: number };
    firestore?: { port: number };
    functions?: { port: number };
    storage?: { port: number };
    ui?: { enabled?: boolean; port?: number };
    singleProjectMode?: boolean;
  };
}

let config: FirebaseConfig;

beforeAll(() => {
  const raw = fs.readFileSync(FIREBASE_JSON_PATH, 'utf8');
  config = JSON.parse(raw) as FirebaseConfig;
});

// ── Structural ───────────────────────────────────────────────────────────────

describe('firebase.json structure', () => {
  it('is valid JSON and contains required top-level keys', () => {
    expect(config.firestore).toBeDefined();
    expect(config.storage).toBeDefined();
    expect(config.functions).toBeDefined();
    expect(config.emulators).toBeDefined();
  });

  it('references firestore rules + indexes files', () => {
    expect(config.firestore?.rules).toBe('firestore.rules');
    expect(config.firestore?.indexes).toBe('firestore.indexes.json');
  });

  it('references storage rules file', () => {
    expect(config.storage?.rules).toBe('storage.rules');
  });
});

// ── Emulators ────────────────────────────────────────────────────────────────

describe('firebase.json — emulators block', () => {
  it('defines all four required emulators', () => {
    expect(config.emulators?.auth?.port).toBeGreaterThan(0);
    expect(config.emulators?.firestore?.port).toBeGreaterThan(0);
    expect(config.emulators?.functions?.port).toBeGreaterThan(0);
    expect(config.emulators?.storage?.port).toBeGreaterThan(0);
  });

  it('assigns each emulator a unique port', () => {
    const ports = [
      config.emulators?.auth?.port,
      config.emulators?.firestore?.port,
      config.emulators?.functions?.port,
      config.emulators?.storage?.port,
      config.emulators?.ui?.port,
    ].filter((p): p is number => typeof p === 'number');
    const unique = new Set(ports);
    expect(unique.size).toBe(ports.length);
  });

  it('enables the emulator UI for local development', () => {
    expect(config.emulators?.ui?.enabled).toBe(true);
  });
});

// ── Functions deploy hardening ───────────────────────────────────────────────

describe('firebase.json — functions deploy', () => {
  it('declares the functions codebase', () => {
    expect(Array.isArray(config.functions)).toBe(true);
    expect(config.functions?.length).toBeGreaterThan(0);
    expect(config.functions?.[0].source).toBe('functions');
  });

  it('ignore list excludes secrets and node_modules from deploy bundle', () => {
    const ignore = config.functions?.[0].ignore ?? [];
    expect(ignore).toContain('node_modules');
    expect(ignore).toContain('.git');
    expect(ignore).toContain('.env');
    expect(ignore).toContain('.env.*');
    expect(ignore).toContain('*.log');
  });

  it('predeploy hook runs secret scan, typecheck, tests, then build', () => {
    const predeploy = config.functions?.[0].predeploy ?? [];
    const joined = predeploy.join(' | ');
    expect(joined).toContain('scan:secrets');
    expect(joined).toContain('typecheck');
    expect(joined).toContain('test');
    expect(joined).toContain('build');

    // Order matters: scan first (cheap, fail-fast), build last
    const idxScan  = predeploy.findIndex((c) => c.includes('scan:secrets'));
    const idxBuild = predeploy.findIndex((c) => c.includes('build'));
    expect(idxScan).toBeGreaterThan(-1);
    expect(idxBuild).toBeGreaterThan(idxScan);
  });
});
