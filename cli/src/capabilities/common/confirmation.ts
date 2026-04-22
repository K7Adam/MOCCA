import { createHash, randomUUID } from "node:crypto";
import { CapabilityError } from "./errors.js";

export type ConfirmationInput = {
  operationId?: string;
};

export type ConfirmationRequest = {
  action: string;
  target: string;
  risk: string;
  payload?: unknown;
  confirmation?: ConfirmationInput;
};

type PendingConfirmation = {
  operationId: string;
  action: string;
  target: string;
  hash: string;
  expiresAt: number;
  risk: string;
};

export class ConfirmationStore {
  private readonly pending = new Map<string, PendingConfirmation>();

  constructor(private readonly ttlMillis = 120_000) {}

  require(request: ConfirmationRequest): void {
    const now = Date.now();
    this.prune(now);
    const hash = hashTarget(request.action, request.target, request.payload);
    const operationId = request.confirmation?.operationId;
    if (operationId != null) {
      const pending = this.pending.get(operationId);
      if (pending != null && pending.hash === hash && pending.expiresAt >= now) {
        this.pending.delete(operationId);
        return;
      }
    }

    const pending: PendingConfirmation = {
      operationId: randomUUID(),
      action: request.action,
      target: request.target,
      hash,
      expiresAt: now + this.ttlMillis,
      risk: request.risk,
    };
    this.pending.set(pending.operationId, pending);
    throw new CapabilityError("confirmation_required", request.risk, {
      operationId: pending.operationId,
      action: pending.action,
      target: pending.target,
      risk: pending.risk,
      expiresAt: pending.expiresAt,
    });
  }

  private prune(now: number): void {
    for (const [operationId, pending] of this.pending.entries()) {
      if (pending.expiresAt < now) {
        this.pending.delete(operationId);
      }
    }
  }
}

function hashTarget(action: string, target: string, payload: unknown): string {
  return createHash("sha256")
    .update(action)
    .update("\0")
    .update(target)
    .update("\0")
    .update(stableStringify(stripConfirmation(payload)))
    .digest("hex");
}

function stripConfirmation(value: unknown): unknown {
  if (value == null || typeof value !== "object") return value;
  if (Array.isArray(value)) return value.map(stripConfirmation);
  const result: Record<string, unknown> = {};
  for (const [key, entry] of Object.entries(value as Record<string, unknown>)) {
    if (key !== "confirmation") result[key] = stripConfirmation(entry);
  }
  return result;
}

function stableStringify(value: unknown): string {
  if (value == null || typeof value !== "object") return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(",")}]`;
  const record = value as Record<string, unknown>;
  return `{${Object.keys(record).sort().map((key) => `${JSON.stringify(key)}:${stableStringify(record[key])}`).join(",")}}`;
}
