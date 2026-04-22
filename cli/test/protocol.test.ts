import { describe, expect, it } from "vitest";
import {
  createEvent,
  createRequest,
  createResponse,
  parseProtocolFrame,
} from "../src/protocol/message";

describe("MOCCA bridge protocol messages", () => {
  it("creates versioned request, response, and event envelopes", () => {
    const request = createRequest({
      id: "req-1",
      ns: "system",
      action: "capabilities",
      payload: { includeExperimental: true },
    });

    expect(request).toEqual({
      v: 1,
      id: "req-1",
      ns: "system",
      action: "capabilities",
      payload: { includeExperimental: true },
    });

    expect(createResponse(request, { ok: true, payload: { ready: true } })).toEqual({
      v: 1,
      id: "req-1",
      ns: "system",
      action: "capabilities",
      ok: true,
      payload: { ready: true },
    });

    expect(createEvent({ ns: "ai", event: "config.snapshot", seq: 7, payload: { model: "openai/gpt-5" } })).toEqual({
      v: 1,
      ns: "ai",
      event: "config.snapshot",
      seq: 7,
      payload: { model: "openai/gpt-5" },
    });
  });

  it("rejects unsupported or malformed protocol frames", () => {
    expect(() => parseProtocolFrame({ v: 2, ns: "system", event: "ready" })).toThrow(/unsupported protocol version/i);
    expect(() => parseProtocolFrame({ v: 1, id: "", ns: "system", action: "ping" })).toThrow(/id/i);
    expect(() => parseProtocolFrame({ v: 1, ns: "system" })).toThrow(/frame/i);
  });
});
