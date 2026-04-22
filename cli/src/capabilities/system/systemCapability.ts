import os from "node:os";
import { createResponse, type MoccaEvent, type MoccaRequest, type MoccaResponse } from "../../protocol/message.js";
import { runCommand } from "../common/childProcess.js";
import { ConfirmationStore, type ConfirmationInput } from "../common/confirmation.js";
import { CapabilityError } from "../common/errors.js";
import { EventSequencer } from "../common/eventSequencer.js";

export type NativeSystemCapabilities = {
  process: { native: boolean; kill: boolean };
  ports: { native: boolean };
  monitor: { native: boolean };
};

export type SystemCapabilityOptions = {
  projectDir: string;
  confirmationStore: ConfirmationStore;
  eventSequencer: EventSequencer;
  eventSink?: (event: MoccaEvent) => void;
};

export class SystemCapability {
  readonly capabilities: NativeSystemCapabilities = {
    process: { native: true, kill: true },
    ports: { native: true },
    monitor: { native: true },
  };

  constructor(private readonly options: SystemCapabilityOptions) {}

  async handle(request: MoccaRequest): Promise<MoccaResponse | undefined> {
    if (request.ns === "process") {
      if (request.action === "list") return ok(request, await this.listProcesses());
      if (request.action === "kill") return ok(request, await this.killProcess(readPayload(request)));
    }
    if (request.ns === "ports" && request.action === "list") return ok(request, await this.listPorts());
    if (request.ns === "monitor" && request.action === "snapshot") return ok(request, await this.monitorSnapshot());
    return undefined;
  }

  private async listProcesses() {
    if (process.platform === "win32") {
      const output = await runCommand("powershell.exe", [
        "-NoProfile",
        "-Command",
        "Get-Process | Sort-Object CPU -Descending | Select-Object -First 40 @{Name='command';Expression={$_.ProcessName}},@{Name='pid';Expression={$_.Id}},@{Name='cpu';Expression={[math]::Round((if($_.CPU){$_.CPU}else{0}),2)}},@{Name='memory';Expression={('{0:N1} MB' -f ($_.WorkingSet64 / 1MB))}},@{Name='user';Expression={''}} | ConvertTo-Csv -NoTypeInformation",
      ], { cwd: this.options.projectDir, timeoutMillis: 15_000 });
      return parseCsv(output.stdout).slice(1).map((columns) => ({
        command: columns[0] ?? "",
        pid: columns[1] ?? "",
        cpu: parseFloatOrNull(columns[2]),
        memory: columns[3] || undefined,
        user: columns[4] || undefined,
      })).filter((entry) => entry.pid !== "");
    }

    const output = await runCommand("ps", ["-axo", "pid=,user=,%cpu=,%mem=,comm="], {
      cwd: this.options.projectDir,
      timeoutMillis: 15_000,
    });
    return output.stdout.split(/\r?\n/).map((line) => line.trim()).filter(Boolean).map((line) => {
      const parts = line.split(/\s+/, 5);
      return {
        pid: parts[0] ?? "",
        user: parts[1] || undefined,
        cpu: parseFloatOrNull(parts[2]),
        memory: parts[3] != null ? `${parts[3]}%` : undefined,
        command: parts[4] ?? "",
      };
    }).filter((entry) => entry.pid !== "");
  }

  private async killProcess(payload: Record<string, unknown>) {
    const pid = readString(payload.pid, "pid");
    this.options.confirmationStore.require({
      action: "process.kill",
      target: pid,
      risk: "Terminate a process on the computer running MOCCA CLI",
      payload,
      confirmation: readConfirmation(payload.confirmation),
    });
    if (process.platform === "win32") {
      await runCommand("taskkill.exe", ["/PID", pid, "/T", "/F"], { cwd: this.options.projectDir, timeoutMillis: 10_000 });
    } else {
      await runCommand("kill", ["-TERM", pid], { cwd: this.options.projectDir, timeoutMillis: 10_000 });
    }
    this.emit("process.changed", { pid, killed: true });
    return { success: true, pid };
  }

  private async listPorts() {
    if (process.platform === "win32") {
      const output = await runCommand("netstat.exe", ["-ano", "-p", "tcp"], {
        cwd: this.options.projectDir,
        timeoutMillis: 15_000,
      });
      return output.stdout.split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => /^TCP/i.test(line) && /LISTENING/i.test(line))
        .map((line) => {
          const parts = line.split(/\s+/);
          const address = parts[1] ?? "";
          return {
            port: extractPort(address) ?? 0,
            protocol: "tcp",
            process: parts[4] ? `pid ${parts[4]}` : undefined,
            address: address.replace(/:\d+$/, ""),
          };
        })
        .filter((port) => port.port > 0)
        .sort((a, b) => a.port - b.port);
    }

    const command = process.platform === "darwin" ? "lsof" : "ss";
    const args = process.platform === "darwin" ? ["-nP", "-iTCP", "-sTCP:LISTEN"] : ["-tlnp"];
    const output = await runCommand(command, args, { cwd: this.options.projectDir, timeoutMillis: 15_000 });
    return process.platform === "darwin" ? parseLsofPorts(output.stdout) : parseSsPorts(output.stdout);
  }

  private async monitorSnapshot() {
    const memoryTotal = os.totalmem();
    const memoryFree = os.freemem();
    const cpuPercent = await readCpuPercent(this.options.projectDir).catch(() => undefined);
    return {
      cpuPercent,
      memoryUsed: memoryTotal - memoryFree,
      memoryTotal,
      diskUsed: undefined,
      diskTotal: undefined,
    };
  }

  private emit(event: string, payload: unknown): void {
    this.options.eventSink?.(this.options.eventSequencer.create({ ns: "process", event, payload }));
  }
}

async function readCpuPercent(cwd: string): Promise<number | undefined> {
  if (process.platform === "win32") {
    const output = await runCommand("powershell.exe", [
      "-NoProfile",
      "-Command",
      "(Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average",
    ], { cwd, timeoutMillis: 10_000 });
    return parseFloatOrNull(output.stdout.trim()) ?? undefined;
  }
  const load = os.loadavg()[0];
  const cores = Math.max(1, os.cpus().length);
  return Math.min(100, Math.max(0, (load / cores) * 100));
}

function parseSsPorts(output: string) {
  return output.split(/\r?\n/).map((line) => line.trim()).filter((line) => line && !line.startsWith("State")).map((line) => {
    const parts = line.split(/\s+/);
    const address = parts[3] ?? parts[4] ?? "";
    const port = extractPort(address);
    const processText = /users:\(\("([^"]+)"/.exec(line)?.[1];
    return port == null ? undefined : { port, protocol: "tcp", process: processText, address: address.replace(/:\d+$/, "") };
  }).filter((entry) => entry != null) as Array<{ port: number; protocol: string; process?: string; address: string }>;
}

function parseLsofPorts(output: string) {
  return output.split(/\r?\n/).slice(1).map((line) => line.trim()).filter(Boolean).map((line) => {
    const parts = line.split(/\s+/);
    const name = parts.at(-1) ?? "";
    const port = extractPort(name);
    return port == null ? undefined : { port, protocol: "tcp", process: parts[0], address: name.replace(/:\d+.*$/, "") };
  }).filter((entry) => entry != null) as Array<{ port: number; protocol: string; process?: string; address: string }>;
}

function parseCsv(output: string): string[][] {
  return output.split(/\r?\n/).filter(Boolean).map((line) => {
    const values = [];
    let current = "";
    let quoted = false;
    for (const char of line) {
      if (char === "\"") quoted = !quoted;
      else if (char === "," && !quoted) {
        values.push(current);
        current = "";
      } else current += char;
    }
    values.push(current);
    return values;
  });
}

function extractPort(address: string): number | undefined {
  return parseInt(address.replace(/\]/g, "").split(":").at(-1) ?? "", 10) || undefined;
}

function parseFloatOrNull(value: string | undefined): number | undefined {
  if (value == null) return undefined;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
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

function readConfirmation(value: unknown): ConfirmationInput | undefined {
  if (typeof value !== "object" || value == null || Array.isArray(value)) return undefined;
  const operationId = (value as Record<string, unknown>).operationId;
  return typeof operationId === "string" ? { operationId } : undefined;
}
