import { describe, expect, it } from "vitest";
import { renderPairingQr } from "../src/pairing/terminalQr";

describe("terminal QR pairing output", () => {
  it("renders a non-empty terminal QR code for the pairing URL", () => {
    const qr = renderPairingQr("mocca://bridge/connect?v=1&host=192.168.0.10&port=17653&pairingCode=123456&tls=0");

    expect(qr.trim().length).toBeGreaterThan(40);
    expect(qr.split(/\r?\n/).length).toBeGreaterThan(4);
  });
});
