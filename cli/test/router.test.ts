import { describe, expect, it } from "vitest";
import { createRequest } from "../src/protocol/message";
import { createBridgeRouter } from "../src/server/router";
import type { OpenCodeRuntimeBridge } from "../src/opencode/runtimeServer";

describe("MOCCA CLI request router", () => {
  it("reports bridge capabilities without exposing implementation details", async () => {
    const router = createBridgeRouter({
      configSnapshotProvider: async () => ({ installed: { available: true, command: "opencode", version: "1.14.19" } }),
    });

    const response = await router.handleRequest(createRequest({
      id: "req-capabilities",
      ns: "system",
      action: "capabilities",
    }));

    expect(response).toMatchObject({
      v: 1,
      id: "req-capabilities",
      ns: "system",
      action: "capabilities",
      ok: true,
      payload: {
        protocolVersion: 1,
        namespaces: expect.arrayContaining(["system", "ai"]),
        ai: {
          opencodeConfigSnapshot: true,
        },
      },
    });
  });

  it("returns the redacted OpenCode config snapshot through ai.config.snapshot", async () => {
    const snapshot = {
      installed: { available: true, command: "opencode", version: "1.14.19" },
      effective: { model: "project/model", plugins: [], tools: {}, raw: {} },
      credentials: [],
      agents: [],
      commands: [],
      mcpServers: [],
    };
    const router = createBridgeRouter({
      configSnapshotProvider: async () => snapshot,
    });

    const response = await router.handleRequest(createRequest({
      id: "req-config",
      ns: "ai",
      action: "config.snapshot",
    }));

    expect(response).toMatchObject({
      id: "req-config",
      ns: "ai",
      action: "config.snapshot",
      ok: true,
      payload: snapshot,
    });
  });

  it("returns targeted OpenCode config projections", async () => {
    const snapshot = {
      installed: { available: true, command: "opencode", version: "1.14.19" },
      effective: { model: "project/model", plugins: [], tools: {}, raw: {} },
      credentials: [{ name: "anthropic", type: "api" }],
      agents: [{ name: "build", primary: true }],
      commands: [{ name: "lint", description: "Run lint" }],
      mcpServers: [{ name: "filesystem", type: "local", enabled: true }],
    };
    const router = createBridgeRouter({
      configSnapshotProvider: async () => snapshot,
    });

    await expect(router.handleRequest(createRequest({
      id: "req-providers",
      ns: "providers",
      action: "credentials.list",
    }))).resolves.toMatchObject({
      ok: true,
      payload: snapshot.credentials,
    });

    await expect(router.handleRequest(createRequest({
      id: "req-agents",
      ns: "ai",
      action: "agents.list",
    }))).resolves.toMatchObject({
      ok: true,
      payload: snapshot.agents,
    });

    await expect(router.handleRequest(createRequest({
      id: "req-commands",
      ns: "commands",
      action: "list",
    }))).resolves.toMatchObject({
      ok: true,
      payload: snapshot.commands,
    });

    await expect(router.handleRequest(createRequest({
      id: "req-mcp",
      ns: "mcp",
      action: "servers.list",
    }))).resolves.toMatchObject({
      ok: true,
      payload: snapshot.mcpServers,
    });
  });

  it("exposes lazy OpenCode runtime startup details for Android auto-configuration", async () => {
    const runtime = createFakeRuntime({
      ensureServer: async () => ({
        baseUrl: "http://192.168.0.42:49200",
        host: "192.168.0.42",
        port: 49200,
        username: "mocca",
        password: "secret",
        useHttps: false,
      }),
    });
    const router = createBridgeRouter({
      configSnapshotProvider: async () => ({}),
      openCodeRuntime: runtime,
    });

    await expect(router.handleRequest(createRequest({
      id: "req-capabilities",
      ns: "system",
      action: "capabilities",
    }))).resolves.toMatchObject({
      ok: true,
      payload: {
        ai: {
          opencodeRuntime: true,
          sessions: true,
          messages: true,
        },
      },
    });

    await expect(router.handleRequest(createRequest({
      id: "req-runtime",
      ns: "ai",
      action: "runtime.ensure",
    }))).resolves.toMatchObject({
      ok: true,
      payload: {
        status: "ready",
        server: {
          host: "192.168.0.42",
          port: 49200,
          username: "mocca",
          password: "secret",
          useHttps: false,
        },
      },
    });
  });

  it("routes session requests through the OpenCode runtime bridge", async () => {
    const runtime = createFakeRuntime({
      listSessions: async () => [{ id: "ses-1", title: "Runtime session" }],
      createSession: async (payload) => ({ id: "ses-2", title: (payload as { title?: string }).title }),
    });
    const router = createBridgeRouter({
      configSnapshotProvider: async () => ({}),
      openCodeRuntime: runtime,
    });

    await expect(router.handleRequest(createRequest({
      id: "req-list",
      ns: "ai",
      action: "sessions.list",
    }))).resolves.toMatchObject({
      ok: true,
      payload: [{ id: "ses-1" }],
    });

    await expect(router.handleRequest(createRequest({
      id: "req-create",
      ns: "ai",
      action: "sessions.create",
      payload: { title: "New" },
    }))).resolves.toMatchObject({
      ok: true,
      payload: { id: "ses-2", title: "New" },
    });
  });

  it("returns typed errors for unknown requests", async () => {
    const router = createBridgeRouter({
      configSnapshotProvider: async () => ({}),
    });

    const response = await router.handleRequest(createRequest({
      id: "req-missing",
      ns: "git",
      action: "status",
    }));

    expect(response).toMatchObject({
      id: "req-missing",
      ns: "git",
      action: "status",
      ok: false,
      error: {
        code: "not_found",
      },
    });
  });
});

function createFakeRuntime(overrides: Partial<OpenCodeRuntimeBridge> = {}): OpenCodeRuntimeBridge {
  return {
    getStatus: () => ({ status: "idle", projectDir: "C:/workspace" }),
    ensureServer: async () => ({
      baseUrl: "http://127.0.0.1:49200",
      host: "127.0.0.1",
      port: 49200,
      username: "mocca",
      password: "secret",
      useHttps: false,
    }),
    listSessions: async () => [],
    createSession: async () => ({}),
    getMessages: async () => [],
    sendMessage: async () => ({ ack: true }),
    abortSession: async () => ({}),
    listProviders: async () => ({ all: [] }),
    listProviderConfig: async () => ({ providers: [], default: {} }),
    listAgents: async () => [],
    listCommands: async () => [],
    getMcpStatus: async () => ({}),
    subscribe: () => () => {},
    close: async () => {},
    ...overrides,
  };
}
