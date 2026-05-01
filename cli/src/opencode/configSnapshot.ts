import { spawn } from "node:child_process";
import { existsSync } from "node:fs";
import { readFile } from "node:fs/promises";
import { homedir } from "node:os";
import { join } from "node:path";
import { parse } from "jsonc-parser";

export type CommandResult = {
  exitCode: number;
  stdout: string;
  stderr: string;
};

export type CommandRunner = (command: string, args: readonly string[]) => Promise<CommandResult>;

export type OpenCodeConfigSnapshotOptions = {
  projectDir: string;
  homeDir?: string;
  appDataRoamingDir?: string;
  runCommand?: CommandRunner;
};

export type OpenCodeConfigSnapshot = {
  installed: {
    available: boolean;
    command: "opencode";
    version?: string;
    error?: string;
  };
  configFiles: Array<{
    scope: "global" | "project";
    path: string;
    config: Record<string, unknown>;
  }>;
  effective: {
    model?: string;
    plugins: string[];
    tools: Record<string, boolean>;
    raw: Record<string, unknown>;
  };
  credentials: Array<{
    name: string;
    type: "api" | "oauth" | string;
  }>;
  agents: Array<{
    name: string;
    primary: boolean;
  }>;
  commands: Array<{
    name: string;
    description?: string;
  }>;
  mcpServers: Array<{
    name: string;
    type?: string;
    enabled?: boolean;
  }>;
};

const SECRET_KEY_PATTERN = /(api[-_]?key|token|secret|password|authorization|credential|cookie|headers?)/i;
const ANSI_PATTERN = /\u001b\[[0-9;]*m/g;
const DEFAULT_COMMAND_TIMEOUT_MS = 8_000;

export async function collectOpenCodeConfigSnapshot(
  options: OpenCodeConfigSnapshotOptions,
): Promise<OpenCodeConfigSnapshot> {
  const homeDir = options.homeDir ?? homedir();
  const appDataRoamingDir = options.appDataRoamingDir ?? process.env.APPDATA;
  const runCommand = options.runCommand ?? defaultCommandRunner;

  const installed = await detectOpenCode(runCommand);
  const configFiles = await readConfigFiles(options.projectDir, homeDir, appDataRoamingDir);
  const effectiveRaw = mergeConfigs(configFiles.map((file) => file.config));
  const effective = normalizeEffectiveConfig(effectiveRaw);

  const [credentials, agents] = installed.available
    ? await Promise.all([
        runOpenCodeList(runCommand, ["providers", "list"]).then(parseCredentialsOutput).catch(() => []),
        runOpenCodeList(runCommand, ["agent", "list"]).then(parseAgentsOutput).catch(() => []),
      ])
    : [[], []];

  return {
    installed,
    configFiles,
    effective,
    credentials,
    agents,
    commands: extractCommands(effectiveRaw),
    mcpServers: extractMcpServers(effectiveRaw),
  };
}

async function detectOpenCode(runCommand: CommandRunner): Promise<OpenCodeConfigSnapshot["installed"]> {
  try {
    const result = await runCommand("opencode", ["--version"]);
    if (result.exitCode !== 0) {
      return {
        available: false,
        command: "opencode",
        error: result.stderr.trim() || `opencode exited with ${result.exitCode}`,
      };
    }
    return {
      available: true,
      command: "opencode",
      version: result.stdout.trim(),
    };
  } catch (error) {
    return {
      available: false,
      command: "opencode",
      error: error instanceof Error ? error.message : String(error),
    };
  }
}

async function runOpenCodeList(runCommand: CommandRunner, args: readonly string[]): Promise<string> {
  const result = await runCommand("opencode", args);
  if (result.exitCode !== 0) {
    throw new Error(result.stderr.trim() || `opencode ${args.join(" ")} exited with ${result.exitCode}`);
  }
  return result.stdout;
}

async function readConfigFiles(
  projectDir: string,
  homeDir: string,
  appDataRoamingDir?: string,
): Promise<OpenCodeConfigSnapshot["configFiles"]> {
  const candidates: Array<{ scope: "global" | "project"; path: string }> = [
    { scope: "global", path: join(homeDir, ".config", "opencode", "opencode.json") },
    { scope: "global", path: join(homeDir, ".config", "opencode", "opencode.jsonc") },
  ];

  if (appDataRoamingDir) {
    candidates.push(
      { scope: "global", path: join(appDataRoamingDir, "opencode", "opencode.json") },
      { scope: "global", path: join(appDataRoamingDir, "opencode", "opencode.jsonc") },
    );
  }

  candidates.push(
    { scope: "project", path: join(projectDir, "opencode.json") },
    { scope: "project", path: join(projectDir, "opencode.jsonc") },
  );

  const configFiles: OpenCodeConfigSnapshot["configFiles"] = [];
  for (const candidate of candidates) {
    if (!existsSync(candidate.path)) continue;
    const text = await readFile(candidate.path, "utf8");
    const parsed = parse(text);
    if (!isRecord(parsed)) continue;
    configFiles.push({
      scope: candidate.scope,
      path: candidate.path,
      config: redactSecrets(parsed),
    });
  }
  return configFiles;
}

function normalizeEffectiveConfig(raw: Record<string, unknown>): OpenCodeConfigSnapshot["effective"] {
  return {
    model: typeof raw.model === "string" ? raw.model : undefined,
    plugins: normalizeStringArray(raw.plugin),
    tools: isRecord(raw.tools) ? normalizeBooleanRecord(raw.tools) : {},
    raw,
  };
}

function mergeConfigs(configs: Array<Record<string, unknown>>): Record<string, unknown> {
  return configs.reduce<Record<string, unknown>>((merged, config) => deepMerge(merged, config), {});
}

function deepMerge(base: Record<string, unknown>, override: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = { ...base };

  for (const [key, value] of Object.entries(override)) {
    if (key === "plugin") {
      result[key] = unique([...normalizeStringArray(result[key]), ...normalizeStringArray(value)]);
      continue;
    }
    if (isRecord(value) && isRecord(result[key])) {
      result[key] = deepMerge(result[key], value);
      continue;
    }
    result[key] = value;
  }

  return redactSecrets(result);
}

function redactSecrets<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.map((entry) => redactSecrets(entry)) as T;
  }

  if (!isRecord(value)) {
    return value;
  }

  const redacted: Record<string, unknown> = {};
  for (const [key, entry] of Object.entries(value)) {
    redacted[key] = SECRET_KEY_PATTERN.test(key) ? "[redacted]" : redactSecrets(entry);
  }
  return redacted as T;
}

function parseCredentialsOutput(output: string): OpenCodeConfigSnapshot["credentials"] {
  return stripAnsi(output)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .map((line) => line.match(/^[•*-]\s+(.+?)\s+(api|oauth)\s*$/i))
    .filter((match): match is RegExpMatchArray => match != null)
    .map((match) => ({ name: match[1].trim(), type: match[2].toLowerCase() }));
}

function parseAgentsOutput(output: string): OpenCodeConfigSnapshot["agents"] {
  const agents: OpenCodeConfigSnapshot["agents"] = [];
  let inPermissionBlock = false;

  for (const rawLine of stripAnsi(output).split(/\r?\n/)) {
    const line = rawLine.trim();
    if (line.length === 0) continue;

    if (inPermissionBlock) {
      if (line.startsWith("]")) {
        inPermissionBlock = false;
      }
      continue;
    }

    if (line.startsWith("[") || line.startsWith("{")) {
      inPermissionBlock = true;
      continue;
    }

    const match = line.match(/^(.+?)(?:\s+\((primary)\))?$/i);
    if (match == null) continue;
    agents.push({ name: match[1].trim(), primary: match[2]?.toLowerCase() === "primary" });
  }

  return agents;
}

function extractCommands(config: Record<string, unknown>): OpenCodeConfigSnapshot["commands"] {
  if (!isRecord(config.command)) return [];
  return Object.entries(config.command).map(([name, value]) => ({
    name,
    description: isRecord(value) && typeof value.description === "string" ? value.description : undefined,
  }));
}

function extractMcpServers(config: Record<string, unknown>): OpenCodeConfigSnapshot["mcpServers"] {
  if (!isRecord(config.mcp)) return [];
  return Object.entries(config.mcp).map(([name, value]) => ({
    name,
    type: isRecord(value) && typeof value.type === "string" ? value.type : undefined,
    enabled: isRecord(value) && typeof value.enabled === "boolean" ? value.enabled : undefined,
  }));
}

function stripAnsi(value: string): string {
  return value.replace(ANSI_PATTERN, "");
}

function normalizeStringArray(value: unknown): string[] {
  if (typeof value === "string") return [value];
  if (!Array.isArray(value)) return [];
  return value.filter((entry): entry is string => typeof entry === "string");
}

function normalizeBooleanRecord(value: Record<string, unknown>): Record<string, boolean> {
  return Object.fromEntries(
    Object.entries(value).filter((entry): entry is [string, boolean] => typeof entry[1] === "boolean"),
  );
}

function unique(values: string[]): string[] {
  return Array.from(new Set(values));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function defaultCommandRunner(command: string, args: readonly string[]): Promise<CommandResult> {
  return new Promise((resolve) => {
    const child = spawn(command, [...args], {
      windowsHide: true,
      shell: false,
    });

    let stdout = "";
    let stderr = "";
    let settled = false;
    let timeout: NodeJS.Timeout;
    const finish = (result: CommandResult) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      resolve(result);
    };
    timeout = setTimeout(() => {
      stderr = stderr.trim()
        ? `${stderr.trim()}\n${command} ${args.join(" ")} timed out after ${DEFAULT_COMMAND_TIMEOUT_MS}ms`
        : `${command} ${args.join(" ")} timed out after ${DEFAULT_COMMAND_TIMEOUT_MS}ms`;
      child.kill();
      finish({ exitCode: 124, stdout, stderr });
    }, DEFAULT_COMMAND_TIMEOUT_MS);

    child.stdout?.setEncoding("utf8");
    child.stderr?.setEncoding("utf8");
    child.stdout?.on("data", (chunk) => {
      stdout += chunk;
    });
    child.stderr?.on("data", (chunk) => {
      stderr += chunk;
    });
    child.on("error", (error) => {
      finish({ exitCode: 1, stdout, stderr: error.message });
    });
    child.on("close", (code) => {
      finish({ exitCode: code ?? 1, stdout, stderr });
    });
  });
}
