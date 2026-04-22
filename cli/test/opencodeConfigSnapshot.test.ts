import { mkdtemp, mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { describe, expect, it } from "vitest";
import { collectOpenCodeConfigSnapshot } from "../src/opencode/configSnapshot";

describe("OpenCode config snapshot", () => {
  it("merges local OpenCode config, credentials, agents, commands, and MCP without exposing secrets", async () => {
    const root = await mkdtemp(join(tmpdir(), "mocca-opencode-config-"));
    const homeDir = join(root, "home");
    const appData = join(root, "AppData", "Roaming");
    const projectDir = join(root, "project");
    const globalConfigDir = join(homeDir, ".config", "opencode");

    await mkdir(globalConfigDir, { recursive: true });
    await mkdir(projectDir, { recursive: true });
    await mkdir(appData, { recursive: true });

    await writeFile(join(globalConfigDir, "opencode.json"), JSON.stringify({
      model: "global/model",
      plugin: ["global-plugin@latest"],
      provider: {
        openai: {
          apiKey: "secret-global-api-key",
          baseURL: "https://api.openai.com/v1",
        },
      },
      tools: {
        git_status: true,
        git_commit: false,
      },
    }));

    await writeFile(join(projectDir, "opencode.json"), JSON.stringify({
      model: "project/model",
      plugin: ["project-plugin@latest"],
      command: {
        "mocca-build": {
          description: "Build debug APK",
          template: "gradlew assembleDebug",
        },
      },
      mcp: {
        stitch: {
          type: "remote",
          url: "https://stitch.googleapis.com/mcp",
          enabled: true,
          headers: {
            "X-Goog-Api-Key": "{env:STITCH_API_KEY}",
          },
        },
      },
      tools: {
        git_status: false,
        git_pull: true,
      },
    }));

    const snapshot = await collectOpenCodeConfigSnapshot({
      projectDir,
      homeDir,
      appDataRoamingDir: appData,
      runCommand: async (command, args) => {
        if (command !== "opencode") throw new Error(`Unexpected command: ${command}`);
        const joined = args.join(" ");
        if (joined === "--version") return { exitCode: 0, stdout: "1.14.19\n", stderr: "" };
        if (joined === "providers list") {
          return {
            exitCode: 0,
            stdout: [
              "T  Credentials ~\\.local\\share\\opencode\\auth.json",
              "|",
              "•  Z.AI Coding Plan api",
              "|",
              "•  OpenAI oauth",
              "|",
              "—  2 credentials",
              "",
            ].join("\n"),
            stderr: "",
          };
        }
        if (joined === "agent list") {
          return {
            exitCode: 0,
            stdout: [
              "build (primary)",
              "  [",
              "  {",
              "    \"permission\":\"*\",",
              "    \"action\":\"allow\",",
              "    \"pattern\":\"*\"",
              "  }",
              "  ]",
              "plan",
              "  []",
            ].join("\n"),
            stderr: "",
          };
        }
        throw new Error(`Unexpected args: ${joined}`);
      },
    });

    expect(snapshot.installed).toEqual({
      available: true,
      command: "opencode",
      version: "1.14.19",
    });
    expect(snapshot.configFiles.map((file) => file.scope)).toEqual(["global", "project"]);
    expect(snapshot.effective.model).toBe("project/model");
    expect(snapshot.effective.plugins).toEqual(["global-plugin@latest", "project-plugin@latest"]);
    expect(snapshot.effective.tools).toMatchObject({
      git_status: false,
      git_commit: false,
      git_pull: true,
    });
    expect(snapshot.credentials).toEqual([
      { name: "Z.AI Coding Plan", type: "api" },
      { name: "OpenAI", type: "oauth" },
    ]);
    expect(snapshot.agents).toEqual([
      { name: "build", primary: true },
      { name: "plan", primary: false },
    ]);
    expect(snapshot.commands).toEqual([
      { name: "mocca-build", description: "Build debug APK" },
    ]);
    expect(snapshot.mcpServers).toEqual([
      { name: "stitch", type: "remote", enabled: true },
    ]);
    expect(JSON.stringify(snapshot)).not.toContain("secret-global-api-key");
    expect(JSON.stringify(snapshot)).not.toContain("STITCH_API_KEY");
  });
});
