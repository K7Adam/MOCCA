import { execFileSync } from "node:child_process";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { afterEach, describe, expect, it } from "vitest";
import { createRequest } from "../src/protocol/message";
import { createBridgeRouter, type BridgeRouter } from "../src/server/router";

describe("native CLI capabilities", () => {
  let tempDirs: string[] = [];
  let router: BridgeRouter | undefined;

  afterEach(async () => {
    await router?.close();
    router = undefined;
    for (const tempDir of tempDirs) {
      await removeTempDir(tempDir);
    }
    tempDirs = [];
  });

  it("keeps file-system requests inside the project root", async () => {
    const projectDir = tempProject();
    router = createRouter(projectDir);

    const response = await router.handleRequest(createRequest({
      id: "req-fs-escape",
      ns: "fs",
      action: "list",
      payload: { path: ".." },
    }));

    expect(response).toMatchObject({
      ok: false,
      error: { code: "invalid_path" },
    });
  });

  it("lists, writes, reads, and protects deletes with confirmation", async () => {
    const projectDir = tempProject();
    router = createRouter(projectDir);

    await expect(router.handleRequest(createRequest({
      id: "req-write",
      ns: "fs",
      action: "write",
      payload: { path: "src/app.txt", content: "hello mocca" },
    }))).resolves.toMatchObject({ ok: true });

    await expect(router.handleRequest(createRequest({
      id: "req-list",
      ns: "fs",
      action: "list",
      payload: { path: "src" },
    }))).resolves.toMatchObject({
      ok: true,
      payload: [expect.objectContaining({ path: "src/app.txt", type: "file" })],
    });

    await expect(router.handleRequest(createRequest({
      id: "req-read",
      ns: "fs",
      action: "read",
      payload: { path: "src/app.txt" },
    }))).resolves.toMatchObject({
      ok: true,
      payload: expect.objectContaining({ content: "hello mocca" }),
    });

    const deleteResponse = await router.handleRequest(createRequest({
      id: "req-delete",
      ns: "fs",
      action: "delete",
      payload: { path: "src/app.txt" },
    }));
    expect(deleteResponse).toMatchObject({
      ok: false,
      error: { code: "confirmation_required" },
    });

    const operationId = (deleteResponse.error?.details as { operationId: string }).operationId;
    await expect(router.handleRequest(createRequest({
      id: "req-delete-confirmed",
      ns: "fs",
      action: "delete",
      payload: { path: "src/app.txt", confirmation: { operationId } },
    }))).resolves.toMatchObject({ ok: true });
  });

  it("reports git status and enforces confirmation for destructive discard", async () => {
    const projectDir = tempProject();
    git(projectDir, "init");
    git(projectDir, "config", "user.email", "mocca@example.test");
    git(projectDir, "config", "user.name", "MOCCA Test");
    writeFileSync(path.join(projectDir, "tracked.txt"), "before\n", "utf8");
    git(projectDir, "add", "tracked.txt");
    git(projectDir, "commit", "-m", "initial");
    writeFileSync(path.join(projectDir, "tracked.txt"), "after\n", "utf8");
    router = createRouter(projectDir);

    await expect(router.handleRequest(createRequest({
      id: "req-status",
      ns: "git",
      action: "status",
    }))).resolves.toMatchObject({
      ok: true,
      payload: expect.objectContaining({
        unstaged: [expect.objectContaining({ path: "tracked.txt" })],
      }),
    });

    const discardResponse = await router.handleRequest(createRequest({
      id: "req-discard",
      ns: "git",
      action: "discard",
      payload: { files: ["tracked.txt"] },
    }));
    expect(discardResponse).toMatchObject({
      ok: false,
      error: { code: "confirmation_required" },
    });

    const operationId = (discardResponse.error?.details as { operationId: string }).operationId;
    await expect(router.handleRequest(createRequest({
      id: "req-discard-confirmed",
      ns: "git",
      action: "discard",
      payload: { files: ["tracked.txt"], confirmation: { operationId } },
    }))).resolves.toMatchObject({ ok: true });
  });

  it("spawns a terminal grid session and returns a bounded snapshot", async () => {
    const projectDir = tempProject();
    router = createRouter(projectDir);

    const response = await router.handleRequest(createRequest({
      id: "req-terminal-spawn",
      ns: "terminal",
      action: "spawn",
      payload: { cols: 80, rows: 12 },
    }));
    expect(response).toMatchObject({
      ok: true,
      payload: expect.objectContaining({ cols: 80, rows: 12 }),
    });

    const terminalId = (response.payload as { id: string }).id;
    await expect(router.handleRequest(createRequest({
      id: "req-terminal-snapshot",
      ns: "terminal",
      action: "snapshot",
      payload: { terminalId },
    }))).resolves.toMatchObject({
      ok: true,
      payload: expect.objectContaining({
        terminalId,
        rows: 12,
        fullFrame: true,
      }),
    });
  });

  it("executes terminal input and exposes command output in snapshots", async () => {
    const projectDir = tempProject();
    router = createRouter(projectDir);

    const spawnResponse = await router.handleRequest(createRequest({
      id: "req-terminal-exec-spawn",
      ns: "terminal",
      action: "spawn",
      payload: { cols: 100, rows: 20, shell: defaultShell() },
    }));
    expect(spawnResponse.ok).toBe(true);
    const terminalId = (spawnResponse.payload as { id: string }).id;

    await expect(router.handleRequest(createRequest({
      id: "req-terminal-exec-write",
      ns: "terminal",
      action: "write",
      payload: { terminalId, data: "echo MOCCA_TERMINAL_ECHO\r" },
    }))).resolves.toMatchObject({ ok: true });

    await expect(readTerminalTextUntil(terminalId, "MOCCA_TERMINAL_ECHO")).resolves.toContain("MOCCA_TERMINAL_ECHO");
  });

  function tempProject(): string {
    const dir = mkdtempSync(path.join(tmpdir(), "mocca-native-cap-"));
    tempDirs.push(dir);
    return dir;
  }

  function createRouter(projectDir: string): BridgeRouter {
    return createBridgeRouter({
      projectDir,
      configSnapshotProvider: async () => ({}),
    });
  }

  function git(cwd: string, ...args: string[]): void {
    execFileSync("git", args, { cwd, stdio: "ignore" });
  }

  function defaultShell(): string {
    if (process.platform === "win32") return process.env.COMSPEC ?? "cmd.exe";
    return process.env.SHELL ?? "/bin/sh";
  }

  async function readTerminalTextUntil(terminalId: string, expected: string): Promise<string> {
    const deadline = Date.now() + 4_000;
    let lastText = "";
    while (Date.now() < deadline) {
      const snapshot = await router!.handleRequest(createRequest({
        id: `req-terminal-exec-snapshot-${Date.now()}`,
        ns: "terminal",
        action: "snapshot",
        payload: { terminalId },
      }));
      expect(snapshot.ok).toBe(true);
      lastText = frameText(snapshot.payload);
      if (lastText.includes(expected)) {
        return lastText;
      }
      await delay(100);
    }
    return lastText;
  }

  function frameText(payload: unknown): string {
    const cells = (payload as { cells?: Record<string, Array<{ char?: string }>> }).cells ?? {};
    return Object.keys(cells)
      .sort((left, right) => Number(left) - Number(right))
      .map((row) => cells[row].map((cell) => cell.char ?? " ").join(""))
      .join("\n");
  }

  function delay(milliseconds: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, milliseconds));
  }

  async function removeTempDir(tempDir: string): Promise<void> {
    for (let attempt = 0; attempt < 8; attempt += 1) {
      try {
        rmSync(tempDir, { recursive: true, force: true });
        return;
      } catch (error) {
        if (attempt === 7) throw error;
        await delay(100);
      }
    }
  }
});
