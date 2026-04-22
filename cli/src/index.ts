#!/usr/bin/env node
import { randomInt } from "node:crypto";
import { collectOpenCodeConfigSnapshot } from "./opencode/configSnapshot.js";
import { selectAdvertiseHost } from "./pairing/networkHost.js";
import { formatStartupMessage } from "./pairing/startupMessage.js";
import { renderPairingQr } from "./pairing/terminalQr.js";
import { OpenCodeRuntimeManager } from "./opencode/runtimeServer.js";
import { startDirectBridgeServer } from "./server/directBridgeServer.js";

const args = process.argv.slice(2);

if (args[0] === "snapshot") {
  const snapshot = await collectOpenCodeConfigSnapshot({
    projectDir: process.cwd(),
  });
  console.log(JSON.stringify(snapshot, null, 2));
} else {
  const host = readOption(args, "--host") ?? "0.0.0.0";
  const advertiseHost = readOption(args, "--advertise-host") ?? selectAdvertiseHost(host);
  const portArg = readOption(args, "--port");
  const port = Number(portArg ?? "17653");
  const pairingCode = readOption(args, "--pairing-code") ?? createPairingCode();
  const openCodeRuntime = new OpenCodeRuntimeManager({
    projectDir: process.cwd(),
    bindHost: host,
    advertiseHost,
  });

  const server = await startDirectBridgeServer({
    projectDir: process.cwd(),
    host,
    advertiseHost,
    port,
    pairingCode,
    fallbackToRandomPort: portArg == null,
    configSnapshotProvider: () => collectOpenCodeConfigSnapshot({ projectDir: process.cwd() }),
    openCodeRuntime,
  });

  console.log(formatStartupMessage({
    pairingCode: server.pairingCode,
    pairingUrl: server.urls.pairingUrl,
    healthUrl: server.urls.healthUrl,
    websocketUrl: server.urls.websocketUrl,
    qrCode: renderPairingQr(server.urls.pairingUrl),
  }));

  const shutdown = async () => {
    await server.close();
    process.exit(0);
  };

  process.once("SIGINT", () => {
    void shutdown();
  });
  process.once("SIGTERM", () => {
    void shutdown();
  });
}

function readOption(args: readonly string[], name: string): string | undefined {
  const index = args.indexOf(name);
  if (index < 0) return undefined;
  return args[index + 1];
}

function createPairingCode(): string {
  return randomInt(0, 1_000_000).toString().padStart(6, "0");
}
