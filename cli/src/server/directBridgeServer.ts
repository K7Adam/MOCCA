import { createServer, type Server as HttpServer } from "node:http";
import { AddressInfo } from "node:net";
import { WebSocketServer, type WebSocket } from "ws";
import { createPairingUrl } from "../pairing/pairing.js";
import { PROTOCOL_VERSION, createRequest, parseProtocolFrame, type MoccaRequest } from "../protocol/message.js";
import { createBridgeRouter, type BridgeRouterOptions } from "./router.js";

const WEBSOCKET_PATH = "/v1/ws";
const HEALTH_PATH = "/v1/health";

export type DirectBridgeServerOptions = BridgeRouterOptions & {
  host: string;
  advertiseHost?: string;
  port: number;
  pairingCode: string;
  fallbackToRandomPort?: boolean;
};

export type DirectBridgeServer = {
  urls: {
    websocketUrl: string;
    healthUrl: string;
    pairingUrl: string;
  };
  pairingCode: string;
  close: () => Promise<void>;
};

export async function startDirectBridgeServer(options: DirectBridgeServerOptions): Promise<DirectBridgeServer> {
  const httpServer = createServer((request, response) => {
    const requestUrl = new URL(request.url ?? "/", `http://${request.headers.host ?? formatHostForUrl(options.host)}`);
    if (request.method === "GET" && requestUrl.pathname === HEALTH_PATH) {
      response.writeHead(200, { "content-type": "application/json" });
      response.end(
        JSON.stringify({
          ok: true,
          protocolVersion: PROTOCOL_VERSION,
          pairingRequired: true,
          websocketPath: WEBSOCKET_PATH,
        }),
      );
      return;
    }

    response.writeHead(404, { "content-type": "application/json" });
    response.end(JSON.stringify({ ok: false, error: "not_found" }));
  });

  const websocketServer = new WebSocketServer({ noServer: true });
  const router = createBridgeRouter({
    ...options,
    eventSink: (event) => {
      options.eventSink?.(event);
      const text = JSON.stringify(event);
      for (const client of websocketServer.clients) {
        if (client.readyState === client.OPEN) {
          client.send(text);
        }
      }
    },
  });

  httpServer.on("upgrade", (request, socket, head) => {
    const requestUrl = new URL(request.url ?? "/", `http://${request.headers.host ?? formatHostForUrl(options.host)}`);
    if (requestUrl.pathname !== WEBSOCKET_PATH) {
      socket.destroy();
      return;
    }

    if (requestUrl.searchParams.get("pairingCode") !== options.pairingCode) {
      websocketServer.handleUpgrade(request, socket, head, (websocket) => {
        websocket.close(1008, "Invalid pairing code");
      });
      return;
    }

    websocketServer.handleUpgrade(request, socket, head, (websocket) => {
      websocketServer.emit("connection", websocket, request);
    });
  });

  websocketServer.on("connection", (websocket) => {
    websocket.on("message", async (data) => {
      await handleSocketMessage(websocket, data.toString(), router.handleRequest);
    });
  });

  try {
    await listen(httpServer, options.port, options.host);
  } catch (error) {
    if (options.fallbackToRandomPort === true && options.port !== 0 && isAddressInUseError(error)) {
      await listen(httpServer, 0, options.host);
    } else {
      throw error;
    }
  }
  const address = httpServer.address();
  if (!isAddressInfo(address)) {
    throw new Error("Unable to resolve bridge server address");
  }

  const urlHost = formatHostForUrl(options.advertiseHost ?? options.host);
  const websocketUrl = new URL(`ws://${urlHost}:${address.port}${WEBSOCKET_PATH}`);
  websocketUrl.searchParams.set("pairingCode", options.pairingCode);
  const healthUrl = new URL(`http://${urlHost}:${address.port}${HEALTH_PATH}`);
  const pairingUrl = createPairingUrl({
    host: urlHost,
    port: address.port,
    pairingCode: options.pairingCode,
    useTls: false,
  });

  return {
    urls: {
      websocketUrl: websocketUrl.toString(),
      healthUrl: healthUrl.toString(),
      pairingUrl,
    },
    pairingCode: options.pairingCode,
    close: async () => {
      await router.close();
      await options.openCodeRuntime?.close();
      await closeWebsocketServer(websocketServer);
      await closeHttpServer(httpServer);
    },
  };
}

async function handleSocketMessage(
  websocket: WebSocket,
  text: string,
  handleRequest: (request: MoccaRequest) => Promise<unknown>,
): Promise<void> {
  let parsed: unknown;
  try {
    parsed = JSON.parse(text);
  } catch (error) {
    websocket.send(JSON.stringify(createInvalidFrameResponse(error)));
    return;
  }

  try {
    const frame = parseProtocolFrame(parsed);
    if (!("action" in frame) || "ok" in frame) {
      websocket.send(JSON.stringify(createInvalidFrameResponse(new Error("Expected request frame"))));
      return;
    }
    websocket.send(JSON.stringify(await handleRequest(frame)));
  } catch (error) {
    websocket.send(JSON.stringify(createInvalidFrameResponse(error)));
  }
}

function createInvalidFrameResponse(error: unknown) {
  return {
    ...createRequest({ id: "invalid-frame", ns: "system", action: "parse" }),
    ok: false,
    error: {
      code: "invalid_frame",
      message: error instanceof Error ? error.message : String(error),
    },
  };
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

function closeWebsocketServer(server: WebSocketServer): Promise<void> {
  return new Promise((resolve, reject) => {
    for (const client of server.clients) {
      client.close();
    }
    server.close((error) => {
      if (error != null) {
        reject(error);
        return;
      }
      resolve();
    });
  });
}

function isAddressInfo(address: string | AddressInfo | null): address is AddressInfo {
  return typeof address === "object" && address != null && typeof address.port === "number";
}

function formatHostForUrl(host: string): string {
  return host.includes(":") && !host.startsWith("[") ? `[${host}]` : host;
}

function isAddressInUseError(error: unknown): boolean {
  return typeof error === "object" && error != null && "code" in error && error.code === "EADDRINUSE";
}
