import {
  PROTOCOL_VERSION,
  createEvent,
  createResponse,
  type MoccaEvent,
  type MoccaRequest,
  type MoccaResponse,
} from "../protocol/message.js";
import type { OpenCodeRuntimeBridge, OpenCodeRuntimeEvent } from "../opencode/runtimeServer.js";
import { ConfirmationStore } from "../capabilities/common/confirmation.js";
import { EventSequencer } from "../capabilities/common/eventSequencer.js";
import { toErrorCode, toErrorDetails } from "../capabilities/common/errors.js";
import { AiConfigManager } from "../capabilities/ai/aiConfigManager.js";
import { FileSystemCapability } from "../capabilities/fs/fsCapability.js";
import { GitCapability } from "../capabilities/git/gitCapability.js";
import { SystemCapability } from "../capabilities/system/systemCapability.js";
import { TerminalCapability } from "../capabilities/terminal/terminalCapability.js";

export type BridgeRouterOptions = {
  projectDir: string;
  configSnapshotProvider: () => Promise<unknown>;
  openCodeRuntime?: OpenCodeRuntimeBridge;
  eventSink?: (event: MoccaEvent) => void;
};

export type BridgeRouter = {
  handleRequest(request: MoccaRequest): Promise<MoccaResponse>;
  close(): Promise<void>;
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
  const eventSequencer = new EventSequencer();
  const confirmationStore = new ConfirmationStore();
  let snapshotCache: { value: unknown; timestamp: number } | undefined;
  let snapshotInFlight: Promise<unknown> | undefined;
  const getConfigSnapshot = async (force = false): Promise<unknown> => {
    const now = Date.now();
    if (!force && snapshotCache != null && now - snapshotCache.timestamp < CONFIG_SNAPSHOT_CACHE_TTL_MS) {
      return snapshotCache.value;
    }
    if (!force && snapshotInFlight != null) {
      return snapshotInFlight;
    }
    snapshotInFlight = options.configSnapshotProvider()
      .then((value) => {
        snapshotCache = { value, timestamp: Date.now() };
        return value;
      })
      .finally(() => {
        snapshotInFlight = undefined;
      });
    return snapshotInFlight;
  };
  const fsCapability = new FileSystemCapability({
    projectDir: options.projectDir,
    confirmationStore,
    eventSequencer,
    eventSink: options.eventSink,
  });
  void fsCapability.loadIgnoreFile();
  const gitCapability = new GitCapability({
    projectDir: options.projectDir,
    confirmationStore,
    eventSequencer,
    eventSink: options.eventSink,
  });
  const terminalCapability = new TerminalCapability({
    projectDir: options.projectDir,
    confirmationStore,
    eventSequencer,
    eventSink: options.eventSink,
  });
  const systemCapability = new SystemCapability({
    projectDir: options.projectDir,
    confirmationStore,
    eventSequencer,
    eventSink: options.eventSink,
  });
  const aiConfigManager = new AiConfigManager({
    projectDir: options.projectDir,
    configSnapshotProvider: getConfigSnapshot,
    openCodeRuntime: options.openCodeRuntime,
    eventSequencer,
    eventSink: options.eventSink,
  });
  const unsubscribeRuntime = options.openCodeRuntime?.subscribe((event) => {
    options.eventSink?.(toBridgeRuntimeEvent(event, eventSequencer.next("ai")));
  });

  return {
    async handleRequest(request: MoccaRequest): Promise<MoccaResponse> {
      try {
        const capabilityResponse = await fsCapability.handle(request)
          ?? await gitCapability.handle(request)
          ?? await terminalCapability.handle(request)
          ?? await systemCapability.handle(request)
          ?? await aiConfigManager.handle(request);
        if (capabilityResponse != null) {
          return capabilityResponse;
        }

        if (request.ns === "system" && request.action === "capabilities") {
          return createResponse(request, {
            ok: true,
            payload: {
              protocolVersion: PROTOCOL_VERSION,
              namespaces: SUPPORTED_NAMESPACES,
              fs: fsCapability.capabilities,
              git: gitCapability.capabilities,
              terminal: terminalCapability.capabilities,
              process: systemCapability.capabilities.process,
              ports: systemCapability.capabilities.ports,
              monitor: systemCapability.capabilities.monitor,
              safety: {
                confirmationRequired: true,
              },
              ai: aiConfigManager.capabilities,
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
          payload: await getConfigSnapshot(request.payload === "refresh"),
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
        const snapshot = await getConfigSnapshot();
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
        const snapshot = await getConfigSnapshot();
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
        const snapshot = await getConfigSnapshot();
        return createResponse(request, {
          ok: true,
          payload: readArrayProjection(snapshot, "commands"),
        });
      }

      if (request.ns === "mcp" && request.action === "servers.list") {
        const snapshot = await getConfigSnapshot();
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
            code: toErrorCode(error),
            message: error instanceof Error ? error.message : String(error),
            details: toErrorDetails(error),
          },
        });
      }
    },
    async close(): Promise<void> {
      unsubscribeRuntime?.();
      await aiConfigManager.close();
      await terminalCapability.close();
      await fsCapability.close();
    },
  };
}

const CONFIG_SNAPSHOT_CACHE_TTL_MS = 30_000;

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
