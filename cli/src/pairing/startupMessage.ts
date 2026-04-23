export type StartupMessageOptions = {
  pairingCode: string;
  pairingUrl: string;
  healthUrl: string;
  websocketUrl: string;
  qrCode: string;
  networkMode?: "lan" | "tailscale";
};

export function formatStartupMessage(options: StartupMessageOptions): string {
  return [
    "MOCCA CLI bridge is running",
    "",
    options.qrCode.trimEnd(),
    "",
    ...(options.networkMode != null ? [`Network: ${formatNetworkMode(options.networkMode)}`] : []),
    `Pairing code: ${options.pairingCode}`,
    `Pairing URL: ${options.pairingUrl}`,
    `Health: ${options.healthUrl}`,
    `WebSocket: ${options.websocketUrl}`,
    "OpenCode runtime starts automatically after pairing.",
    "Press Ctrl+C to stop.",
  ].join("\n");
}

function formatNetworkMode(networkMode: "lan" | "tailscale"): string {
  return networkMode === "tailscale" ? "Tailscale" : "LAN";
}
