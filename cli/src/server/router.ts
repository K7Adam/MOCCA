import {
  PROTOCOL_VERSION,
  createEvent,
  createResponse,
  type MoccaEvent,
  type MoccaRequest,
  type MoccaResponse,
} from "../protocol/message.js";
import type { OpenCodeRuntimeBridge, OpenCodeRuntimeEvent } from "../opencode/runtimeServer.js";

export type BridgeRouterOptions = {
  configSnapshotProvider: () => Promise<unknown>;
  openCodeRuntime?: OpenCodeRuntimeBridge;
  eventSink?: (event: MoccaEvent) => void;
};

export type BridgeRouter = {
  handleRequest(request: MoccaRequest): Promise<MoccaResponse>;
  close(): void;
};

const SUPPORTED_NAMESPACES = [
  "system",
  "ai",
  "fs",
  "git",
  "terminal",
  "process",
  "ports",
  "monitor",
  "mcp",
  "providers",
  "commands",
  "project",
  "diagnostics",
];

export function createBridgeRouter(options: BridgeRouterOptions): BridgeRouter {
  let nextEventSeq = 0;
  const unsubscribeRuntime = options.openCodeRuntime?.subscribe((event) => {
    options.eventSink?.(toBridgeRuntimeEvent(event, nextEventSeq++));
  });

  return {
    async handleRequest(request: MoccaRequest): Promise<MoccaResponse> {
      try {
        if (request.ns === "system" && request.action === "capabilities") {
          return createResponse(request, {
            ok: true,
            payload: {
              protocolVersion: PROTOCOL_VERSION,
              namespaces: SUPPORTED_NAMESPACES,
              ai: {
                opencodeConfigSnapshot: true,
                opencodeRuntime: options.openCodeRuntime != null,
                sessions: options.openCodeRuntime != null,
                messages: options.openCodeRuntime != null,
              },
            },
          });
        }

        if (request.ns === "ai" && request.action === "runtime.status") {
          return createResponse(request, {
            ok: true,
            payload: options.openCodeRuntime?.getStatus() ?? { status: "unavailable" },
          });
        }

        if (request.ns === "ai" && request.action === "runtime.ensure") {
          const runtime = requireRuntime(options);
          return createResponse(request, {
            ok: true,
            payload: {
              status: "ready",
              server: await runtime.ensureServer(),
            },
          });
        }

      if (request.ns === "ai" && request.action === "config.snapshot") {
        return createResponse(request, {
          ok: true,
          payload: await options.configSnapshotProvider(),
        });
      }

      if (request.ns === "ai" && request.action === "sessions.list") {
        return createResponse(request, {
          ok: true,
          payload: await requireRuntime(options).listSessions(),
        });
      }

      if (request.ns === "ai" && request.action === "sessions.create") {
        return createResponse(request, {
          ok: true,
          payload: await requireRuntime(options).createSession(request.payload),
        });
      }

      if (request.ns === "ai" && request.action === "messages.list") {
        return createResponse(request, {
          ok: true,
          payload: await requireRuntime(options).getMessages(request.payload),
        });
      }

      if (request.ns === "ai" && request.action === "messages.send") {
        return createResponse(request, {
          ok: true,
          payload: await requireRuntime(options).sendMessage(request.payload),
        });
      }

      if (request.ns === "ai" && request.action === "session.abort") {
        return createResponse(request, {
          ok: true,
          payload: await requireRuntime(options).abortSession(request.payload),
        });
      }

      if (request.ns === "providers" && request.action === "list") {
        return createResponse(request, {
          ok: true,
          payload: await requireRuntime(options).listProviders(),
        });
      }

      if (request.ns === "providers" && request.action === "config") {
        return createResponse(request, {
          ok: true,
          payload: await requireRuntime(options).listProviderConfig(),
        });
      }

      if (request.ns === "mcp" && request.action === "status") {
        return createResponse(request, {
          ok: true,
          payload: await requireRuntime(options).getMcpStatus(),
        });
      }

      if (request.ns === "providers" && request.action === "credentials.list") {
        const snapshot = await options.configSnapshotProvider();
        return createResponse(request, {
          ok: true,
          payload: readArrayProjection(snapshot, "credentials"),
        });
      }

      if (request.ns === "ai" && request.action === "agents.list") {
        if (options.openCodeRuntime != null) {
          return createResponse(request, {
            ok: true,
            payload: await options.openCodeRuntime.listAgents(),
          });
        }
        const snapshot = await options.configSnapshotProvider();
        return createResponse(request, {
          ok: true,
          payload: readArrayProjection(snapshot, "agents"),
        });
      }

      if (request.ns === "commands" && request.action === "list") {
        if (options.openCodeRuntime != null) {
          return createResponse(request, {
            ok: true,
            payload: await options.openCodeRuntime.listCommands(),
          });
        }
        const snapshot = await options.configSnapshotProvider();
        return createResponse(request, {
          ok: true,
          payload: readArrayProjection(snapshot, "commands"),
        });
      }

      if (request.ns === "mcp" && request.action === "servers.list") {
        const snapshot = await options.configSnapshotProvider();
        return createResponse(request, {
          ok: true,
          payload: readArrayProjection(snapshot, "mcpServers"),
        });
      }

        return createResponse(request, {
          ok: false,
          error: {
            code: "not_found",
            message: `No handler registered for ${request.ns}.${request.action}`,
          },
        });
      } catch (error) {
        return createResponse(request, {
          ok: false,
          error: {
            code: readErrorCode(error),
            message: error instanceof Error ? error.message : String(error),
          },
        });
      }
    },
    close(): void {
      unsubscribeRuntime?.();
    },
  };
}

function requireRuntime(options: BridgeRouterOptions): OpenCodeRuntimeBridge {
  if (options.openCodeRuntime == null) {
    throw Object.assign(new Error("OpenCode runtime is not available"), {
      code: "runtime_unavailable",
    });
  }
  return options.openCodeRuntime;
}

function toBridgeRuntimeEvent(event: OpenCodeRuntimeEvent, seq: number): MoccaEvent {
  return createEvent({
    ns: "ai",
    event: "runtime.event",
    seq,
    payload: event,
  });
}

function readArrayProjection(snapshot: unknown, key: string): unknown[] {
  if (typeof snapshot !== "object" || snapshot == null || Array.isArray(snapshot)) {
    return [];
  }

  const value = (snapshot as Record<string, unknown>)[key];
  return Array.isArray(value) ? value : [];
}

function readErrorCode(error: unknown): string {
  if (typeof error === "object" && error != null && "code" in error && typeof error.code === "string") {
    return error.code;
  }
  return "bridge_request_failed";
}
