import { createServer, type Server as HttpServer } from "node:http";
import { AddressInfo } from "node:net";
import WebSocket from "ws";
import { afterEach, describe, expect, it } from "vitest";
import { startDirectBridgeServer, type DirectBridgeServer } from "../src/server/directBridgeServer";
import { createRequest } from "../src/protocol/message";
import type { OpenCodeRuntimeBridge, OpenCodeRuntimeEvent } from "../src/opencode/runtimeServer";

const TEST_PROJECT_DIR = process.cwd();

describe("direct CLI bridge server", () => {
  let server: DirectBridgeServer | undefined;

  afterEach(async () => {
    await server?.close();
    server = undefined;
  });

  it("accepts a paired websocket client and handles protocol requests", async () => {
    server = await startDirectBridgeServer({
      projectDir: TEST_PROJECT_DIR,
      host: "127.0.0.1",
      port: 0,
      pairingCode: "123456",
      configSnapshotProvider: async () => ({
        installed: { available: true, command: "opencode", version: "1.14.19" },
      }),
    });

    const socket = await openSocket(server.urls.websocketUrl);
    socket.send(JSON.stringify(createRequest({ id: "req-1", ns: "system", action: "capabilities" })));

    await expect(readJson(socket)).resolves.toMatchObject({
      id: "req-1",
      ns: "system",
      action: "capabilities",
      ok: true,
      payload: {
        protocolVersion: 1,
      },
    });

    socket.close();
  });

  it("exposes a QR-ready pairing URL for the active server address", async () => {
    server = await startDirectBridgeServer({
      projectDir: TEST_PROJECT_DIR,
      host: "127.0.0.1",
      port: 0,
      pairingCode: "123456",
      configSnapshotProvider: async () => ({}),
    });

    const healthUrl = new URL(server.urls.healthUrl);

    expect(server.urls.pairingUrl).toBe(
      `mocca://bridge/connect?v=1&host=127.0.0.1&port=${healthUrl.port}&pairingCode=123456&tls=0`,
    );
  });

  it("uses an advertised host for user-facing URLs while binding to a different host", async () => {
    server = await startDirectBridgeServer({
      projectDir: TEST_PROJECT_DIR,
      host: "127.0.0.1",
      advertiseHost: "192.168.0.42",
      port: 0,
      pairingCode: "123456",
      configSnapshotProvider: async () => ({}),
    });

    const healthUrl = new URL(server.urls.healthUrl);

    expect(healthUrl.hostname).toBe("192.168.0.42");
    expect(server.urls.websocketUrl).toContain("ws://192.168.0.42:");
    expect(server.urls.pairingUrl).toBe(
      `mocca://bridge/connect?v=1&host=192.168.0.42&port=${healthUrl.port}&pairingCode=123456&tls=0`,
    );
  });

  it("rejects a client with the wrong pairing code", async () => {
    server = await startDirectBridgeServer({
      projectDir: TEST_PROJECT_DIR,
      host: "127.0.0.1",
      port: 0,
      pairingCode: "123456",
      configSnapshotProvider: async () => ({}),
    });

    const badUrl = new URL(server.urls.websocketUrl);
    badUrl.searchParams.set("pairingCode", "000000");
    const socket = await openSocket(badUrl.toString());

    await expect(waitForClose(socket)).resolves.toMatchObject({ code: 1008 });
  });

  it("returns a typed error for invalid JSON frames", async () => {
    server = await startDirectBridgeServer({
      projectDir: TEST_PROJECT_DIR,
      host: "127.0.0.1",
      port: 0,
      pairingCode: "123456",
      configSnapshotProvider: async () => ({}),
    });

    const socket = await openSocket(server.urls.websocketUrl);
    socket.send("{not-json");

    await expect(readJson(socket)).resolves.toMatchObject({
      v: 1,
      id: "invalid-frame",
      ns: "system",
      action: "parse",
      ok: false,
      error: {
        code: "invalid_frame",
      },
    });

    socket.close();
  });

  it("exposes health metadata without requiring a websocket pairing code", async () => {
    server = await startDirectBridgeServer({
      projectDir: TEST_PROJECT_DIR,
      host: "127.0.0.1",
      port: 0,
      pairingCode: "123456",
      configSnapshotProvider: async () => ({}),
    });

    const response = await fetch(server.urls.healthUrl);

    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toMatchObject({
      ok: true,
      protocolVersion: 1,
      pairingRequired: true,
      websocketPath: "/v1/ws",
    });
  });

  it("broadcasts OpenCode runtime events to paired clients", async () => {
    let listener: ((event: OpenCodeRuntimeEvent) => void) | undefined;
    const runtime = createFakeRuntime({
      subscribe: (next) => {
        listener = next;
        return () => {
          listener = undefined;
        };
      },
    });
    server = await startDirectBridgeServer({
      projectDir: TEST_PROJECT_DIR,
      host: "127.0.0.1",
      port: 0,
      pairingCode: "123456",
      configSnapshotProvider: async () => ({}),
      openCodeRuntime: runtime,
    });

    const socket = await openSocket(server.urls.websocketUrl);
    listener?.({
      type: "message.updated",
      properties: { sessionID: "ses-1" },
    });

    await expect(readJson(socket)).resolves.toMatchObject({
      v: 1,
      ns: "ai",
      event: "runtime.event",
      payload: {
        type: "message.updated",
        properties: { sessionID: "ses-1" },
      },
    });

    socket.close();
  });

  it("falls back to an ephemeral port when the preferred port is unavailable", async () => {
    const blocker = createServer((_request, response) => {
      response.writeHead(200);
      response.end("busy");
    });
    await listen(blocker, 0, "127.0.0.1");
    const blockedPort = getPort(blocker);

    try {
      server = await startDirectBridgeServer({
        projectDir: TEST_PROJECT_DIR,
        host: "127.0.0.1",
        port: blockedPort,
        pairingCode: "123456",
        fallbackToRandomPort: true,
        configSnapshotProvider: async () => ({}),
      });

      const healthUrl = new URL(server.urls.healthUrl);
      expect(Number(healthUrl.port)).not.toBe(blockedPort);

      const response = await fetch(server.urls.healthUrl);
      expect(response.status).toBe(200);
    } finally {
      await closeHttpServer(blocker);
    }
  });
});

function openSocket(url: string): Promise<WebSocket> {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(url);
    socket.once("open", () => resolve(socket));
    socket.once("error", reject);
  });
}

function readJson(socket: WebSocket): Promise<unknown> {
  return new Promise((resolve, reject) => {
    socket.once("message", (data) => {
      try {
        resolve(JSON.parse(data.toString()));
      } catch (error) {
        reject(error);
      }
    });
    socket.once("error", reject);
  });
}

function waitForClose(socket: WebSocket): Promise<{ code: number; reason: string }> {
  return new Promise((resolve) => {
    socket.once("close", (code, reason) => {
      resolve({ code, reason: reason.toString() });
    });
  });
}

function listen(server: HttpServer, port: number, host: string): Promise<void> {
  return new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(port, host, () => {
      server.off("error", reject);
      resolve();
    });
  });
}

function closeHttpServer(server: HttpServer): Promise<void> {
  return new Promise((resolve, reject) => {
    server.close((error) => {
      if (error != null) {
        reject(error);
        return;
      }
      resolve();
    });
  });
}

function getPort(server: HttpServer): number {
  const address = server.address();
  if (typeof address === "object" && address != null) {
    return (address as AddressInfo).port;
  }
  throw new Error("Server is not listening on a TCP port");
}

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
