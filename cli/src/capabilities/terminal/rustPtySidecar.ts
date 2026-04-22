import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import { existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { CapabilityError } from "../common/errors.js";

const DEFAULT_WAIT_MS = 2_500;

export type TerminalSidecarEventSink = (event: string, payload: unknown) => void;

export type TerminalSidecarSession = {
  id: string;
  shell: string;
  title: string;
  cols: number;
  rows: number;
  exited: boolean;
};

type SidecarFrame = Record<string, unknown> & {
  event?: string;
  terminalId?: string;
  message?: string;
};

type Waiter = {
  predicate: (frame: SidecarFrame) => boolean;
  resolve: (frame: SidecarFrame) => void;
  reject: (error: Error) => void;
  timeout: NodeJS.Timeout;
};

export class RustPtySidecar {
  private readonly child: ChildProcessWithoutNullStreams;
  private readonly waiters: Waiter[] = [];
  private readonly sessions = new Map<string, TerminalSidecarSession>();
  private stdoutBuffer = "";
  private closed = false;

  static isAvailable(): boolean {
    return findRustPtyBinary() != null;
  }

  static createIfAvailable(projectDir: string, eventSink: TerminalSidecarEventSink): RustPtySidecar | undefined {
    const binary = findRustPtyBinary();
    if (binary == null) return undefined;
    return new RustPtySidecar(binary, projectDir, eventSink);
  }

  private constructor(
    private readonly binary: string,
    projectDir: string,
    private readonly eventSink: TerminalSidecarEventSink,
  ) {
    this.child = spawn(binary, [], {
      cwd: projectDir,
      shell: false,
      windowsHide: true,
      stdio: ["pipe", "pipe", "pipe"],
      env: {
        ...process.env,
        TERM: process.env.TERM ?? "xterm-256color",
        COLORTERM: process.env.COLORTERM ?? "truecolor",
      },
    });
    this.child.stdout.setEncoding("utf8");
    this.child.stderr.setEncoding("utf8");
    this.child.stdout.on("data", (chunk: string) => this.readStdout(chunk));
    this.child.stderr.on("data", (chunk: string) => {
      this.eventSink("terminal.error", { message: chunk.trim(), source: "mocca-pty" });
    });
    this.child.on("error", (error) => this.rejectAll(error));
    this.child.on("exit", (exitCode) => {
      this.closed = true;
      for (const session of this.sessions.values()) {
        if (!session.exited) {
          session.exited = true;
          this.eventSink("terminal.exited", { terminalId: session.id, exitCode, sidecarExited: true });
        }
      }
      this.rejectAll(new CapabilityError("terminal_sidecar_exited", "Rust PTY sidecar exited", { exitCode }));
    });
  }

  list(): TerminalSidecarSession[] {
    return [...this.sessions.values()];
  }

  isExited(terminalId: string): boolean {
    const session = this.sessions.get(terminalId);
    if (session == null) throw new CapabilityError("terminal_not_found", "Terminal session was not found", { terminalId });
    return session.exited;
  }

  async spawn(payload: Record<string, unknown>) {
    const cols = readOptionalNumber(payload.cols) ?? 120;
    const rows = readOptionalNumber(payload.rows) ?? 40;
    const shell = readOptionalString(payload.shell);
    const frame = await this.sendAndWait(
      { cmd: "spawn", cols, rows, shell },
      (candidate) => candidate.event === "spawned" || candidate.event === "error",
    );
    throwIfSidecarError(frame);
    const terminalId = readString(frame.terminalId, "terminalId");
    const title = readOptionalString(frame.title) ?? path.basename(readOptionalString(frame.shell) ?? shell ?? "");
    const response = {
      id: terminalId,
      shell: readOptionalString(frame.shell) ?? shell ?? "",
      title,
      cols: readOptionalNumber(frame.cols) ?? cols,
      rows: readOptionalNumber(frame.rows) ?? rows,
    };
    this.sessions.set(terminalId, { ...response, exited: false });
    return response;
  }

  async write(payload: Record<string, unknown>) {
    const terminalId = readString(payload.terminalId, "terminalId");
    this.requireSession(terminalId);
    this.send({ cmd: "write", terminalId, data: readString(payload.data, "data") });
    return { success: true, terminalId };
  }

  async resize(payload: Record<string, unknown>) {
    const terminalId = readString(payload.terminalId, "terminalId");
    const session = this.requireSession(terminalId);
    const cols = readOptionalNumber(payload.cols) ?? session.cols;
    const rows = readOptionalNumber(payload.rows) ?? session.rows;
    this.send({ cmd: "resize", terminalId, cols, rows });
    session.cols = cols;
    session.rows = rows;
    return { success: true, terminalId, cols, rows };
  }

  async snapshot(terminalId: string) {
    this.requireSession(terminalId);
    const frame = await this.sendAndWait(
      { cmd: "snapshot", terminalId },
      (candidate) => (candidate.event === "state" && candidate.terminalId === terminalId) || candidate.event === "error",
    );
    throwIfSidecarError(frame);
    return eventPayload(frame);
  }

  async kill(terminalId: string) {
    this.requireSession(terminalId);
    const frame = await this.sendAndWait(
      { cmd: "kill", terminalId },
      (candidate) => (candidate.event === "exit" && candidate.terminalId === terminalId) || candidate.event === "error",
    );
    throwIfSidecarError(frame);
    this.sessions.delete(terminalId);
    return { success: true, terminalId };
  }

  async close(): Promise<void> {
    if (this.closed) return;
    const sessionIds = [...this.sessions.keys()];
    for (const terminalId of sessionIds) {
      await this.kill(terminalId).catch(() => undefined);
    }
    this.closed = true;
    const exited = new Promise<void>((resolve) => {
      if (this.child.exitCode != null || this.child.signalCode != null) {
        resolve();
        return;
      }
      const timeout = setTimeout(resolve, 1_000);
      const done = () => {
        clearTimeout(timeout);
        resolve();
      };
      this.child.once("exit", done);
      this.child.once("close", done);
    });
    this.child.kill();
    await exited;
    this.rejectAll(new CapabilityError("terminal_sidecar_closed", "Rust PTY sidecar was closed"));
  }

  private readStdout(chunk: string): void {
    this.stdoutBuffer += chunk;
    const lines = this.stdoutBuffer.split(/\r?\n/);
    this.stdoutBuffer = lines.pop() ?? "";
    for (const line of lines) {
      if (line.trim().length === 0) continue;
      try {
        this.handleFrame(JSON.parse(line) as SidecarFrame);
      } catch (error) {
        this.eventSink("terminal.error", {
          message: error instanceof Error ? error.message : String(error),
          source: "mocca-pty",
        });
      }
    }
  }

  private handleFrame(frame: SidecarFrame): void {
    this.updateSession(frame);
    const bridgeEvent = toBridgeEvent(frame.event);
    if (bridgeEvent != null) {
      this.eventSink(bridgeEvent, eventPayload(frame));
    }
    for (const waiter of [...this.waiters]) {
      if (waiter.predicate(frame)) {
        this.removeWaiter(waiter);
        waiter.resolve(frame);
      }
    }
  }

  private updateSession(frame: SidecarFrame): void {
    const terminalId = readOptionalString(frame.terminalId);
    if (terminalId == null) return;
    if (frame.event === "spawned") {
      const session = {
        id: terminalId,
        shell: readOptionalString(frame.shell) ?? "",
        title: readOptionalString(frame.title) ?? "",
        cols: readOptionalNumber(frame.cols) ?? 120,
        rows: readOptionalNumber(frame.rows) ?? 40,
        exited: false,
      };
      this.sessions.set(terminalId, session);
      return;
    }
    const session = this.sessions.get(terminalId);
    if (session == null) return;
    if (frame.event === "state") {
      session.cols = readOptionalNumber(frame.cols) ?? session.cols;
      session.rows = readOptionalNumber(frame.rows) ?? session.rows;
    }
    if (frame.event === "exit") {
      session.exited = true;
      this.sessions.delete(terminalId);
    }
  }

  private sendAndWait(command: Record<string, unknown>, predicate: Waiter["predicate"]): Promise<SidecarFrame> {
    return new Promise((resolve, reject) => {
      const waiter: Waiter = {
        predicate,
        resolve,
        reject,
        timeout: setTimeout(() => {
          this.removeWaiter(waiter);
          reject(new CapabilityError("terminal_sidecar_timeout", "Rust PTY sidecar did not answer in time", {
            binary: this.binary,
          }));
        }, DEFAULT_WAIT_MS),
      };
      this.waiters.push(waiter);
      try {
        this.send(command);
      } catch (error) {
        this.removeWaiter(waiter);
        reject(error instanceof Error ? error : new Error(String(error)));
      }
    });
  }

  private send(command: Record<string, unknown>): void {
    if (this.closed || this.child.stdin.destroyed) {
      throw new CapabilityError("terminal_sidecar_unavailable", "Rust PTY sidecar is not running");
    }
    this.child.stdin.write(`${JSON.stringify(withoutUndefined(command))}\n`);
  }

  private requireSession(terminalId: string): TerminalSidecarSession {
    const session = this.sessions.get(terminalId);
    if (session == null) throw new CapabilityError("terminal_not_found", "Terminal session was not found", { terminalId });
    return session;
  }

  private removeWaiter(waiter: Waiter): void {
    clearTimeout(waiter.timeout);
    const index = this.waiters.indexOf(waiter);
    if (index >= 0) this.waiters.splice(index, 1);
  }

  private rejectAll(error: Error): void {
    for (const waiter of [...this.waiters]) {
      this.removeWaiter(waiter);
      waiter.reject(error);
    }
  }
}

function findRustPtyBinary(): string | undefined {
  const configured = process.env.MOCCA_PTY_SIDECAR;
  if (configured != null && configured.length > 0 && existsSync(configured)) return configured;

  const extension = process.platform === "win32" ? ".exe" : "";
  const moduleDir = path.dirname(fileURLToPath(import.meta.url));
  const cliRoot = path.resolve(moduleDir, "../../..");
  const candidates = [
    path.join(cliRoot, "pty", "target", "release", `mocca-pty${extension}`),
    path.join(cliRoot, "pty", "target", "debug", `mocca-pty${extension}`),
  ];
  return candidates.find((candidate) => existsSync(candidate));
}

function toBridgeEvent(event: unknown): string | undefined {
  switch (event) {
    case "spawned":
      return "terminal.spawned";
    case "state":
      return "terminal.state";
    case "exit":
      return "terminal.exited";
    case "error":
      return "terminal.error";
    default:
      return undefined;
  }
}

function eventPayload(frame: SidecarFrame): Record<string, unknown> {
  const { event: _event, ...payload } = frame;
  return payload;
}

function throwIfSidecarError(frame: SidecarFrame): void {
  if (frame.event === "error") {
    throw new CapabilityError("terminal_sidecar_error", readOptionalString(frame.message) ?? "Rust PTY sidecar error", frame);
  }
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

function withoutUndefined(value: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(value).filter(([, entry]) => entry !== undefined));
}
