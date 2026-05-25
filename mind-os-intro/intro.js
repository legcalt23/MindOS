// ═══════════════════════════════════════════════════════════════════════════════
// Mind OS — "The Awakening Core" Intro Animation
// Premium motion design · 5-second sequence
// ═══════════════════════════════════════════════════════════════════════════════

// ─── Configuration ───────────────────────────────────────────────────────────

const CONFIG = {
    DURATION: 5000,
    PARTICLE_COUNT: 80,
    MAX_CONNECTIONS: 45,
    CONNECTION_DISTANCE: 180,
    BRAND_NAME: "Mind OS",

    PHASE: {
        BLACK_BREATH:  { start: 0,    end: 800  },
        CONSTELLATION: { start: 800,  end: 1700 },
        CONVERGENCE:   { start: 1700, end: 3000 },
        NAME_EMERGES:  { start: 3000, end: 4200 },
        TRANSITION:    { start: 4200, end: 5000 },
    },

    COLOR: {
        DEEP_VOID:      '#05060A',
        COSMIC_INK:     '#0A0B14',
        GRAPHITE:       '#13141F',
        QUANTUM_VIOLET: '#7B6BFF',
        AURORA_LILAC:   '#A78BFA',
        SYNAPSE_BLUE:   '#5B8DEF',
        STELLAR_WHITE:  '#F5F0FF',
        HOLO_SILVER:    '#E6E4F0',
    },

    CORE: {
        RADIUS: 52,
        ROTATION_SPEED: 0.0004,
        BREATH_AMPLITUDE: 0.025,
        BREATH_SPEED: 0.002,
    },

    TYPE: {
        FONT: "500 44px -apple-system, BlinkMacSystemFont, 'Inter Display', 'Inter', 'Segoe UI', sans-serif",
        TRACKING: 10,
        OS_FONT: "400 44px -apple-system, BlinkMacSystemFont, 'Inter Display', 'Inter', 'Segoe UI', sans-serif",
    },
};

// ─── Easing ──────────────────────────────────────────────────────────────────

const ease = {
    smooth: t => t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2,
    out:    t => 1 - Math.pow(1 - t, 3),
    inOut:  t => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2,
    expo:   t => t === 1 ? 1 : 1 - Math.pow(2, -10 * t),
    outQuart: t => 1 - Math.pow(1 - t, 4),
};

// ─── Utility ─────────────────────────────────────────────────────────────────

function hexToRgb(hex) {
    return {
        r: parseInt(hex.slice(1, 3), 16),
        g: parseInt(hex.slice(3, 5), 16),
        b: parseInt(hex.slice(5, 7), 16),
    };
}

function rgba(color, a) { return `rgba(${color.r},${color.g},${color.b},${a})`; }
function lerp(a, b, t) { return a + (b - a) * t; }
function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }

function phaseProgress(time, phase) {
    if (time < phase.start) return 0;
    if (time > phase.end) return 1;
    return (time - phase.start) / (phase.end - phase.start);
}

// Pre-compute palette as RGB
const PAL = {};
for (const [k, v] of Object.entries(CONFIG.COLOR)) PAL[k] = hexToRgb(v);

// ─── Canvas ──────────────────────────────────────────────────────────────────

const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d');
const timerEl = document.getElementById('timer');
const replayBtn = document.getElementById('replay-btn');

let W, H, cx, cy, dpr;

function resize() {
    dpr = window.devicePixelRatio || 1;
    W = window.innerWidth;
    H = window.innerHeight;
    canvas.width = W * dpr;
    canvas.height = H * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    cx = W / 2;
    cy = H / 2;
}

window.addEventListener('resize', resize);
resize();

// ─── Particle ────────────────────────────────────────────────────────────────

class Particle {
    constructor(index, total) {
        // Golden-angle distribution for even spread
        const goldenAngle = Math.PI * (3 - Math.sqrt(5));
        const angle = index * goldenAngle + (Math.random() - 0.5) * 0.3;
        const radiusMin = 80;
        const radiusMax = Math.min(W, H) * 0.42;
        const normalizedR = (index + 0.5) / total;
        const dist = radiusMin + Math.sqrt(normalizedR) * (radiusMax - radiusMin);

        this.originX = cx + Math.cos(angle) * dist;
        this.originY = cy + Math.sin(angle) * dist;
        this.x = this.originX;
        this.y = this.originY;
        this.size = 1 + Math.random() * 2.2;
        this.baseOpacity = 0.35 + Math.random() * 0.55;
        this.currentOpacity = 0;
        this.visible = false;

        // Staggered reveal within constellation phase
        this.revealDelay = index * 12 + Math.random() * 60;
        this.driftPhase = Math.random() * Math.PI * 2;

        // Convergence params — spiral inward
        this.convergenceAngle = angle + (Math.random() - 0.5) * 0.8;
        this.spiralTurns = 0.6 + Math.random() * 1.2;
        this.convergenceSpeed = 0.85 + Math.random() * 0.3;
    }

    update(time) {
        const convergenceP = phaseProgress(time, CONFIG.PHASE.CONVERGENCE);

        // Reveal
        const revealTime = CONFIG.PHASE.CONSTELLATION.start + this.revealDelay;
        if (time >= revealTime) {
            const fadeIn = clamp((time - revealTime) / 350, 0, 1);
            this.currentOpacity = this.baseOpacity * ease.out(fadeIn);
            this.visible = true;
        }

        if (convergenceP > 0) {
            const t = ease.smooth(clamp(convergenceP * this.convergenceSpeed, 0, 1));
            const spiralAngle = this.convergenceAngle + t * Math.PI * this.spiralTurns;
            const origDist = Math.hypot(this.originX - cx, this.originY - cy);
            const dist = (1 - t) * origDist;

            this.x = cx + Math.cos(spiralAngle) * dist;
            this.y = cy + Math.sin(spiralAngle) * dist;

            if (t > 0.65) {
                this.currentOpacity *= 1 - ((t - 0.65) / 0.35);
            }
        } else {
            const drift = Math.sin(time * 0.0012 + this.driftPhase) * 2.5;
            this.x = this.originX + drift;
            this.y = this.originY + Math.cos(time * 0.0009 + this.driftPhase) * 2;
        }
    }

    draw() {
        if (!this.visible || this.currentOpacity <= 0.01) return;

        // Halo
        const grad = ctx.createRadialGradient(this.x, this.y, 0, this.x, this.y, this.size * 5);
        grad.addColorStop(0, rgba(PAL.QUANTUM_VIOLET, this.currentOpacity * 0.35));
        grad.addColorStop(1, rgba(PAL.QUANTUM_VIOLET, 0));
        ctx.fillStyle = grad;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size * 5, 0, Math.PI * 2);
        ctx.fill();

        // Dot
        ctx.fillStyle = rgba(PAL.STELLAR_WHITE, this.currentOpacity);
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fill();
    }
}

// ─── Connections (proximity-based) ───────────────────────────────────────────

class Connection {
    constructor(p1, p2, index) {
        this.p1 = p1;
        this.p2 = p2;
        this.revealDelay = CONFIG.PHASE.CONSTELLATION.start + 250 + index * 18;
        this.opacity = 0;
        // Subtle curve offset perpendicular to the line
        this.curveStrength = (Math.random() - 0.5) * 40;
    }

    update(time) {
        if (time < this.revealDelay) { this.opacity = 0; return; }

        const fadeIn = clamp((time - this.revealDelay) / 500, 0, 1);
        const convergenceP = phaseProgress(time, CONFIG.PHASE.CONVERGENCE);
        const fadeOut = convergenceP > 0.4 ? 1 - ease.smooth((convergenceP - 0.4) / 0.6) : 1;

        this.opacity = ease.out(fadeIn) * 0.2 * Math.max(fadeOut, 0);
    }

    draw() {
        if (this.opacity <= 0.005) return;

        // Control point: midpoint + perpendicular offset for organic curve
        const mx = (this.p1.x + this.p2.x) / 2;
        const my = (this.p1.y + this.p2.y) / 2;
        const dx = this.p2.x - this.p1.x;
        const dy = this.p2.y - this.p1.y;
        const len = Math.hypot(dx, dy) || 1;
        const nx = -dy / len;
        const ny = dx / len;

        ctx.strokeStyle = rgba(PAL.QUANTUM_VIOLET, this.opacity);
        ctx.lineWidth = 0.5;
        ctx.beginPath();
        ctx.moveTo(this.p1.x, this.p1.y);
        ctx.quadraticCurveTo(
            mx + nx * this.curveStrength,
            my + ny * this.curveStrength,
            this.p2.x, this.p2.y
        );
        ctx.stroke();
    }
}

function buildConnections(particles) {
    const conns = [];
    const maxDist = CONFIG.CONNECTION_DISTANCE;
    const used = new Set();

    for (let i = 0; i < particles.length && conns.length < CONFIG.MAX_CONNECTIONS; i++) {
        for (let j = i + 1; j < particles.length && conns.length < CONFIG.MAX_CONNECTIONS; j++) {
            const dist = Math.hypot(
                particles[i].originX - particles[j].originX,
                particles[i].originY - particles[j].originY
            );
            if (dist < maxDist && !used.has(`${i}-${j}`)) {
                conns.push(new Connection(particles[i], particles[j], conns.length));
                used.add(`${i}-${j}`);
            }
        }
    }
    return conns;
}

// ─── Crystalline Core ────────────────────────────────────────────────────────

class CrystalCore {
    constructor() {
        this.vertices = this._icosahedron();
        this.edges = this._edges();
        this.rotX = 0;
        this.rotY = 0;
        this.rotZ = 0;
        this.scale = 0;
        this.opacity = 0;
        this.glowIntensity = 0;
    }

    _icosahedron() {
        const phi = (1 + Math.sqrt(5)) / 2;
        const raw = [
            [-1, phi, 0], [1, phi, 0], [-1, -phi, 0], [1, -phi, 0],
            [0, -1, phi], [0, 1, phi], [0, -1, -phi], [0, 1, -phi],
            [phi, 0, -1], [phi, 0, 1], [-phi, 0, -1], [-phi, 0, 1],
        ];
        const len = Math.sqrt(1 + phi * phi);
        return raw.map(v => [v[0] / len, v[1] / len, v[2] / len]);
    }

    _edges() {
        const edges = [];
        const v = this.vertices;
        const threshold = 2 / Math.sqrt(1 + Math.pow((1 + Math.sqrt(5)) / 2, 2)) * 1.1;
        for (let i = 0; i < v.length; i++) {
            for (let j = i + 1; j < v.length; j++) {
                const d = Math.hypot(v[i][0] - v[j][0], v[i][1] - v[j][1], v[i][2] - v[j][2]);
                if (d < threshold) edges.push([i, j]);
            }
        }
        return edges;
    }

    project(v, radius) {
        const cx_ = Math.cos(this.rotX), sx = Math.sin(this.rotX);
        const cy_ = Math.cos(this.rotY), sy = Math.sin(this.rotY);
        const cz_ = Math.cos(this.rotZ), sz = Math.sin(this.rotZ);
        let [x, y, z] = v;
        let y1 = y * cx_ - z * sx, z1 = y * sx + z * cx_;
        let x2 = x * cy_ + z1 * sy, z2 = -x * sy + z1 * cy_;
        let x3 = x2 * cz_ - y1 * sz, y3 = x2 * sz + y1 * cz_;
        const p = 4 / (4 + z2);
        return { x: cx + x3 * radius * this.scale * p, y: cy + y3 * radius * this.scale * p, z: z2, p };
    }

    update(time) {
        const convP = phaseProgress(time, CONFIG.PHASE.CONVERGENCE);
        const transP = phaseProgress(time, CONFIG.PHASE.TRANSITION);

        // Appear
        if (convP > 0.35) {
            const t = (convP - 0.35) / 0.65;
            this.opacity = ease.outQuart(clamp(t, 0, 1));
            this.scale = ease.outQuart(clamp(t, 0, 1));
        }

        // Glow
        this.glowIntensity = convP > 0.6 ? ease.out((convP - 0.6) / 0.4) : 0;

        // Breathing
        const breath = Math.sin(time * CONFIG.CORE.BREATH_SPEED) * CONFIG.CORE.BREATH_AMPLITUDE;
        this.scale *= (1 + breath);

        // Rotation
        this.rotX += CONFIG.CORE.ROTATION_SPEED * 1.3;
        this.rotY += CONFIG.CORE.ROTATION_SPEED;
        this.rotZ += CONFIG.CORE.ROTATION_SPEED * 0.5;

        // Transition: expand + dissolve
        if (transP > 0) {
            const t = ease.smooth(transP);
            this.scale *= 1 + t * 2.5;
            this.opacity *= 1 - t;
        }
    }

    draw() {
        if (this.opacity <= 0.01) return;

        const R = CONFIG.CORE.RADIUS;

        // Multi-layer inner glow
        if (this.glowIntensity > 0) {
            // Outer soft glow
            const g1 = ctx.createRadialGradient(cx, cy, 0, cx, cy, R * this.scale * 3);
            g1.addColorStop(0, rgba(PAL.QUANTUM_VIOLET, this.opacity * this.glowIntensity * 0.25));
            g1.addColorStop(0.4, rgba(PAL.QUANTUM_VIOLET, this.opacity * this.glowIntensity * 0.08));
            g1.addColorStop(1, rgba(PAL.QUANTUM_VIOLET, 0));
            ctx.fillStyle = g1;
            ctx.beginPath();
            ctx.arc(cx, cy, R * this.scale * 3, 0, Math.PI * 2);
            ctx.fill();

            // Inner bright glow
            const g2 = ctx.createRadialGradient(cx, cy, 0, cx, cy, R * this.scale * 1.2);
            g2.addColorStop(0, rgba(PAL.STELLAR_WHITE, this.opacity * this.glowIntensity * 0.15));
            g2.addColorStop(0.5, rgba(PAL.AURORA_LILAC, this.opacity * this.glowIntensity * 0.1));
            g2.addColorStop(1, rgba(PAL.QUANTUM_VIOLET, 0));
            ctx.fillStyle = g2;
            ctx.beginPath();
            ctx.arc(cx, cy, R * this.scale * 1.2, 0, Math.PI * 2);
            ctx.fill();
        }

        // Edges with depth
        for (const [i, j] of this.edges) {
            const a = this.project(this.vertices[i], R);
            const b = this.project(this.vertices[j], R);
            const depth = 0.35 + 0.65 * ((a.z + b.z + 2) / 4);

            ctx.strokeStyle = rgba(PAL.AURORA_LILAC, this.opacity * depth * 0.85);
            ctx.lineWidth = 1.0 + depth * 0.4;
            ctx.beginPath();
            ctx.moveTo(a.x, a.y);
            ctx.lineTo(b.x, b.y);
            ctx.stroke();
        }

        // Vertices
        for (const v of this.vertices) {
            const pt = this.project(v, R);
            const depth = 0.3 + 0.7 * ((pt.z + 1) / 2);
            const sz = 1.8 * pt.p;

            // Vertex glow
            const vg = ctx.createRadialGradient(pt.x, pt.y, 0, pt.x, pt.y, sz * 4);
            vg.addColorStop(0, rgba(PAL.AURORA_LILAC, this.opacity * depth * 0.4));
            vg.addColorStop(1, rgba(PAL.AURORA_LILAC, 0));
            ctx.fillStyle = vg;
            ctx.beginPath();
            ctx.arc(pt.x, pt.y, sz * 4, 0, Math.PI * 2);
            ctx.fill();

            ctx.fillStyle = rgba(PAL.STELLAR_WHITE, this.opacity * depth);
            ctx.beginPath();
            ctx.arc(pt.x, pt.y, sz, 0, Math.PI * 2);
            ctx.fill();
        }
    }
}

// ─── Spark ───────────────────────────────────────────────────────────────────

function drawSpark(time) {
    const appearP = clamp((time - 200) / 400, 0, 1);
    if (appearP <= 0) return;

    let size = lerp(2, 6, ease.out(appearP));

    // Pulse 0.5s – 0.8s
    if (time >= 500 && time <= 800) {
        const pp = (time - 500) / 300;
        size = lerp(size, 14, Math.sin(pp * Math.PI));

        // Emission wave
        if (pp > 0.2) {
            const wp = (pp - 0.2) / 0.8;
            ctx.strokeStyle = rgba(PAL.QUANTUM_VIOLET, (1 - wp) * 0.25);
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.arc(cx, cy, wp * Math.min(W, H) * 0.18, 0, Math.PI * 2);
            ctx.stroke();
        }
    }

    // Fade out during convergence
    const convP = phaseProgress(time, CONFIG.PHASE.CONVERGENCE);
    let sparkAlpha = convP > 0.3 ? Math.max(0, 1 - (convP - 0.3) / 0.4) : 1;
    if (sparkAlpha <= 0) return;

    // Gaussian halo
    const hg = ctx.createRadialGradient(cx, cy, 0, cx, cy, size * 8);
    hg.addColorStop(0, rgba(PAL.QUANTUM_VIOLET, 0.45 * sparkAlpha));
    hg.addColorStop(0.4, rgba(PAL.QUANTUM_VIOLET, 0.12 * sparkAlpha));
    hg.addColorStop(1, rgba(PAL.QUANTUM_VIOLET, 0));
    ctx.fillStyle = hg;
    ctx.beginPath();
    ctx.arc(cx, cy, size * 8, 0, Math.PI * 2);
    ctx.fill();

    // Core
    const cg = ctx.createRadialGradient(cx, cy, 0, cx, cy, size);
    cg.addColorStop(0, rgba(PAL.STELLAR_WHITE, sparkAlpha));
    cg.addColorStop(0.5, rgba(PAL.AURORA_LILAC, sparkAlpha * 0.7));
    cg.addColorStop(1, rgba(PAL.QUANTUM_VIOLET, 0));
    ctx.fillStyle = cg;
    ctx.beginPath();
    ctx.arc(cx, cy, size, 0, Math.PI * 2);
    ctx.fill();
}

// ─── Wordmark ────────────────────────────────────────────────────────────────

function drawWordmark(time) {
    const nameP = phaseProgress(time, CONFIG.PHASE.NAME_EMERGES);
    const transP = phaseProgress(time, CONFIG.PHASE.TRANSITION);
    if (nameP <= 0) return;

    const revealT = ease.expo(clamp(nameP * 1.4, 0, 1));
    let opacity = revealT;
    let blur = (1 - revealT) * 10;
    let yOff = (1 - revealT) * 8;

    if (transP > 0) {
        const t = ease.smooth(transP);
        opacity *= 1 - t;
        yOff -= t * 10;
    }

    if (opacity <= 0.01) return;

    const textY = cy + CONFIG.CORE.RADIUS + 55 + yOff;

    ctx.save();
    if (blur > 0.3) ctx.filter = `blur(${blur}px)`;

    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';

    // Measure total width for dual-color rendering
    ctx.font = CONFIG.TYPE.FONT;
    ctx.letterSpacing = `${CONFIG.TYPE.TRACKING}px`;
    const fullText = "Mind OS";
    const fullWidth = ctx.measureText(fullText).width;

    // Glow layer
    ctx.shadowColor = rgba(PAL.QUANTUM_VIOLET, 0.6);
    ctx.shadowBlur = 24;

    // Draw "Mind " in white
    ctx.font = CONFIG.TYPE.FONT;
    ctx.letterSpacing = `${CONFIG.TYPE.TRACKING}px`;
    ctx.fillStyle = rgba(PAL.STELLAR_WHITE, opacity);
    ctx.textAlign = 'center';
    ctx.fillText(fullText, cx, textY);

    // Overdraw "OS" in lilac
    ctx.shadowBlur = 0;
    ctx.font = CONFIG.TYPE.OS_FONT;
    const mindOnly = ctx.measureText("Mind ").width;
    ctx.font = CONFIG.TYPE.FONT;
    const mindW = ctx.measureText("Mind ").width;
    ctx.font = CONFIG.TYPE.OS_FONT;
    ctx.letterSpacing = `${CONFIG.TYPE.TRACKING}px`;
    const osW = ctx.measureText("OS").width;

    // Position: start of "OS" = center - halfFull + mindW
    ctx.textAlign = 'left';
    ctx.fillStyle = rgba(PAL.AURORA_LILAC, opacity);
    const osX = cx - fullWidth / 2 + mindW;
    ctx.fillText("OS", osX, textY);

    // Underline
    if (revealT > 0.4 && opacity > 0.2) {
        const lineAlpha = Math.min((revealT - 0.4) / 0.6, 1) * opacity * 0.45;
        const lw = fullWidth * 0.6;
        const ly = textY + 28;

        const lg = ctx.createLinearGradient(cx - lw, ly, cx + lw, ly);
        lg.addColorStop(0, rgba(PAL.QUANTUM_VIOLET, 0));
        lg.addColorStop(0.2, rgba(PAL.QUANTUM_VIOLET, lineAlpha));
        lg.addColorStop(0.8, rgba(PAL.QUANTUM_VIOLET, lineAlpha));
        lg.addColorStop(1, rgba(PAL.QUANTUM_VIOLET, 0));

        ctx.strokeStyle = lg;
        ctx.lineWidth = 0.7;
        ctx.beginPath();
        ctx.moveTo(cx - lw, ly);
        ctx.lineTo(cx + lw, ly);
        ctx.stroke();
    }

    ctx.restore();
}

// ─── Background ──────────────────────────────────────────────────────────────

function drawBackground() {
    const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, Math.max(W, H) * 0.7);
    grad.addColorStop(0, CONFIG.COLOR.COSMIC_INK);
    grad.addColorStop(1, CONFIG.COLOR.DEEP_VOID);
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, W, H);
}

// ─── Animation Engine ────────────────────────────────────────────────────────

let particles = [];
let connections = [];
let core;
let startTime = null;
let animFrame = null;
let finished = false;

function init() {
    particles = [];
    connections = [];
    finished = false;

    for (let i = 0; i < CONFIG.PARTICLE_COUNT; i++) {
        particles.push(new Particle(i, CONFIG.PARTICLE_COUNT));
    }

    connections = buildConnections(particles);
    core = new CrystalCore();
    startTime = performance.now();
}

function renderFrame(time) {
    timerEl.textContent = `${(time / 1000).toFixed(3)}s`;

    ctx.clearRect(0, 0, W, H);
    drawBackground();
    drawSpark(time);

    for (const p of particles) p.update(time);
    for (const c of connections) { c.update(time); c.draw(); }
    for (const p of particles) p.draw();

    core.update(time);
    core.draw();

    drawWordmark(time);
}

function render(timestamp) {
    if (!startTime) startTime = timestamp;
    const time = Math.min(timestamp - startTime, CONFIG.DURATION);

    renderFrame(time);

    if (time < CONFIG.DURATION) {
        animFrame = requestAnimationFrame(render);
    } else {
        finished = true;
        timerEl.textContent = '5.000s — Complete';
    }
}

function start() {
    if (animFrame) cancelAnimationFrame(animFrame);
    init();
    animFrame = requestAnimationFrame(render);
}

// Debug: freeze at specific ms
window.freezeAt = function(ms) {
    if (animFrame) cancelAnimationFrame(animFrame);
    init();
    core.rotX = ms * CONFIG.CORE.ROTATION_SPEED * 1.3;
    core.rotY = ms * CONFIG.CORE.ROTATION_SPEED;
    core.rotZ = ms * CONFIG.CORE.ROTATION_SPEED * 0.5;
    renderFrame(ms);
    timerEl.textContent = `${(ms / 1000).toFixed(3)}s — Frozen`;
};

// ─── Controls ────────────────────────────────────────────────────────────────

replayBtn.addEventListener('click', start);

// ─── Launch ──────────────────────────────────────────────────────────────────

start();
