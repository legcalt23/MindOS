// Worker: one concurrent lock acquirer against an INJECTED temp lockPath. Never touches the real lock.
import { workerData, parentPort } from 'node:worker_threads';
import { acquire } from '../ledger-lock.mjs';
const r = acquire({ lockPath: workerData.lockPath, ledgerPath: workerData.ledgerPath, taskId: workerData.taskId, owner: 'racer', leaseMs: workerData.leaseMs });
parentPort.postMessage({ won: r.ok, token: r.token || null, reason: r.reason || null });
