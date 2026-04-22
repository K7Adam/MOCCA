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

  it("prefers WLAN over Tailscale and other virtual adapters", () => {
    expect(selectAdvertiseHost("0.0.0.0", {
      Tailscale: [{ address: "100.86.20.31", family: "IPv4", internal: false }],
      "vEthernet (WSL)": [{ address: "172.27.112.1", family: "IPv4", internal: false }],
      Docker: [{ address: "172.17.0.1", family: "IPv4", internal: false }],
      WLAN: [{ address: "192.168.0.39", family: "IPv4", internal: false }],
    })).toBe("192.168.0.39");
  });

  it("ignores link-local adapter addresses", () => {
    expect(selectAdvertiseHost("0.0.0.0", {
      Ethernet: [{ address: "169.254.194.115", family: "IPv4", internal: false }],
      WLAN: [{ address: "10.0.0.25", family: "IPv4", internal: false }],
    })).toBe("10.0.0.25");
  });

  it("falls back to localhost when no LAN address is available", () => {
    expect(selectAdvertiseHost("::", {
      lo: [{ address: "::1", family: "IPv6", internal: true }],
    })).toBe("127.0.0.1");
  });
});
