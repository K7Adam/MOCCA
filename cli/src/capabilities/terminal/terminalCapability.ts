import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { createResponse, type MoccaEvent, type MoccaRequest, type MoccaResponse } from "../../protocol/message.js";
import { ConfirmationStore, type ConfirmationInput } from "../common/confirmation.js";
import { CapabilityError } from "../common/errors.js";
import { EventSequencer } from "../common/eventSequencer.js";
import { RustPtySidecar } from "./rustPtySidecar.js";

const DEFAULT_COLS = 120;
const DEFAULT_ROWS = 40;
const FRAME_INTERVAL_MS = 42;
const DEFAULT_SCROLLBACK = 1_000;

export type TerminalCapabilities = {
  ptyGrid: boolean;
  dirtyRows: boolean;
  sidecar: "rust" | "node-fallback";
};

export type TerminalCapabilityOptions = {
  projectDir: string;
  confirmationStore: ConfirmationStore;
  eventSequencer: EventSequencer;
  eventSink?: (event: MoccaEvent) => void;
};

type TerminalCell = {
  char: string;
  fg?: string;
  bg?: string;
  attrs?: string[];
};

type TerminalSession = {
  id: string;
  title: string;
  shell: string;
  cols: number;
  rows: number;
  scrollback: number;
  child: ChildProcessWithoutNullStreams;
  lines: string[];
  dirtyRows: Set<number>;
  frameTimer?: NodeJS.Timeout;
  exited: boolean;
};

export class TerminalCapability {
  readonly capabilities: TerminalCapabilities;
  private rustSidecar?: RustPtySidecar;
  private readonly rustSidecarAvailable: boolean;
  private readonly sessions = new Map<string, TerminalSession>();

  constructor(private readonly options: TerminalCapabilityOptions) {
    this.rustSidecarAvailable = RustPtySidecar.isAvailable();
    this.capabilities = {
      ptyGrid: true,
      dirtyRows: true,
      sidecar: this.rustSidecarAvailable ? "rust" : "node-fallback",
    };
  }

  async handle(request: MoccaRequest): Promise<MoccaResponse | undefined> {
    if (request.ns !== "terminal") return undefined;
    switch (request.action) {
      case "list":
        return ok(request, await this.list());
      case "spawn":
        return ok(request, await this.spawn(readPayload(request)));
      case "write":
        return ok(request, await this.write(readPayload(request)));
      case "resize":
        return ok(request, await this.resize(readPayload(request)));
      case "scroll":
        return ok(request, await this.snapshot(readString(readPayload(request).terminalId, "terminalId")));
      case "snapshot":
        return ok(request, await this.snapshot(readString(readPayload(request).terminalId, "terminalId")));
      case "kill":
        return ok(request, await this.kill(readPayload(request)));
      default:
        return undefined;
    }
  }

  async close(): Promise<void> {
    await this.rustSidecar?.close();
    await Promise.all([...this.sessions.values()].map((session) => this.stopSession(session)));
    this.sessions.clear();
  }

  private list() {
    if (this.rustSidecarAvailable) return this.rustSidecar?.list() ?? [];
    return [...this.sessions.values()].map((session) => ({
      id: session.id,
      shell: session.shell,
      title: session.title,
      cols: session.cols,
      rows: session.rows,
      exited: session.exited,
    }));
  }

  private spawn(payload: Record<string, unknown>) {
    const rustSidecar = this.sidecar();
    if (rustSidecar != null) return rustSidecar.spawn(payload);
    const id = randomUUID();
    const shell = readOptionalString(payload.shell) ?? defaultShell();
    const cols = readOptionalNumber(payload.cols) ?? DEFAULT_COLS;
    const rows = readOptionalNumber(payload.rows) ?? DEFAULT_ROWS;
    const scrollback = readOptionalNumber(payload.scrollback) ?? DEFAULT_SCROLLBACK;
    const child = spawn(shell, [], {
      cwd: this.options.projectDir,
      shell: false,
      windowsHide: true,
      env: {
        ...process.env,
        TERM: process.env.TERM ?? "xterm-256color",
        COLORTERM: process.env.COLORTERM ?? "truecolor",
        COLUMNS: String(cols),
        LINES: String(rows),
      },
    });
    const session: TerminalSession = {
      id,
      title: path.basename(shell),
      shell,
      cols,
      rows,
      scrollback,
      child,
      lines: [""],
      dirtyRows: new Set(Array.from({ length: rows }, (_, index) => index)),
      exited: false,
    };
    child.stdout.setEncoding("utf8");
    child.stderr.setEncoding("utf8");
    child.stdout.on("data", (chunk: string) => this.append(session, chunk));
    child.stderr.on("data", (chunk: string) => this.append(session, chunk));
    child.on("exit", (exitCode) => {
      session.exited = true;
      this.emit("terminal.exited", { terminalId: id, exitCode });
    });
    child.on("error", (error) => {
      this.emit("terminal.error", { terminalId: id, message: error.message });
    });
    this.sessions.set(id, session);
    this.emit("terminal.spawned", { terminalId: id, shell, cols, rows, title: session.title });
    this.scheduleFrame(session);
    return { id, shell, title: session.title, cols, rows };
  }

  private write(payload: Record<string, unknown>) {
    const rustSidecar = this.sidecar();
    if (rustSidecar != null) return rustSidecar.write(payload);
    const session = this.requireSession(readString(payload.terminalId, "terminalId"));
    const data = readString(payload.data, "data");
    session.child.stdin.write(data);
    return { success: true, terminalId: session.id };
  }

  private resize(payload: Record<string, unknown>) {
    const rustSidecar = this.sidecar();
    if (rustSidecar != null) return rustSidecar.resize(payload);
    const session = this.requireSession(readString(payload.terminalId, "terminalId"));
    session.cols = readOptionalNumber(payload.cols) ?? session.cols;
    session.rows = readOptionalNumber(payload.rows) ?? session.rows;
    session.dirtyRows = new Set(Array.from({ length: session.rows }, (_, index) => index));
    this.scheduleFrame(session);
    return { success: true, terminalId: session.id, cols: session.cols, rows: session.rows };
  }

  private snapshot(terminalId: string) {
    const rustSidecar = this.sidecar();
    if (rustSidecar != null) return rustSidecar.snapshot(terminalId);
    const session = this.requireSession(terminalId);
    return this.frame(session, true);
  }

  private kill(payload: Record<string, unknown>) {
    const rustSidecar = this.sidecar();
    if (rustSidecar != null) {
      const terminalId = readString(payload.terminalId, "terminalId");
      if (!rustSidecar.isExited(terminalId)) {
        this.options.confirmationStore.require({
          action: "terminal.kill",
          target: terminalId,
          risk: "Terminate a running terminal session",
          payload,
          confirmation: readConfirmation(payload.confirmation),
        });
      }
      return rustSidecar.kill(terminalId);
    }
    const session = this.requireSession(readString(payload.terminalId, "terminalId"));
    if (!session.exited) {
      this.options.confirmationStore.require({
        action: "terminal.kill",
        target: session.id,
        risk: "Terminate a running terminal session",
        payload,
        confirmation: readConfirmation(payload.confirmation),
      });
    }
    session.child.kill();
    session.exited = true;
    this.sessions.delete(session.id);
    this.emit("terminal.exited", { terminalId: session.id, killed: true });
    return { success: true, terminalId: session.id };
  }

  private stopSession(session: TerminalSession): Promise<void> {
    if (session.frameTimer != null) clearTimeout(session.frameTimer);
    return new Promise((resolve) => {
      if (session.exited) {
        resolve();
        return;
      }
      const timeout = setTimeout(resolve, 500);
      session.child.once("exit", () => {
        clearTimeout(timeout);
        resolve();
      });
      session.child.kill();
    });
  }

  private sidecar(): RustPtySidecar | undefined {
    if (!this.rustSidecarAvailable) return undefined;
    this.rustSidecar ??= RustPtySidecar.createIfAvailable(
      this.options.projectDir,
      (event, payload) => this.emit(event, payload),
    );
    return this.rustSidecar;
  }

  private append(session: TerminalSession, chunk: string): void {
    for (const char of chunk.replace(/\x1b\[[0-9;?]*[ -/]*[@-~]/g, "")) {
      if (char === "\r") {
        session.lines[session.lines.length - 1] = "";
      } else if (char === "\n") {
        session.lines.push("");
      } else if (char === "\b") {
        const current = session.lines[session.lines.length - 1] ?? "";
        session.lines[session.lines.length - 1] = current.slice(0, -1);
      } else if (char >= " ") {
        session.lines[session.lines.length - 1] = (session.lines[session.lines.length - 1] ?? "") + char;
      }
    }
    if (session.lines.length > session.scrollback) {
      session.lines = session.lines.slice(-session.scrollback);
    }
    const start = Math.max(0, session.rows - Math.min(session.rows, chunk.split(/\r?\n/).length + 1));
    for (let row = start; row < session.rows; row += 1) session.dirtyRows.add(row);
    this.scheduleFrame(session);
  }

  private scheduleFrame(session: TerminalSession): void {
    if (session.frameTimer != null) return;
    session.frameTimer = setTimeout(() => {
      session.frameTimer = undefined;
      if (session.dirtyRows.size > 0) {
        this.emit("terminal.state", this.frame(session, false));
        session.dirtyRows.clear();
      }
    }, FRAME_INTERVAL_MS);
  }

  private frame(session: TerminalSession, fullFrame: boolean) {
    const visibleLines = session.lines.slice(-session.rows);
    while (visibleLines.length < session.rows) visibleLines.unshift("");
    const rows = fullFrame ? Array.from({ length: session.rows }, (_, index) => index) : [...session.dirtyRows];
    const cells: Record<string, TerminalCell[]> = {};
    for (const row of rows) {
      const line = visibleLines[row] ?? "";
      cells[String(row)] = Array.from(line.slice(0, session.cols)).map((char) => ({ char }));
    }
    const cursorY = Math.min(session.rows - 1, visibleLines.length - 1);
    const cursorX = Math.min(session.cols - 1, (visibleLines.at(-1) ?? "").length);
    return {
      terminalId: session.id,
      fullFrame,
      cols: session.cols,
      rows: session.rows,
      cells,
      cursorX,
      cursorY,
      cursorVisible: true,
      cursorStyle: "block",
      appCursorKeys: false,
      bracketedPaste: false,
      mouseMode: null,
      mouseEncoding: null,
      reverseVideo: false,
      title: session.title,
      scrollbackLength: session.lines.length,
    };
  }

  private requireSession(terminalId: string): TerminalSession {
    const session = this.sessions.get(terminalId);
    if (session == null) throw new CapabilityError("terminal_not_found", "Terminal session was not found", { terminalId });
    return session;
  }

  private emit(event: string, payload: unknown): void {
    this.options.eventSink?.(this.options.eventSequencer.create({ ns: "terminal", event, payload }));
  }
}

function defaultShell(): string {
  if (process.platform === "win32") return process.env.COMSPEC ?? "powershell.exe";
  return process.env.SHELL ?? "/bin/sh";
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
  if (typeof value !== "string") throw new CapabilityError("invalid_payload", `${field} must be a string`);
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
