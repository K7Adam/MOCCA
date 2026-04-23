import { describe, expect, it } from "vitest";
import { formatStartupMessage } from "../src/pairing/startupMessage";

describe("CLI startup message", () => {
  it("includes QR code, pairing URL, health URL, websocket URL, and pairing code", () => {
    const message = formatStartupMessage({
      pairingCode: "123456",
      pairingUrl: "mocca://bridge/connect?v=1&host=192.168.0.10&port=17653&pairingCode=123456&tls=0",
      healthUrl: "http://192.168.0.10:17653/v1/health",
      websocketUrl: "ws://192.168.0.10:17653/v1/ws?pairingCode=123456",
      qrCode: "QR-CODE",
    });

    expect(message).toContain("MOCCA CLI bridge is running");
    expect(message).toContain("QR-CODE");
    expect(message).toContain("Pairing code: 123456");
    expect(message).toContain("Pairing URL: mocca://bridge/connect");
    expect(message).toContain("Health: http://192.168.0.10:17653/v1/health");
    expect(message).toContain("WebSocket: ws://192.168.0.10:17653/v1/ws?pairingCode=123456");
    expect(message).toContain("OpenCode runtime starts automatically after pairing.");
  });

  it("includes network mode when the CLI was started for a specific network", () => {
    const message = formatStartupMessage({
      pairingCode: "123456",
      pairingUrl: "mocca://bridge/connect?v=1&host=100.86.20.31&port=17653&pairingCode=123456&tls=0&network=tailscale",
      healthUrl: "http://100.86.20.31:17653/v1/health",
      websocketUrl: "ws://100.86.20.31:17653/v1/ws?pairingCode=123456",
      qrCode: "QR-CODE",
      networkMode: "tailscale",
    });

    expect(message).toContain("Network: Tailscale");
  });
});
