// V2d PHASE 5 — DETERMINISTIC ZIP WRITER (store-only).
//
// bsdtar's zip writer embeds an NTFS extra field carrying mtime/atime/**ctime**. `utimes` cannot set ctime,
// and staged copies get a fresh ctime on every build, so two builds of identical content produced different
// bytes (observed: 28cba698… → e4f1ab45… with no input change). Rather than hunt for tool flags that might
// suppress it, write the container ourselves: the only inputs are the sorted path and the exact bytes.
//
// * method 0 (STORE) — no compression, so no zlib-version or level variance
// * fixed DOS timestamp (SOURCE_DATE_EPOCH, default 1980-01-01, the ZIP epoch floor)
// * entries sorted by path byte-order — P5-12: ordering is specified, not incidental
// * no extra fields, no comments, no data descriptors, no zip64 (guarded)
// Readable by `unzip -l`, `tar -tf`, and any standard tool.
import { createHash } from 'node:crypto';

const CRC = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c; }
  return t;
})();
export function crc32(buf) {
  let c = ~0;
  for (let i = 0; i < buf.length; i++) c = CRC[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (~c) >>> 0;
}

// DOS date/time. The ZIP epoch floor is 1980-01-01; anything earlier is unrepresentable.
function dosDateTime(epochSeconds) {
  const d = new Date(Math.max(epochSeconds, 315532800) * 1000); // 315532800 = 1980-01-01T00:00:00Z
  const time = ((d.getUTCHours() & 31) << 11) | ((d.getUTCMinutes() & 63) << 5) | ((d.getUTCSeconds() / 2) & 31);
  const date = (((d.getUTCFullYear() - 1980) & 127) << 9) | (((d.getUTCMonth() + 1) & 15) << 5) | (d.getUTCDate() & 31);
  return { time, date };
}

// entries: [{ path, data }]. Returns the ZIP buffer.
export function buildZip(entries, { epochSeconds = Number(process.env.SOURCE_DATE_EPOCH ?? 315532800) } = {}) {
  const sorted = [...entries].sort((a, b) => (a.path < b.path ? -1 : a.path > b.path ? 1 : 0));
  const seen = new Set();
  for (const e of sorted) {
    if (e.path.includes('\\')) throw new Error(`zip path must be /-separated: ${e.path}`);
    if (e.path.startsWith('/') || /^[A-Za-z]:/.test(e.path) || e.path.split('/').includes('..')) throw new Error(`unsafe zip path: ${e.path}`);
    if (seen.has(e.path)) throw new Error(`duplicate zip entry: ${e.path}`);
    seen.add(e.path);
  }
  const { time, date } = dosDateTime(epochSeconds);
  const locals = [];
  const centrals = [];
  let offset = 0;
  for (const e of sorted) {
    const name = Buffer.from(e.path, 'utf8');
    const data = e.data;
    if (data.length > 0xffffffff) throw new Error(`entry too large for zip32: ${e.path}`);
    const crc = crc32(data);
    const lf = Buffer.alloc(30);
    lf.writeUInt32LE(0x04034b50, 0);   // local file header signature
    lf.writeUInt16LE(20, 4);           // version needed
    lf.writeUInt16LE(0, 6);            // flags — no data descriptor, no UTF-8 bit needed for ASCII paths
    lf.writeUInt16LE(0, 8);            // method 0 = STORE
    lf.writeUInt16LE(time, 10);
    lf.writeUInt16LE(date, 12);
    lf.writeUInt32LE(crc, 14);
    lf.writeUInt32LE(data.length, 18); // compressed size == size (stored)
    lf.writeUInt32LE(data.length, 22);
    lf.writeUInt16LE(name.length, 26);
    lf.writeUInt16LE(0, 28);           // no extra field — this is what bsdtar could not omit
    locals.push(lf, name, data);

    const cd = Buffer.alloc(46);
    cd.writeUInt32LE(0x02014b50, 0);   // central directory signature
    cd.writeUInt16LE(20, 4);           // version made by
    cd.writeUInt16LE(20, 6);           // version needed
    cd.writeUInt16LE(0, 8);
    cd.writeUInt16LE(0, 10);
    cd.writeUInt16LE(time, 12);
    cd.writeUInt16LE(date, 14);
    cd.writeUInt32LE(crc, 16);
    cd.writeUInt32LE(data.length, 20);
    cd.writeUInt32LE(data.length, 24);
    cd.writeUInt16LE(name.length, 28);
    cd.writeUInt16LE(0, 30);           // extra len
    cd.writeUInt16LE(0, 32);           // comment len
    cd.writeUInt16LE(0, 34);           // disk number
    cd.writeUInt16LE(0, 36);           // internal attrs
    cd.writeUInt32LE(0, 38);           // external attrs — 0, so no permission bits leak into the hash
    cd.writeUInt32LE(offset, 42);
    centrals.push(cd, name);
    offset += 30 + name.length + data.length;
  }
  const cdBuf = Buffer.concat(centrals);
  if (sorted.length > 0xffff || offset > 0xffffffff || cdBuf.length > 0xffffffff) throw new Error('zip64 required — refusing (the format guard is deliberate)');
  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(0x06054b50, 0);
  eocd.writeUInt16LE(0, 4);
  eocd.writeUInt16LE(0, 6);
  eocd.writeUInt16LE(sorted.length, 8);
  eocd.writeUInt16LE(sorted.length, 10);
  eocd.writeUInt32LE(cdBuf.length, 12);
  eocd.writeUInt32LE(offset, 16);
  eocd.writeUInt16LE(0, 20);           // no archive comment
  const zip = Buffer.concat([...locals, cdBuf, eocd]);
  return { buffer: zip, sha256: createHash('sha256').update(zip).digest('hex'), entryCount: sorted.length, order: sorted.map((e) => e.path) };
}
