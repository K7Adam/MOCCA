export const PROTOCOL_VERSION = 1 as const;

export type BridgeNamespace =
  | "system"
  | "ai"
  | "fs"
  | "git"
  | "terminal"
  | "process"
  | "ports"
  | "monitor"
  | "mcp"
  | "providers"
  | "commands"
  | "project"
  | "diagnostics"
  | string;

export type MoccaRequest = {
  v: typeof PROTOCOL_VERSION;
  id: string;
  ns: BridgeNamespace;
  action: string;
  payload?: unknown;
};

export type MoccaError = {
  code: string;
  message: string;
  details?: unknown;
};

export type MoccaResponse = {
  v: typeof PROTOCOL_VERSION;
  id: string;
  ns: BridgeNamespace;
  action: string;
  ok: boolean;
  payload?: unknown;
  error?: MoccaError;
};

export type MoccaEvent = {
  v: typeof PROTOCOL_VERSION;
  ns: BridgeNamespace;
  event: string;
  seq?: number;
  payload?: unknown;
};

export type ProtocolFrame = MoccaRequest | MoccaResponse | MoccaEvent;

export function createRequest(input: Omit<MoccaRequest, "v">): MoccaRequest {
  assertNonBlank(input.id, "id");
  assertNonBlank(input.ns, "ns");
  assertNonBlank(input.action, "action");
  return withoutUndefined({
    v: PROTOCOL_VERSION,
    id: input.id,
    ns: input.ns,
    action: input.action,
    payload: input.payload,
  });
}

export function createResponse(
  request: MoccaRequest,
  result: { ok: true; payload?: unknown } | { ok: false; error: MoccaError; payload?: unknown },
): MoccaResponse {
  const base = {
    v: PROTOCOL_VERSION,
    id: request.id,
    ns: request.ns,
    action: request.action,
    ok: result.ok,
  };

  if (result.ok) {
    return withoutUndefined({ ...base, payload: result.payload });
  }

  return withoutUndefined({ ...base, payload: result.payload, error: result.error });
}

export function createEvent(input: Omit<MoccaEvent, "v">): MoccaEvent {
  assertNonBlank(input.ns, "ns");
  assertNonBlank(input.event, "event");
  return withoutUndefined({
    v: PROTOCOL_VERSION,
    ns: input.ns,
    event: input.event,
    seq: input.seq,
    payload: input.payload,
  });
}

export function parseProtocolFrame(frame: unknown): ProtocolFrame {
  if (!isRecord(frame)) {
    throw new Error("Malformed protocol frame: expected object");
  }

  if (frame.v !== PROTOCOL_VERSION) {
    throw new Error(`Unsupported protocol version: ${String(frame.v)}`);
  }

  if (typeof frame.ns !== "string" || frame.ns.length === 0) {
    throw new Error("Malformed protocol frame: ns is required");
  }

  if ("event" in frame) {
    if (typeof frame.event !== "string" || frame.event.length === 0) {
      throw new Error("Malformed event frame: event is required");
    }
    return frame as MoccaEvent;
  }

  if ("action" in frame) {
    if (typeof frame.id !== "string" || frame.id.length === 0) {
      throw new Error("Malformed request/response frame: id is required");
    }
    if (typeof frame.action !== "string" || frame.action.length === 0) {
      throw new Error("Malformed request/response frame: action is required");
    }
    if ("ok" in frame && typeof frame.ok !== "boolean") {
      throw new Error("Malformed response frame: ok must be boolean");
    }
    return frame as MoccaRequest | MoccaResponse;
  }

  throw new Error("Malformed protocol frame: expected request, response, or event frame");
}

function assertNonBlank(value: string, field: string): void {
  if (value.trim().length === 0) {
    throw new Error(`Protocol ${field} must not be blank`);
  }
}

function withoutUndefined<T extends Record<string, unknown>>(value: T): T {
  return Object.fromEntries(Object.entries(value).filter(([, entry]) => entry !== undefined)) as T;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
