import { createResponse, type MoccaRequest, type MoccaResponse } from "../../protocol/message.js";
import { runCommand } from "../common/childProcess.js";
import { ConfirmationStore, type ConfirmationInput } from "../common/confirmation.js";
import { CapabilityError } from "../common/errors.js";
import { EventSequencer } from "../common/eventSequencer.js";
import type { MoccaEvent } from "../../protocol/message.js";
import { PathSandbox, normalizeRelativePath } from "../common/pathSandbox.js";

export type GitCapabilities = {
  native: boolean;
  porcelainV2: boolean;
  queuedWrites: boolean;
};

export type GitCapabilityOptions = {
  projectDir: string;
  confirmationStore: ConfirmationStore;
  eventSequencer: EventSequencer;
  eventSink?: (event: MoccaEvent) => void;
};

export class GitCapability {
  readonly capabilities: GitCapabilities = {
    native: true,
    porcelainV2: true,
    queuedWrites: true,
  };

  private readonly sandbox: PathSandbox;
  private writeQueue: Promise<unknown> = Promise.resolve();

  constructor(private readonly options: GitCapabilityOptions) {
    this.sandbox = new PathSandbox(options.projectDir);
  }

  async handle(request: MoccaRequest): Promise<MoccaResponse | undefined> {
    if (request.ns !== "git") return undefined;
    switch (request.action) {
      case "status":
        return ok(request, await this.status());
      case "diff":
        return ok(request, await this.diff(readPayload(request)));
      case "branches":
        return ok(request, await this.branches());
      case "log":
        return ok(request, await this.log(readPayload(request)));
      case "commitDetails":
        return ok(request, await this.commitDetails(readPayload(request)));
      case "stage":
        return ok(request, await this.writeOperation("stage", () => this.stage(readPayload(request))));
      case "unstage":
        return ok(request, await this.writeOperation("unstage", () => this.unstage(readPayload(request))));
      case "discard":
        return ok(request, await this.writeOperation("discard", () => this.discard(readPayload(request))));
      case "commit":
        return ok(request, await this.writeOperation("commit", () => this.commit(readPayload(request))));
      case "fetch":
        return ok(request, await this.writeOperation("fetch", () => this.fetch(readPayload(request))));
      case "pull":
        return ok(request, await this.writeOperation("pull", () => this.pull(readPayload(request))));
      case "push":
        return ok(request, await this.writeOperation("push", () => this.push(readPayload(request))));
      case "checkout":
        return ok(request, await this.writeOperation("checkout", () => this.checkout(readPayload(request))));
      case "stashes":
        return ok(request, await this.stashes());
      case "stashApply":
        return ok(request, await this.writeOperation("stashApply", () => this.stashApply(readPayload(request))));
      case "stashCreate":
        return ok(request, await this.writeOperation("stashCreate", () => this.stashCreate(readPayload(request))));
      case "stashDrop":
        return ok(request, await this.writeOperation("stashDrop", () => this.stashDrop(readPayload(request))));
      case "remotes":
        return ok(request, await this.remotes());
      case "tags":
        return ok(request, await this.tags());
      case "tagCreate":
        return ok(request, await this.writeOperation("tagCreate", () => this.tagCreate(readPayload(request))));
      case "tagDelete":
        return ok(request, await this.writeOperation("tagDelete", () => this.tagDelete(readPayload(request))));
      case "remoteAdd":
        return ok(request, await this.writeOperation("remoteAdd", () => this.remoteAdd(readPayload(request))));
      case "remoteRemove":
        return ok(request, await this.writeOperation("remoteRemove", () => this.remoteRemove(readPayload(request))));
      case "merge":
        return ok(request, await this.writeOperation("merge", () => this.runGitResult(["merge", readString(readPayload(request).branch, "branch")])));
      case "rebase":
        return ok(request, await this.writeOperation("rebase", () => this.runGitResult(["rebase", readString(readPayload(request).branch, "branch")])));
      default:
        return undefined;
    }
  }

  private async status() {
    const output = await this.git(["status", "--porcelain=v2", "--branch", "--untracked-files=all"]);
    return parsePorcelainV2(output.stdout);
  }

  private async diff(payload: Record<string, unknown>) {
    const file = readOptionalString(payload.path);
    const staged = payload.staged === true;
    const args = ["diff", "--no-ext-diff", "--no-color"];
    if (staged) args.push("--staged");
    if (file != null && file !== "") args.push("--", normalizeRelativePath(file));
    const output = await this.git(args);
    return parseUnifiedDiff(output.stdout, file);
  }

  private async branches() {
    const output = await this.git(["branch", "-a", "--format=%(refname:short)|%(HEAD)|%(objectname:short)|%(upstream:short)|%(committerdate:unix)"]);
    return lines(output.stdout).map((line) => {
      const [name, head, lastCommit, upstream, lastCommitTime] = line.split("|");
      return {
        name: name?.replace(/^remotes\//, "") ?? "",
        current: head === "*",
        remote: name?.startsWith("remotes/") === true || name?.startsWith("origin/") === true,
        upstream: upstream || undefined,
        lastCommit: lastCommit || undefined,
        lastCommitTime: lastCommitTime != null && lastCommitTime !== "" ? Number(lastCommitTime) * 1000 : undefined,
      };
    }).filter((branch) => branch.name !== "");
  }

  private async log(payload: Record<string, unknown>) {
    const count = readOptionalNumber(payload.count) ?? 50;
    const skip = readOptionalNumber(payload.skip) ?? 0;
    const branch = readOptionalString(payload.branch);
    const args = ["log", `-n${count}`, `--skip=${skip}`, "--format=%H%x1f%h%x1f%s%x1f%an%x1f%ae%x1f%at%x1f%P%x1f%D%x1e"];
    if (branch != null && branch !== "") args.push(branch);
    const output = await this.git(args);
    const commits = output.stdout.split("\x1e").map((record) => record.trim()).filter(Boolean).map((record) => {
      const [hash, shortHash, message, author, email, date, parents, refs] = record.split("\x1f");
      return {
        hash,
        shortHash,
        message,
        author,
        email: email || undefined,
        date: Number(date) * 1000,
        parents: parents ? parents.split(" ").filter(Boolean) : [],
        refs: refs ? refs.split(", ").filter(Boolean) : [],
      };
    });
    return { commits, total: commits.length, hasMore: commits.length >= count };
  }

  private async commitDetails(payload: Record<string, unknown>) {
    const hash = readString(payload.hash, "hash");
    const output = await this.git(["show", "--no-ext-diff", "--no-color", "--format=fuller", "--stat", hash]);
    return { hash, text: output.stdout };
  }

  private async stage(payload: Record<string, unknown>) {
    const files = readFiles(payload.files);
    return this.runGitResult(["add", "--", ...files]);
  }

  private async unstage(payload: Record<string, unknown>) {
    const files = readFiles(payload.files);
    return this.runGitResult(["reset", "HEAD", "--", ...files]);
  }

  private async discard(payload: Record<string, unknown>) {
    const files = readFiles(payload.files);
    this.options.confirmationStore.require({
      action: "git.discard",
      target: files.join(", "),
      risk: "Discard local file changes",
      payload,
      confirmation: readConfirmation(payload.confirmation),
    });
    return this.runGitResult(["checkout", "--", ...files]);
  }

  private async commit(payload: Record<string, unknown>) {
    const message = readString(payload.message, "message");
    if (message.trim() === "") throw new CapabilityError("invalid_payload", "Commit message must not be blank");
    const args = ["commit", "-m", message];
    if (payload.amend === true) args.push("--amend");
    return this.runGitResult(args);
  }

  private async fetch(payload: Record<string, unknown>) {
    const args = ["fetch"];
    if (payload.prune === true) args.push("--prune");
    if (payload.all === true) args.push("--all");
    else args.push(readOptionalString(payload.remote) ?? "origin");
    return this.runGitResult(args);
  }

  private async pull(payload: Record<string, unknown>) {
    const args = ["pull"];
    if (payload.rebase === true) args.push("--rebase");
    args.push(readOptionalString(payload.remote) ?? "origin");
    const branch = readOptionalString(payload.branch);
    if (branch != null && branch !== "") args.push(branch);
    return this.runGitResult(args);
  }

  private async push(payload: Record<string, unknown>) {
    const force = payload.force === true;
    if (force) {
      this.options.confirmationStore.require({
        action: "git.push.force",
        target: readOptionalString(payload.branch) ?? "current branch",
        risk: "Force push can overwrite remote history",
        payload,
        confirmation: readConfirmation(payload.confirmation),
      });
    }
    const args = ["push"];
    if (force) args.push("--force-with-lease");
    if (payload.setUpstream === true) args.push("--set-upstream");
    args.push(readOptionalString(payload.remote) ?? "origin");
    const branch = readOptionalString(payload.branch);
    if (branch != null && branch !== "") args.push(branch);
    return this.runGitResult(args);
  }

  private async checkout(payload: Record<string, unknown>) {
    const ref = readString(payload.ref, "ref");
    const force = payload.force === true;
    if (force) {
      this.options.confirmationStore.require({
        action: "git.checkout.force",
        target: ref,
        risk: "Force checkout can discard local changes",
        payload,
        confirmation: readConfirmation(payload.confirmation),
      });
    }
    const args = ["checkout"];
    if (payload.create === true) args.push("-b");
    if (force) args.push("--force");
    args.push(ref);
    return this.runGitResult(args);
  }

  private async stashes() {
    const output = await this.git(["stash", "list", "--format=%gd%x1f%gs%x1f%cr"]);
    return lines(output.stdout).map((line) => {
      const [ref, message, date] = line.split("\x1f");
      const index = Number(ref?.replace("stash@{", "").replace("}", ""));
      return { index, message: message ?? "", dateLabel: date };
    }).filter((stash) => Number.isFinite(stash.index));
  }

  private async stashApply(payload: Record<string, unknown>) {
    const index = readNumber(payload.index, "index");
    return this.runGitResult([payload.pop === true ? "stash" : "stash", payload.pop === true ? "pop" : "apply", `stash@{${index}}`]);
  }

  private async stashCreate(payload: Record<string, unknown>) {
    const message = readOptionalString(payload.message);
    const args = ["stash", "push"];
    if (message != null && message !== "") args.push("-m", message);
    return this.runGitResult(args);
  }

  private async stashDrop(payload: Record<string, unknown>) {
    const index = readNumber(payload.index, "index");
    this.options.confirmationStore.require({
      action: "git.stash.drop",
      target: `stash@{${index}}`,
      risk: "Drop a saved stash",
      payload,
      confirmation: readConfirmation(payload.confirmation),
    });
    return this.runGitResult(["stash", "drop", `stash@{${index}}`]);
  }

  private async remotes() {
    const output = await this.git(["remote", "-v"]);
    const remotes = new Map<string, { fetchUrl?: string; pushUrl?: string }>();
    for (const line of lines(output.stdout)) {
      const [name, url, type] = line.trim().split(/\s+/);
      if (name == null || url == null) continue;
      const entry = remotes.get(name) ?? {};
      if (type?.includes("fetch")) entry.fetchUrl = url;
      if (type?.includes("push")) entry.pushUrl = url;
      remotes.set(name, entry);
    }
    return [...remotes.entries()].map(([name, urls]) => ({
      name,
      url: urls.fetchUrl ?? urls.pushUrl ?? "",
      fetchUrl: urls.fetchUrl,
      pushUrl: urls.pushUrl,
    }));
  }

  private async tags() {
    const output = await this.git(["tag", "--list"]);
    return lines(output.stdout);
  }

  private async tagCreate(payload: Record<string, unknown>) {
    const name = readString(payload.name, "name");
    const message = readOptionalString(payload.message);
    const args = ["tag"];
    if (message != null && message !== "") args.push("-a", name, "-m", message);
    else args.push(name);
    return this.runGitResult(args);
  }

  private async tagDelete(payload: Record<string, unknown>) {
    const name = readString(payload.name, "name");
    this.options.confirmationStore.require({
      action: "git.tag.delete",
      target: name,
      risk: "Delete a git tag",
      payload,
      confirmation: readConfirmation(payload.confirmation),
    });
    return this.runGitResult(["tag", "-d", name]);
  }

  private async remoteAdd(payload: Record<string, unknown>) {
    return this.runGitResult(["remote", "add", readString(payload.name, "name"), readString(payload.url, "url")]);
  }

  private async remoteRemove(payload: Record<string, unknown>) {
    const name = readString(payload.name, "name");
    this.options.confirmationStore.require({
      action: "git.remote.remove",
      target: name,
      risk: "Remove a git remote",
      payload,
      confirmation: readConfirmation(payload.confirmation),
    });
    return this.runGitResult(["remote", "remove", name]);
  }

  private async writeOperation<T>(name: string, operation: () => Promise<T>): Promise<T> {
    const next = this.writeQueue.then(operation, operation);
    this.writeQueue = next.catch(() => undefined);
    const result = await next;
    this.emit("git.operationCompleted", { operation: name });
    this.emit("git.statusChanged", await this.status().catch(() => null));
    return result;
  }

  private async runGitResult(args: readonly string[]) {
    const output = await this.git(args);
    return {
      success: true,
      message: [output.stdout.trim(), output.stderr.trim()].filter(Boolean).join("\n"),
    };
  }

  private git(args: readonly string[]) {
    for (const arg of args) {
      if (arg.includes("\0")) throw new CapabilityError("invalid_payload", "Git arguments must not contain NUL bytes");
    }
    return runCommand("git", args, { cwd: this.sandbox.root, timeoutMillis: 60_000 });
  }

  private emit(event: string, payload: unknown): void {
    this.options.eventSink?.(this.options.eventSequencer.create({ ns: "git", event, payload }));
  }
}

function parsePorcelainV2(output: string) {
  let branch = "unknown";
  let upstream: string | undefined;
  let ahead = 0;
  let behind = 0;
  const staged = [];
  const unstaged = [];
  const untracked = [];
  const conflicted = [];

  for (const line of lines(output)) {
    if (line.startsWith("# branch.head ")) branch = line.substring("# branch.head ".length).trim();
    else if (line.startsWith("# branch.upstream ")) upstream = line.substring("# branch.upstream ".length).trim();
    else if (line.startsWith("# branch.ab ")) {
      const match = /\+(\d+)\s+-(\d+)/.exec(line);
      ahead = Number(match?.[1] ?? 0);
      behind = Number(match?.[2] ?? 0);
    } else if (line.startsWith("? ")) {
      untracked.push(line.substring(2));
    } else if (line.startsWith("u ")) {
      const path = line.split(" ").slice(10).join(" ");
      if (path) conflicted.push(path);
    } else if (line.startsWith("1 ") || line.startsWith("2 ")) {
      const parts = line.split(" ");
      const status = parts[1] ?? "  ";
      const pathValue = line.startsWith("2 ") ? parts.slice(9).join(" ").split("\t").at(0) : parts.slice(8).join(" ");
      const oldPath = line.startsWith("2 ") ? parts.slice(9).join(" ").split("\t").at(1) : undefined;
      if (pathValue == null || pathValue === "") continue;
      if (status[0] !== "." && status[0] !== " ") staged.push({ path: pathValue, oldPath, status: mapStatus(status[0]) });
      if (status[1] !== "." && status[1] !== " ") unstaged.push({ path: pathValue, oldPath, status: mapStatus(status[1]) });
    }
  }

  return {
    branch: branch === "(detached)" ? "detached" : branch,
    upstream,
    ahead,
    behind,
    staged,
    unstaged,
    untracked,
    conflicted,
    stashes: 0,
    clean: staged.length === 0 && unstaged.length === 0 && untracked.length === 0 && conflicted.length === 0,
  };
}

function parseUnifiedDiff(output: string, requestedPath?: string) {
  const files = [];
  let current: {
    path: string;
    oldPath?: string;
    status: string;
    additions: number;
    deletions: number;
    hunks: Array<{ oldStart: number; oldLines: number; newStart: number; newLines: number; header: string; lines: unknown[] }>;
  } | undefined;
  let currentHunk: { oldStart: number; oldLines: number; newStart: number; newLines: number; header: string; lines: unknown[] } | undefined;
  let oldLine = 0;
  let newLine = 0;

  for (const line of output.split(/\r?\n/)) {
    if (line.startsWith("diff --git ")) {
      if (current != null) files.push(current);
      current = { path: requestedPath ?? "", status: "modified", additions: 0, deletions: 0, hunks: [] };
      currentHunk = undefined;
      continue;
    }
    if (current == null) continue;
    if (line.startsWith("--- ")) {
      current.oldPath = normalizeDiffPath(line.substring(4));
    } else if (line.startsWith("+++ ")) {
      current.path = normalizeDiffPath(line.substring(4));
      if (current.oldPath === "/dev/null") current.status = "added";
      if (current.path === "/dev/null") current.status = "deleted";
    } else if (line.startsWith("@@")) {
      const match = /^@@ -(\d+),?(\d*) \+(\d+),?(\d*) @@(.*)$/.exec(line);
      oldLine = Number(match?.[1] ?? 0);
      newLine = Number(match?.[3] ?? 0);
      currentHunk = {
        oldStart: oldLine,
        oldLines: Number(match?.[2] || 1),
        newStart: newLine,
        newLines: Number(match?.[4] || 1),
        header: line,
        lines: [],
      };
      current.hunks.push(currentHunk);
    } else if (currentHunk != null) {
      if (line.startsWith("+") && !line.startsWith("+++")) {
        current.additions += 1;
        currentHunk.lines.push({ type: "addition", content: line.substring(1), newLineNumber: newLine++ });
      } else if (line.startsWith("-") && !line.startsWith("---")) {
        current.deletions += 1;
        currentHunk.lines.push({ type: "deletion", content: line.substring(1), oldLineNumber: oldLine++ });
      } else {
        const content = line.startsWith(" ") ? line.substring(1) : line;
        currentHunk.lines.push({ type: "context", content, oldLineNumber: oldLine++, newLineNumber: newLine++ });
      }
    }
  }
  if (current != null) files.push(current);
  return {
    files: files.filter((file) => file.path !== "/dev/null"),
    additions: files.reduce((sum, file) => sum + file.additions, 0),
    deletions: files.reduce((sum, file) => sum + file.deletions, 0),
    binary: output.includes("Binary files"),
  };
}

function normalizeDiffPath(value: string): string {
  return value.replace(/^a\//, "").replace(/^b\//, "").trim();
}

function mapStatus(status: string) {
  switch (status) {
    case "A": return "added";
    case "D": return "deleted";
    case "R": return "renamed";
    case "C": return "copied";
    case "U": return "unmerged";
    case "M": return "modified";
    default: return "unknown";
  }
}

function ok(request: MoccaRequest, payload: unknown): MoccaResponse {
  return createResponse(request, { ok: true, payload });
}

function readPayload(request: MoccaRequest): Record<string, unknown> {
  if (request.payload == null) return {};
  if (typeof request.payload !== "object" || Array.isArray(request.payload)) {
    throw new CapabilityError("invalid_payload", "Expected request payload object");
  }
  return request.payload as Record<string, unknown>;
}

function readFiles(value: unknown): string[] {
  if (!Array.isArray(value) || value.some((entry) => typeof entry !== "string")) {
    throw new CapabilityError("invalid_payload", "files must be a string array");
  }
  return value.map((entry) => normalizeRelativePath(entry));
}

function readString(value: unknown, field: string): string {
  if (typeof value !== "string") throw new CapabilityError("invalid_payload", `${field} must be a string`);
  return value;
}

function readNumber(value: unknown, field: string): number {
  if (typeof value !== "number" || !Number.isFinite(value)) throw new CapabilityError("invalid_payload", `${field} must be a number`);
  return value;
}

function readOptionalString(value: unknown): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function readOptionalNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function readConfirmation(value: unknown): ConfirmationInput | undefined {
  if (typeof value !== "object" || value == null || Array.isArray(value)) return undefined;
  const operationId = (value as Record<string, unknown>).operationId;
  return typeof operationId === "string" ? { operationId } : undefined;
}

function lines(value: string): string[] {
  return value.split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
}
