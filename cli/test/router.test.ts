import { describe, expect, it } from "vitest";
import { createRequest } from "../src/protocol/message";
import { createBridgeRouter } from "../src/server/router";
import type { OpenCodeRuntimeBridge } from "../src/opencode/runtimeServer";

const TEST_PROJECT_DIR = process.cwd();

describe("MOCCA CLI request router", () => {
  it("reports bridge capabilities without exposing implementation details", async () => {
    const router = createBridgeRouter({
      projectDir: TEST_PROJECT_DIR,
      configSnapshotProvider: async () => ({ installed: { available: true, command: "opencode", version: "1.14.19" } }),
    });

    const response = await router.handleRequest(createRequest({
      id: "req-capabilities",
      ns: "system",
      action: "capabilities",
    }));

    expect(response).toMatchObject({
      v: 2,
      id: "req-capabilities",
      ns: "system",
      action: "capabilities",
      ok: true,
      payload: {
        protocolVersion: 2,
        namespaces: expect.arrayContaining(["system", "ai"]),
        ai: {
          opencodeConfigSnapshot: true,
          configNormalized: true,
          providers: true,
          agents: true,
          modes: true,
          selectionDefaults: true,
          variantForwarding: true,
          configEvents: true,
          events: false,
          eventReplay: false,
          permissions: false,
          questions: false,
          sessionStatus: false,
          usage: false,
        },
      },
    });
  });

  it("returns normalized AI runtime config through ai.config.get", async () => {
    const router = createBridgeRouter({
      projectDir: TEST_PROJECT_DIR,
      configSnapshotProvider: async () => ({
        installed: { available: true, command: "opencode", version: "1.14.19" },
        effective: { model: "anthropic/claude-3-5", plugins: [], tools: {}, raw: {} },
        configFiles: [],
        credentials: [],
        agents: [{ name: "build", primary: true }],
        commands: [],
        mcpServers: [],
      }),
      openCodeRuntime: createFakeRuntime({
        listProviders: async () => ({
          all: [{
            id: "anthropic",
            name: "Anthropic",
            models: {
              "claude-3-5": {
                name: "Claude 3.5",
                variants: {
                  fast: { name: "Fast" },
                },
              },
            },
          }],
          connected: ["anthropic"],
        }),
        listAgents: async () => [{ id: "build", name: "Build", primary: true }],
      }),
    });

    await expect(router.handleRequest(createRequest({
      id: "req-ai-config",
      ns: "ai",
      action: "config.get",
    }))).resolves.toMatchObject({
      ok: true,
      payload: {
        source: "mocca-cli",
        defaultSelection: {
          providerId: "anthropic",
          modelId: "claude-3-5",
          agentId: "build",
        },
        providers: [{
          id: "anthropic",
          models: [{
            id: "claude-3-5",
            variants: [{ id: "fast" }],
          }],
        }],
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
      projectDir: TEST_PROJECT_DIR,
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
      projectDir: TEST_PROJECT_DIR,
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
      projectDir: TEST_PROJECT_DIR,
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
          events: true,
          permissions: true,
          questions: true,
          sessionStatus: true,
          usage: true,
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
      projectDir: TEST_PROJECT_DIR,
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

  it("forwards validated message selection fields through ai.messages.send", async () => {
    let forwardedPayload: unknown;
    const runtime = createFakeRuntime({
      sendMessage: async (payload) => {
        forwardedPayload = payload;
        return { ack: true };
      },
    });
    const router = createBridgeRouter({
      projectDir: TEST_PROJECT_DIR,
      configSnapshotProvider: async () => ({}),
      openCodeRuntime: runtime,
    });

    await expect(router.handleRequest(createRequest({
      id: "req-send",
      ns: "ai",
      action: "messages.send",
      payload: {
        sessionId: "ses-1",
        text: "hello",
        model: { providerID: "anthropic", modelID: "claude-3-5" },
        variant: "fast",
        agent: "build",
      },
    }))).resolves.toMatchObject({ ok: true, payload: { ack: true } });

    expect(forwardedPayload).toMatchObject({
      sessionId: "ses-1",
      model: { providerID: "anthropic", modelID: "claude-3-5" },
      variant: "fast",
      agent: "build",
    });
  });

  it("returns typed errors for unknown requests", async () => {
    const router = createBridgeRouter({
      projectDir: TEST_PROJECT_DIR,
      configSnapshotProvider: async () => ({}),
    });

    const response = await router.handleRequest(createRequest({
      id: "req-missing",
      ns: "diagnostics",
      action: "status",
    }));

    expect(response).toMatchObject({
      id: "req-missing",
      ns: "diagnostics",
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
