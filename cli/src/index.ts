#!/usr/bin/env node
import { randomInt } from "node:crypto";
import { collectOpenCodeConfigSnapshot } from "./opencode/configSnapshot.js";
import { detectTailscaleAdvertiseHost, selectAdvertiseHost } from "./pairing/networkHost.js";
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
} else if (hasFlag(args, "--help") || hasFlag(args, "-h")) {
  console.log(formatHelpMessage());
} else {
  const host = readOption(args, "--host") ?? "0.0.0.0";
  const networkMode = normalizeNetworkMode(readOption(args, "--network") ?? readPositionalNetworkMode(args));
  const advertiseHost = resolveAdvertiseHost({
    explicitAdvertiseHost: readOption(args, "--advertise-host"),
    host,
    networkMode,
  });
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
    networkMode,
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
    networkMode,
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

function hasFlag(args: readonly string[], name: string): boolean {
  return args.includes(name);
}

function readPositionalNetworkMode(args: readonly string[]): string | undefined {
  if (hasFlag(args, "--tailscale") || hasFlag(args, "--tailnet")) return "tailscale";
  if (args[0] === "tailscale" || args[0] === "tailnet") return "tailscale";
  return undefined;
}

function normalizeNetworkMode(networkMode: string | undefined): "lan" | "tailscale" | undefined {
  if (networkMode == null || networkMode === "lan") return networkMode;
  if (networkMode === "tailscale" || networkMode === "tailnet") return "tailscale";

  console.error(`Unknown network mode: ${networkMode}`);
  console.error("Use --network lan, --network tailscale, or mocca-cli tailscale.");
  process.exit(1);
}

function resolveAdvertiseHost(options: {
  explicitAdvertiseHost: string | undefined;
  host: string;
  networkMode: "lan" | "tailscale" | undefined;
}): string {
  if (options.explicitAdvertiseHost != null) return options.explicitAdvertiseHost;
  if (options.networkMode == null || options.networkMode === "lan") return selectAdvertiseHost(options.host);
  if (options.networkMode === "tailscale") {
    const tailscaleHost = detectTailscaleAdvertiseHost();
    if (tailscaleHost != null) return tailscaleHost;

    console.error([
      "MOCCA CLI could not find a Tailscale IPv4 address.",
      "Make sure Tailscale is running on this computer, or pass --advertise-host <100.x.x.x>.",
    ].join("\n"));
    process.exit(1);
  }
  throw new Error(`Unhandled network mode: ${options.networkMode}`);
}

function createPairingCode(): string {
  return randomInt(0, 1_000_000).toString().padStart(6, "0");
}

function formatHelpMessage(): string {
  return [
    "MOCCA CLI",
    "",
    "Usage:",
    "  mocca-cli",
    "  mocca-cli tailscale",
    "  mocca-cli --tailscale",
    "",
    "Options:",
    "  --host <host>              Bind host. Defaults to 0.0.0.0.",
    "  --port <port>              Bind port. Defaults to 17653.",
    "  --advertise-host <host>    Host written into the QR code.",
    "  --network <lan|tailscale>  Choose the advertised network.",
    "  --tailscale, --tailnet     Shortcut for --network tailscale.",
    "  --pairing-code <code>      Fixed pairing code for tests.",
    "  -h, --help                 Show this help.",
    "",
    "Examples:",
    "  mocca-cli                  Start for local LAN/Wi-Fi pairing.",
    "  mocca-cli tailscale        Start for pairing over Tailscale.",
  ].join("\n");
}
