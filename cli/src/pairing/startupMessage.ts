export type StartupMessageOptions = {
  pairingCode: string;
  pairingUrl: string;
  healthUrl: string;
  websocketUrl: string;
  qrCode: string;
};

export function formatStartupMessage(options: StartupMessageOptions): string {
  return [
    "MOCCA CLI bridge is running",
    "",
    options.qrCode.trimEnd(),
    "",
    `Pairing code: ${options.pairingCode}`,
    `Pairing URL: ${options.pairingUrl}`,
    `Health: ${options.healthUrl}`,
    `WebSocket: ${options.websocketUrl}`,
    "OpenCode runtime starts automatically after pairing.",
    "Press Ctrl+C to stop.",
  ].join("\n");
}
