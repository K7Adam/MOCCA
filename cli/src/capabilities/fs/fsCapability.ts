import { promises as fs } from "node:fs";
import path from "node:path";
import fg from "fast-glob";
import ignore from "ignore";
import chokidar, { type FSWatcher } from "chokidar";
import { type MoccaEvent, type MoccaRequest, type MoccaResponse, createResponse } from "../../protocol/message.js";
import { ConfirmationStore, type ConfirmationInput } from "../common/confirmation.js";
import { CapabilityError } from "../common/errors.js";
import { EventSequencer } from "../common/eventSequencer.js";
import { normalizeSlashes, PathSandbox, fileExists } from "../common/pathSandbox.js";

const MAX_TEXT_READ_BYTES = 2 * 1024 * 1024;
const SEARCH_FILE_LIMIT_BYTES = 1024 * 1024;

export type FileSystemCapabilities = {
  native: boolean;
  watch: boolean;
  chunkedRead: boolean;
  binaryMetadata: boolean;
};

export type FsCapabilityOptions = {
  projectDir: string;
  confirmationStore: ConfirmationStore;
  eventSequencer: EventSequencer;
  eventSink?: (event: MoccaEvent) => void;
};

export class FileSystemCapability {
  readonly capabilities: FileSystemCapabilities = {
    native: true,
    watch: true,
    chunkedRead: true,
    binaryMetadata: true,
  };

  private readonly sandbox: PathSandbox;
  private readonly watchers = new Map<string, FSWatcher>();
  private readonly ignored = ignore();

  constructor(private readonly options: FsCapabilityOptions) {
    this.sandbox = new PathSandbox(options.projectDir);
    this.ignored.add([".git", "node_modules", "dist", "build", ".gradle", ".idea"]);
  }

  async loadIgnoreFile(): Promise<void> {
    const gitIgnorePath = path.join(this.sandbox.root, ".gitignore");
    try {
      const contents = await fs.readFile(gitIgnorePath, "utf8");
      this.ignored.add(contents);
    } catch {
      // No project .gitignore is a valid state.
    }
  }

  async handle(request: MoccaRequest): Promise<MoccaResponse | undefined> {
    if (request.ns !== "fs") return undefined;

    switch (request.action) {
      case "list":
        return ok(request, await this.list(readOptionalString(readPayload(request).path) ?? ""));
      case "stat":
        return ok(request, await this.stat(readOptionalString(readPayload(request).path) ?? ""));
      case "read":
        return ok(request, await this.read(readPayload(request)));
      case "write":
        return ok(request, await this.write(readPayload(request)));
      case "create":
        return ok(request, await this.create(readPayload(request)));
      case "mkdir":
        return ok(request, await this.mkdir(readPayload(request)));
      case "move":
        return ok(request, await this.move(readPayload(request)));
      case "delete":
        return ok(request, await this.delete(readPayload(request)));
      case "search":
        return ok(request, await this.search(readPayload(request)));
      case "find":
        return ok(request, await this.find(readPayload(request)));
      case "watch.open":
        return ok(request, await this.openWatch(readPayload(request)));
      case "watch.close":
        return ok(request, await this.closeWatch(readPayload(request)));
      default:
        return undefined;
    }
  }

  async close(): Promise<void> {
    await Promise.all([...this.watchers.values()].map((watcher) => watcher.close()));
    this.watchers.clear();
  }

  private async list(inputPath = "") {
    const target = this.sandbox.resolveExisting(inputPath);
    const entries = await fs.readdir(target.absolutePath, { withFileTypes: true });
    const files = await Promise.all(entries
      .filter((entry) => !this.isIgnored(path.posix.join(target.relativePath, entry.name)))
      .map(async (entry) => {
        const absolutePath = path.join(target.absolutePath, entry.name);
        const stat = await fs.stat(absolutePath);
        return {
          name: entry.name,
          path: this.sandbox.toPublicPath(absolutePath),
          type: entry.isDirectory() ? "directory" : "file",
          size: entry.isDirectory() ? undefined : stat.size,
          updated: stat.mtimeMs,
          modifiedAt: stat.mtimeMs,
        };
      }));
    files.sort((a, b) => Number(b.type === "directory") - Number(a.type === "directory") || a.name.localeCompare(b.name));
    return files;
  }

  private async stat(inputPath = "") {
    const target = this.sandbox.resolveExisting(inputPath);
    const stat = await fs.stat(target.absolutePath);
    return {
      name: path.basename(target.absolutePath),
      path: target.relativePath,
      type: stat.isDirectory() ? "directory" : "file",
      size: stat.isDirectory() ? undefined : stat.size,
      updated: stat.mtimeMs,
      modifiedAt: stat.mtimeMs,
      binary: stat.isFile() ? await isBinaryFile(target.absolutePath) : false,
    };
  }

  private async read(payload: Record<string, unknown>) {
    const target = this.sandbox.resolveExisting(readString(payload.path, "path"));
    const stat = await fs.stat(target.absolutePath);
    if (!stat.isFile()) {
      throw new CapabilityError("not_a_file", "Path is not a file", { path: target.relativePath });
    }
    const offset = readOptionalNumber(payload.offset) ?? 0;
    const length = readOptionalNumber(payload.length);
    const encoding = readOptionalString(payload.encoding) ?? "utf8";
    const binary = await isBinaryFile(target.absolutePath);
    const requestedLength = length ?? Math.min(stat.size - offset, MAX_TEXT_READ_BYTES);
    const handle = await fs.open(target.absolutePath, "r");
    try {
      const buffer = Buffer.alloc(Math.max(0, Math.min(requestedLength, stat.size - offset)));
      await handle.read(buffer, 0, buffer.length, offset);
      if (binary && encoding !== "base64") {
        return {
          path: target.relativePath,
          content: "",
          binary: true,
          size: stat.size,
          truncated: stat.size > buffer.length,
        };
      }
      return {
        path: target.relativePath,
        content: encoding === "base64" ? buffer.toString("base64") : buffer.toString("utf8"),
        binary,
        size: stat.size,
        offset,
        length: buffer.length,
        truncated: offset + buffer.length < stat.size,
        language: languageForPath(target.relativePath),
      };
    } finally {
      await handle.close();
    }
  }

  private async write(payload: Record<string, unknown>) {
    const target = await this.sandbox.resolveForWrite(readString(payload.path, "path"));
    const content = readString(payload.content, "content");
    await fs.mkdir(path.dirname(target.absolutePath), { recursive: true });
    await fs.writeFile(target.absolutePath, content, "utf8");
    this.emit("fs.changed", { path: target.relativePath, kind: "write" });
    return { success: true, path: target.relativePath };
  }

  private async create(payload: Record<string, unknown>) {
    const target = await this.sandbox.resolveForWrite(readString(payload.path, "path"));
    if (fileExists(target.absolutePath)) {
      throw new CapabilityError("file_exists", "File already exists", { path: target.relativePath });
    }
    await fs.mkdir(path.dirname(target.absolutePath), { recursive: true });
    await fs.writeFile(target.absolutePath, readOptionalString(payload.content) ?? "", "utf8");
    this.emit("fs.changed", { path: target.relativePath, kind: "create" });
    return { success: true, path: target.relativePath };
  }

  private async mkdir(payload: Record<string, unknown>) {
    const target = await this.sandbox.resolveForWrite(readString(payload.path, "path"));
    await fs.mkdir(target.absolutePath, { recursive: payload.recursive !== false });
    this.emit("fs.changed", { path: target.relativePath, kind: "mkdir" });
    return { success: true, path: target.relativePath };
  }

  private async move(payload: Record<string, unknown>) {
    const from = this.sandbox.resolveExisting(readString(payload.from, "from"));
    const to = await this.sandbox.resolveForWrite(readString(payload.to, "to"));
    const overwrite = payload.overwrite === true;
    if (fileExists(to.absolutePath) && !overwrite) {
      throw new CapabilityError("file_exists", "Destination already exists", { path: to.relativePath });
    }
    if (fileExists(to.absolutePath) && overwrite) {
      this.options.confirmationStore.require({
        action: "fs.move",
        target: `${from.relativePath} -> ${to.relativePath}`,
        risk: "Overwrite existing file or directory",
        payload,
        confirmation: readConfirmation(payload.confirmation),
      });
    }
    await fs.mkdir(path.dirname(to.absolutePath), { recursive: true });
    await fs.rename(from.absolutePath, to.absolutePath);
    this.emit("fs.renamed", { from: from.relativePath, to: to.relativePath });
    return { success: true, from: from.relativePath, to: to.relativePath };
  }

  private async delete(payload: Record<string, unknown>) {
    const target = this.sandbox.resolveExisting(readString(payload.path, "path"));
    this.options.confirmationStore.require({
      action: "fs.delete",
      target: target.relativePath,
      risk: "Delete file-system content from the project",
      payload,
      confirmation: readConfirmation(payload.confirmation),
    });
    await fs.rm(target.absolutePath, { recursive: payload.recursive === true, force: false });
    this.emit("fs.deleted", { path: target.relativePath });
    return { success: true, path: target.relativePath };
  }

  private async search(payload: Record<string, unknown>) {
    const query = readString(payload.query, "query");
    const rootPath = readOptionalString(payload.path) ?? "";
    const maxResults = readOptionalNumber(payload.maxResults) ?? 100;
    const results = [];
    for (const relativePath of await this.findFilesForSearch(rootPath)) {
      const absolutePath = path.join(this.sandbox.root, relativePath);
      const stat = await fs.stat(absolutePath);
      if (!stat.isFile() || stat.size > SEARCH_FILE_LIMIT_BYTES || await isBinaryFile(absolutePath)) continue;
      const contents = await fs.readFile(absolutePath, "utf8");
      const lines = contents.split(/\r?\n/);
      for (let i = 0; i < lines.length; i += 1) {
        const column = lines[i].toLowerCase().indexOf(query.toLowerCase());
        if (column >= 0) {
          results.push({
            file: normalizeSlashes(relativePath),
            path: normalizeSlashes(relativePath),
            line: i + 1,
            column: column + 1,
            match: lines[i].trim(),
            context: lines.slice(Math.max(0, i - 2), Math.min(lines.length, i + 3)).join("\n"),
          });
          if (results.length >= maxResults) return results;
        }
      }
    }
    return results;
  }

  private async find(payload: Record<string, unknown>) {
    const pattern = readString(payload.pattern ?? payload.query, "pattern");
    const rootPath = readOptionalString(payload.path) ?? "";
    return (await this.findFilesForSearch(rootPath, pattern)).map(normalizeSlashes);
  }

  private async openWatch(payload: Record<string, unknown>) {
    const target = this.sandbox.resolveExisting(readOptionalString(payload.path) ?? "");
    const id = target.relativePath || ".";
    if (this.watchers.has(id)) return { watchId: id, path: target.relativePath };
    const watcher = chokidar.watch(target.absolutePath, {
      ignoreInitial: true,
      ignored: (candidate) => this.isIgnored(this.sandbox.toPublicPath(path.resolve(candidate.toString()))),
    });
    watcher
      .on("add", (candidate) => this.emit("fs.changed", { path: this.sandbox.toPublicPath(path.resolve(candidate)), kind: "add" }))
      .on("change", (candidate) => this.emit("fs.changed", { path: this.sandbox.toPublicPath(path.resolve(candidate)), kind: "change" }))
      .on("unlink", (candidate) => this.emit("fs.deleted", { path: this.sandbox.toPublicPath(path.resolve(candidate)) }))
      .on("unlinkDir", (candidate) => this.emit("fs.deleted", { path: this.sandbox.toPublicPath(path.resolve(candidate)) }));
    this.watchers.set(id, watcher);
    return { watchId: id, path: target.relativePath };
  }

  private async closeWatch(payload: Record<string, unknown>) {
    const watchId = readOptionalString(payload.watchId) ?? readOptionalString(payload.path) ?? ".";
    const watcher = this.watchers.get(watchId);
    if (watcher != null) {
      await watcher.close();
      this.watchers.delete(watchId);
    }
    return { success: true, watchId };
  }

  private async findFilesForSearch(rootPath: string, pattern = "**/*"): Promise<string[]> {
    const root = this.sandbox.resolveExisting(rootPath);
    const entries = await fg(pattern, {
      cwd: root.absolutePath,
      dot: true,
      onlyFiles: true,
      unique: true,
      followSymbolicLinks: false,
      ignore: [".git/**", "node_modules/**", "dist/**", "build/**", ".gradle/**"],
    });
    return entries
      .map((entry) => normalizeSlashes(path.posix.join(root.relativePath, entry)))
      .filter((entry) => !this.isIgnored(entry));
  }

  private isIgnored(relativePath: string): boolean {
    if (relativePath === "") return false;
    return this.ignored.ignores(normalizeSlashes(relativePath));
  }

  private emit(event: string, payload: unknown): void {
    this.options.eventSink?.(this.options.eventSequencer.create({ ns: "fs", event, payload }));
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

function readString(value: unknown, field: string): string {
  if (typeof value !== "string") {
    throw new CapabilityError("invalid_payload", `${field} must be a string`);
  }
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

async function isBinaryFile(absolutePath: string): Promise<boolean> {
  const handle = await fs.open(absolutePath, "r");
  try {
    const buffer = Buffer.alloc(512);
    const { bytesRead } = await handle.read(buffer, 0, buffer.length, 0);
    for (let i = 0; i < bytesRead; i += 1) {
      if (buffer[i] === 0) return true;
    }
    return false;
  } finally {
    await handle.close();
  }
}

function languageForPath(relativePath: string): string {
  const ext = relativePath.substring(relativePath.lastIndexOf(".") + 1).toLowerCase();
  return ext === relativePath ? "plaintext" : ext;
}
