import { describe, expect, it } from "vitest";
import { selectAdvertiseHost } from "../src/pairing/networkHost";

describe("bridge advertise host selection", () => {
  it("keeps explicit non-wildcard bind hosts", () => {
    expect(selectAdvertiseHost("127.0.0.1", {})).toBe("127.0.0.1");
    expect(selectAdvertiseHost("mocca.local", {})).toBe("mocca.local");
  });

  it("selects a LAN IPv4 address for wildcard bind hosts", () => {
    expect(selectAdvertiseHost("0.0.0.0", {
      lo: [{ address: "127.0.0.1", family: "IPv4", internal: true }],
      wifi: [{ address: "192.168.0.42", family: "IPv4", internal: false }],
    })).toBe("192.168.0.42");
  });

  it("falls back to localhost when no LAN address is available", () => {
    expect(selectAdvertiseHost("::", {
      lo: [{ address: "::1", family: "IPv6", internal: true }],
    })).toBe("127.0.0.1");
  });
});
