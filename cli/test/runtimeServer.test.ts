import { describe, expect, it } from "vitest";
import { readPromptParts } from "../src/opencode/runtimeServer";

describe("OpenCode runtime prompt normalization", () => {
  it("removes nullish nested fields from prompt parts before forwarding to OpenCode", () => {
    const parts = readPromptParts(
      [
        {
          type: "text",
          text: "hello",
          id: null,
          synthetic: null,
          metadata: {
            keep: true,
            drop: null,
          },
        },
      ],
      undefined,
    );

    expect(parts).toEqual([
      {
        type: "text",
        text: "hello",
        metadata: {
          keep: true,
        },
      },
    ]);
  });

  it("falls back to a text part when no structured parts are present", () => {
    expect(readPromptParts([], "fallback")).toEqual([{ type: "text", text: "fallback" }]);
  });
});
