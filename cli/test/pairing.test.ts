import { describe, expect, it } from "vitest";
import { createPairingUrl } from "../src/pairing/pairing";

describe("MOCCA CLI pairing payload", () => {
  it("creates a compact deep link for QR pairing", () => {
    expect(createPairingUrl({
      host: "192.168.0.10",
      port: 17653,
      pairingCode: "123456",
      useTls: false,
    })).toBe("mocca://bridge/connect?v=1&host=192.168.0.10&port=17653&pairingCode=123456&tls=0");
  });

  it("encodes special query values and TLS mode", () => {
    expect(createPairingUrl({
      host: "mocca pc.local",
      port: 443,
      pairingCode: "a b+c/?",
      useTls: true,
    })).toBe("mocca://bridge/connect?v=1&host=mocca+pc.local&port=443&pairingCode=a+b%2Bc%2F%3F&tls=1");
  });
});
