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
      rmSync(tempDir, { recursive: true, force: true });
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
});
