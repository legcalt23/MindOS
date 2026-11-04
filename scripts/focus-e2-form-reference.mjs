/**
 * Independent reference for E2's drawn form — the derivation `E2FormGoldenTest`'s literals come from.
 *
 * That test's KDoc used to state that its literals' independence was REPORTED, not VERIFIED, because the
 * reference "is not committed to this repo". This file is that reference, committed. It is written from the
 * PUBLISHED algorithms (splitmix64, and the Kotlin standard library's documented derivations on top of
 * `nextBits`), in a different language, and it never reads the Kotlin it checks.
 *
 * It is validated against anchors that were themselves independently established, before it is trusted to
 * produce anything: splitmix64's own canonical vector for seed 0, plus every vector `DeterministicRngGoldenTest`
 * already pins. If any anchor fails, the script exits non-zero and prints nothing usable.
 *
 * Run:  node scripts/focus-e2-form-reference.mjs
 */

const MASK64 = (1n << 64n) - 1n;
const GOLDEN_GAMMA = 0x9e3779b97f4a7c15n;
const MIX_1 = 0xbf58476d1ce4e5b9n;
const MIX_2 = 0x94d049bb133111ebn;

const toSigned64 = (v) => BigInt.asIntN(64, v);
const toSigned32 = (v) => Number(BigInt.asIntN(32, BigInt(v)));

/** splitmix64, exactly as published. */
class SplitMix64 {
  constructor(seed) {
    this.state = BigInt.asUintN(64, BigInt(seed));
  }

  next64() {
    this.state = (this.state + GOLDEN_GAMMA) & MASK64;
    let z = this.state;
    z = ((z ^ (z >> 30n)) * MIX_1) & MASK64;
    z = ((z ^ (z >> 27n)) * MIX_2) & MASK64;
    return z ^ (z >> 31n);
  }

  /** `DeterministicRng.nextBits`: take the high bits of one draw. */
  nextBits(bitCount) {
    if (bitCount === 0) return 0;
    return toSigned32(this.next64() >> BigInt(64 - bitCount));
  }

  nextLong() {
    return toSigned64(this.next64());
  }

  // --- kotlin.random.Random derivations on top of nextBits, from the stdlib's documented implementations ---

  nextBoolean() {
    return this.nextBits(1) !== 0;
  }

  nextInt() {
    return this.nextBits(32);
  }

  /** `Random.nextInt(until)`: power-of-two shortcut, otherwise rejection sampling. */
  nextIntBound(n) {
    if ((n & -n) === n) return this.nextBits(31 - Math.clz32(n));
    let v;
    let bits;
    do {
      bits = this.nextInt() >>> 1;
      v = bits % n;
    } while (toSigned32(bits - v + (n - 1)) < 0);
    return v;
  }

  /** `Random.nextDouble()` = `doubleFromParts(nextBits(26), nextBits(27))`. */
  nextDouble() {
    const hi26 = BigInt(this.nextBits(26));
    const low27 = BigInt(this.nextBits(27));
    return Number((hi26 << 27n) + low27) / Number(1n << 53n);
  }

  /** `Random.nextLong(until)` for a non-power-of-two bound: rejection sampling on the raw draw. */
  nextLongBound(n) {
    const bound = BigInt(n);
    let v;
    let bits;
    do {
      bits = BigInt.asUintN(64, this.next64()) >> 1n;
      v = bits % bound;
    } while (toSigned64(bits - v + (bound - 1n)) < 0n);
    return v;
  }
}

/** `MutableList.shuffle(random)`: Fisher-Yates walking down, `j = random.nextInt(i + 1)`. */
function shuffled(list, rng) {
  const out = [...list];
  for (let i = out.length - 1; i >= 1; i--) {
    const j = rng.nextIntBound(i + 1);
    const copy = out[i];
    out[i] = out[j];
    out[j] = copy;
  }
  return out;
}

/** `conditionSchedule(size, proportion, random)`. */
function conditionSchedule(size, proportion, rng, atLeast = 0) {
  const flagged = Math.min(size, Math.max(Math.min(atLeast, size), Math.round(size * proportion)));
  const slots = Array.from({ length: size }, (_, i) => i < flagged);
  return shuffled(slots, rng);
}

const lerp = (a, b, t) => a + (b - a) * Math.min(Math.max(t, 0), 1);

// ---------------------------------------------------------------------------
// Anchors. Nothing below runs unless every one of these holds.
// ---------------------------------------------------------------------------

const anchors = [];
const check = (name, actual, expected) => {
  const ok = JSON.stringify(actual) === JSON.stringify(expected);
  anchors.push({ name, ok, actual, expected });
};

// splitmix64's own published vector for seed 0.
check('canonical splitmix64 anchor (seed 0, first draw)', new SplitMix64(0).nextLong().toString(), (-0x1ddf57c684e23251n).toString());

// Everything DeterministicRngGoldenTest pins.
const first4 = (seed) => {
  const r = new SplitMix64(seed);
  return [r.nextLong(), r.nextLong(), r.nextLong(), r.nextLong()].map(String);
};
check('stream seed 0', first4(0), ['-2152535657050944081', '7960286522194355700', '487617019471545679', '-537132696929009172']);
check('stream seed 42', first4(42), ['-4767286540954276203', '2949826092126892291', '5139283748462763858', '6349198060258255764']);
check('stream seed -1', first4(-1), ['-1956407806741107680', '-1612297016619662647', '4048727598324417001', '7862637804313477842']);

{
  const r = new SplitMix64(0);
  check('nextBits(32) x3 @ seed 0', [r.nextBits(32), r.nextBits(32), r.nextBits(32)], [-501176263, 1853398634, 113532184]);
}
{
  const r = new SplitMix64(2026);
  check('nextInt(10) x5 @ seed 2026', [0, 0, 0, 0, 0].map(() => r.nextIntBound(10)), [6, 4, 8, 3, 7]);
  check('nextDouble x3 @ seed 2026', [0, 0, 0].map(() => r.nextDouble()), [0.726404219165592, 0.8045466443128232, 0.23006396495012704]);
  check('nextLong(1000) x3 @ seed 2026', [0, 0, 0].map(() => r.nextLongBound(1000).toString()), ['0', '676', '658']);
}

const failed = anchors.filter((a) => !a.ok);
for (const a of anchors) console.log(`${a.ok ? 'OK  ' : 'FAIL'} ${a.name}`);
if (failed.length > 0) {
  for (const a of failed) console.error(`\n${a.name}\n  expected ${JSON.stringify(a.expected)}\n  actual   ${JSON.stringify(a.actual)}`);
  console.error('\nThe reference does not reproduce the anchored layer; its output is NOT trustworthy.');
  process.exit(1);
}

// ---------------------------------------------------------------------------
// E2's form. Only reached with every anchor green.
// ---------------------------------------------------------------------------

/** `E2DifficultyMapping`, from the exercise's declared difficulty curve. */
const e2Params = (d) => ({
  incongruentProportion: lerp(0.2, 0.6, d.interference ?? 0),
  flankerCount: Math.trunc(lerp(2, 6, d.perceptualLoad ?? 0)),
  responseWindowMs: Math.trunc(lerp(1500, 900, d.pressure ?? 0)),
  isiMs: Math.trunc(lerp(700, 500, d.pressure ?? 0)),
});

function e2Form(seed, trials, difficulty = {}) {
  const params = e2Params(difficulty);
  const rng = new SplitMix64(seed);
  const incongruent = conditionSchedule(trials, params.incongruentProportion, rng);
  return Array.from({ length: trials }, (_, index) => {
    const direction = rng.nextBoolean() ? 'LEFT' : 'RIGHT';
    return {
      index,
      direction,
      congruent: !incongruent[index],
      flankerCount: params.flankerCount,
      responseWindowMs: params.responseWindowMs,
      isiMs: params.isiMs,
    };
  });
}

const render = (form) => ({
  directions: form.map((t) => t.direction[0]).join(''),
  congruency: form.map((t) => (t.congruent ? 'C' : 'I')).join(''),
  firstThree: form.slice(0, 3).map((t) =>
    [t.index, t.direction, 'true', 'false', t.direction, t.congruent ? 'C' : 'I', t.flankerCount, t.responseWindowMs, t.isiMs].join('|'),
  ),
  incongruentCount: form.filter((t) => !t.congruent).length,
});

console.log('\n--- E2 seed 4242, n=40, EASIEST (generatorVersion 3: congruency drawn by quota) ---');
console.log(JSON.stringify(render(e2Form(4242, 40)), null, 2));

console.log('\n--- E2 seed 4242, n=40, INTERFERENCE=1.0 (must draw more incongruent) ---');
console.log(JSON.stringify(render(e2Form(4242, 40, { interference: 1 })), null, 2));

console.log('\n--- E2 seed 4242, n=40, PRESSURE=1.0 (timing only; directions must be unchanged) ---');
console.log(JSON.stringify(render(e2Form(4242, 40, { pressure: 1 })), null, 2));
