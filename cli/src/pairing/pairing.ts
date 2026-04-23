export type PairingUrlOptions = {
  host: string;
  port: number;
  pairingCode: string;
  useTls?: boolean;
  network?: "lan" | "tailscale";
};

export function createPairingUrl(options: PairingUrlOptions): string {
  assertNonBlank(options.host, "host");
  assertNonBlank(options.pairingCode, "pairingCode");
  if (!Number.isInteger(options.port) || options.port < 1 || options.port > 65535) {
    throw new Error("Pairing port must be between 1 and 65535");
  }

  const params = new URLSearchParams();
  params.set("v", "1");
  params.set("host", options.host.trim());
  params.set("port", String(options.port));
  params.set("pairingCode", options.pairingCode);
  params.set("tls", options.useTls === true ? "1" : "0");
  if (options.network != null) {
    params.set("network", options.network);
  }
  return `mocca://bridge/connect?${params.toString()}`;
}

function assertNonBlank(value: string, field: string): void {
  if (value.trim().length === 0) {
    throw new Error(`Pairing ${field} must not be blank`);
  }
}
