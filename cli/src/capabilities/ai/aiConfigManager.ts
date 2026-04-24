import { createHash } from "node:crypto";
import { watch, type FSWatcher } from "chokidar";
import { createResponse, PROTOCOL_VERSION, type MoccaEvent, type MoccaRequest, type MoccaResponse } from "../../protocol/message.js";
import type { EventSequencer } from "../common/eventSequencer.js";
import type { OpenCodeRuntimeBridge } from "../../opencode/runtimeServer.js";

export type AiConfigManagerOptions = {
  projectDir: string;
  configSnapshotProvider: () => Promise<unknown>;
  openCodeRuntime?: OpenCodeRuntimeBridge;
  eventSequencer: EventSequencer;
  eventSink?: (event: MoccaEvent) => void;
};

export type AiCapabilities = {
  opencodeConfigSnapshot: boolean;
  opencodeRuntime: boolean;
  sessions: boolean;
  messages: boolean;
  configNormalized: boolean;
  providers: boolean;
  agents: boolean;
  modes: boolean;
  selectionDefaults: boolean;
  variantForwarding: boolean;
  configEvents: boolean;
};

export class AiConfigManager {
  readonly capabilities: AiCapabilities;
  private readonly options: AiConfigManagerOptions;
  private watcher: FSWatcher | undefined;
  private watchedPaths = new Set<string>();
  private refreshTimer: NodeJS.Timeout | undefined;
  private cachedSnapshot: unknown;

  constructor(options: AiConfigManagerOptions) {
    this.options = options;
    this.capabilities = {
      opencodeConfigSnapshot: true,
      opencodeRuntime: options.openCodeRuntime != null,
      sessions: options.openCodeRuntime != null,
      messages: options.openCodeRuntime != null,
      configNormalized: true,
      providers: true,
      agents: true,
      modes: true,
      selectionDefaults: true,
      variantForwarding: true,
      configEvents: true,
    };
  }

  async handle(request: MoccaRequest): Promise<MoccaResponse | undefined> {
    if (request.ns === "ai" && request.action === "config.get") {
      return createResponse(request, { ok: true, payload: await this.getConfig(false) });
    }
    if (request.ns === "ai" && request.action === "config.refresh") {
      return createResponse(request, { ok: true, payload: await this.getConfig(true) });
    }
    return undefined;
  }

  async getConfig(force: boolean): Promise<unknown> {
    if (!force && this.cachedSnapshot != null) return this.cachedSnapshot;

    const [configSnapshot, runtimeProviders, providerConfig, runtimeAgents] = await Promise.all([
      this.options.configSnapshotProvider().catch((error) => ({
        installed: { available: false, command: "opencode", error: stringifyError(error) },
        effective: { plugins: [], tools: {}, raw: {} },
        configFiles: [],
        credentials: [],
        agents: [],
        commands: [],
        mcpServers: [],
      })),
      this.options.openCodeRuntime?.listProviders().catch(() => undefined) ?? Promise.resolve(undefined),
      this.options.openCodeRuntime?.listProviderConfig().catch(() => undefined) ?? Promise.resolve(undefined),
      this.options.openCodeRuntime?.listAgents().catch(() => undefined) ?? Promise.resolve(undefined),
    ]);

    const normalized = normalizeAiConfig({
      projectDir: this.options.projectDir,
      configSnapshot,
      runtimeProviders,
      providerConfig,
      runtimeAgents,
    });
    this.cachedSnapshot = normalized;
    this.updateWatcher(readConfigPaths(configSnapshot));
    return normalized;
  }

  async close(): Promise<void> {
    if (this.refreshTimer != null) clearTimeout(this.refreshTimer);
    await this.watcher?.close();
  }

  private updateWatcher(paths: string[]): void {
    const nextPaths = new Set(paths.filter((path) => path.trim().length > 0));
    if (sameSet(this.watchedPaths, nextPaths)) return;

    this.watchedPaths = nextPaths;
    void this.watcher?.close();
    this.watcher = undefined;
    if (nextPaths.size === 0) return;

    this.watcher = watch([...nextPaths], {
      ignoreInitial: true,
      awaitWriteFinish: { stabilityThreshold: 250, pollInterval: 50 },
    });
    this.watcher.on("add", () => this.scheduleChangedEvent());
    this.watcher.on("change", () => this.scheduleChangedEvent());
    this.watcher.on("unlink", () => this.scheduleChangedEvent());
  }

  private scheduleChangedEvent(): void {
    if (this.refreshTimer != null) clearTimeout(this.refreshTimer);
    this.refreshTimer = setTimeout(async () => {
      this.refreshTimer = undefined;
      try {
        const snapshot = await this.getConfig(true);
        this.options.eventSink?.({
          v: PROTOCOL_VERSION,
          ns: "ai",
          event: "config.changed",
          seq: this.options.eventSequencer.next("ai"),
          payload: snapshot,
        });
      } catch (error) {
        this.options.eventSink?.({
          v: PROTOCOL_VERSION,
          ns: "ai",
          event: "config.error",
          seq: this.options.eventSequencer.next("ai"),
          payload: { message: stringifyError(error) },
        });
      }
    }, 300);
  }
}

function normalizeAiConfig(input: {
  projectDir: string;
  configSnapshot: unknown;
  runtimeProviders: unknown;
  providerConfig: unknown;
  runtimeAgents: unknown;
}) {
  const snapshot = readRecord(input.configSnapshot);
  const effective = readRecord(snapshot.effective);
  const raw = readRecord(effective.raw);
  const providers = normalizeProviders(input.runtimeProviders, input.providerConfig);
  const agents = normalizeAgents(input.runtimeAgents, snapshot.agents, raw);
  const modes = normalizeModes(raw, agents);
  const defaultSelection = normalizeDefaultSelection(effective.model, input.providerConfig, providers, agents, modes);

  const normalized = {
    projectDir: input.projectDir,
    source: "mocca-cli",
    fingerprint: { value: "" },
    defaultSelection,
    providers,
    agents,
    modes,
    commands: Array.isArray(snapshot.commands) ? snapshot.commands : [],
    mcpServers: Array.isArray(snapshot.mcpServers) ? snapshot.mcpServers : [],
    configFiles: Array.isArray(snapshot.configFiles) ? snapshot.configFiles : [],
    installed: readRecord(snapshot.installed),
  };

  normalized.fingerprint.value = createHash("sha256")
    .update(JSON.stringify({
      projectDir: normalized.projectDir,
      defaultSelection,
      providers,
      agents,
      modes,
      configFiles: normalized.configFiles,
    }))
    .digest("hex");
  return normalized;
}

function normalizeProviders(runtimeProviders: unknown, providerConfig: unknown) {
  const providerRecords = readProviderRecords(runtimeProviders, providerConfig);
  return providerRecords
    .map(([providerId, provider]) => {
      const record = readRecord(provider);
      const modelsRecord = readRecord(record.models);
      const models = Object.entries(modelsRecord)
        .map(([modelId, value]) => normalizeModel(providerId, modelId, value))
        .sort((a, b) => a.id.localeCompare(b.id));
      return {
        id: providerId,
        name: readString(record.name) ?? providerId,
        source: readString(record.source),
        connected: models.length > 0,
        models,
      };
    })
    .filter((provider) => provider.id.length > 0)
    .sort((a, b) => a.name.localeCompare(b.name) || a.id.localeCompare(b.id));
}

function readProviderRecords(runtimeProviders: unknown, providerConfig: unknown): Array<[string, unknown]> {
  if (Array.isArray(runtimeProviders)) return runtimeProviders.map((entry) => [readString(readRecord(entry).id) ?? "", entry]);
  const runtime = readRecord(runtimeProviders);
  if (Array.isArray(runtime.all)) return runtime.all.map((entry) => [readString(readRecord(entry).id) ?? "", entry]);
  if (Array.isArray(runtime.providers)) return runtime.providers.map((entry) => [readString(readRecord(entry).id) ?? "", entry]);

  if (Array.isArray(providerConfig)) return providerConfig.map((entry) => [readString(readRecord(entry).id) ?? "", entry]);
  const config = readRecord(providerConfig);
  if (Array.isArray(config.providers)) return config.providers.map((entry) => [readString(readRecord(entry).id) ?? "", entry]);
  if (isRecord(config.providers)) return Object.entries(config.providers);
  return [];
}

function normalizeModel(providerId: string, modelId: string, value: unknown) {
  const record = readRecord(value);
  const limit = readRecord(record.limit);
  const contextLimit = readNumber(limit.context)
    ?? readNumber(limit.max_tokens)
    ?? readNumber(limit.context_window)
    ?? readNumber(limit.context_length);
  return {
    providerId,
    id: readString(record.id) ?? modelId,
    name: readString(record.name) ?? modelId,
    status: readString(record.status),
    contextLimit,
    capabilities: Object.keys(readRecord(record.capabilities)).sort(),
    variants: Object.entries(readRecord(record.variants))
      .map(([variantId, variant]) => ({
        id: variantId,
        name: readString(readRecord(variant).name) ?? variantId,
        description: readString(readRecord(variant).description),
      }))
      .sort((a, b) => a.id.localeCompare(b.id)),
  };
}

function normalizeAgents(runtimeAgents: unknown, snapshotAgents: unknown, raw: Record<string, unknown>) {
  const fromRuntime = Array.isArray(runtimeAgents) ? runtimeAgents : [];
  const fromSnapshot = Array.isArray(snapshotAgents) ? snapshotAgents : [];
  const fromConfig = Object.entries(readRecord(raw.agent)).map(([name, value]) => ({ ...readRecord(value), name }));
  const byId = new Map<string, unknown>();
  for (const entry of [...fromConfig, ...fromSnapshot, ...fromRuntime]) {
    const record = readRecord(entry);
    const id = readString(record.id) ?? readString(record.name);
    if (id) byId.set(id, record);
  }
  return [...byId.entries()]
    .map(([id, record]) => {
      const value = readRecord(record);
      const model = readModelLike(value.model);
      return {
        id,
        name: readString(value.name) ?? id,
        description: readString(value.description),
        modeId: readString(value.mode),
        hidden: readBoolean(value.hidden) ?? readBoolean(value.isHidden) ?? false,
        primary: readBoolean(value.primary) ?? false,
        model,
      };
    })
    .filter((agent) => !agent.hidden)
    .sort((a, b) => Number(b.primary) - Number(a.primary) || a.name.localeCompare(b.name));
}

function normalizeModes(raw: Record<string, unknown>, agents: Array<{ id: string; name: string; description?: string; modeId?: string }>) {
  const explicitModes = Object.entries(readRecord(raw.mode)).map(([id, value]) => ({
    id,
    name: readString(readRecord(value).name) ?? id,
    description: readString(readRecord(value).description),
  }));
  if (explicitModes.length > 0) return explicitModes.sort((a, b) => a.name.localeCompare(b.name));
  return agents.map((agent) => ({
    id: agent.id,
    name: agent.name,
    description: agent.description,
  }));
}

function normalizeDefaultSelection(
  model: unknown,
  providerConfig: unknown,
  providers: Array<{ id: string; models: Array<{ id: string }> }>,
  agents: Array<{ id: string; primary: boolean }>,
  modes: Array<{ id: string }>,
) {
  const split = splitModel(readString(model));
  const providerDefaults = readRecord(readRecord(providerConfig).default);
  const providerDefault = Object.entries(providerDefaults).find(([, value]) => typeof value === "string");
  const fromProviderDefault = providerDefault ? { providerId: providerDefault[0], modelId: providerDefault[1] as string } : {};
  const first = providers.flatMap((provider) => provider.models.map((model) => ({ providerId: provider.id, modelId: model.id })))[0];
  const primaryAgent = agents.find((agent) => agent.primary) ?? agents[0];
  return {
    providerId: split?.providerId ?? fromProviderDefault.providerId ?? first?.providerId,
    modelId: split?.modelId ?? fromProviderDefault.modelId ?? first?.modelId,
    agentId: primaryAgent?.id,
    modeId: modes[0]?.id,
    explicitModel: false,
  };
}

function splitModel(value: string | undefined): { providerId: string; modelId: string } | undefined {
  if (!value) return undefined;
  const [providerId, modelId] = value.split("/", 2);
  return providerId && modelId ? { providerId, modelId } : undefined;
}

function readModelLike(value: unknown): { providerId: string; modelId: string } | undefined {
  const split = splitModel(readString(value));
  if (split) return split;
  const record = readRecord(value);
  const providerId = readString(record.providerID ?? record.providerId);
  const modelId = readString(record.modelID ?? record.modelId);
  return providerId && modelId ? { providerId, modelId } : undefined;
}

function readConfigPaths(snapshot: unknown): string[] {
  const files = readRecord(snapshot).configFiles;
  if (!Array.isArray(files)) return [];
  return files.map((file) => readString(readRecord(file).path)).filter((path): path is string => path != null);
}

function readRecord(value: unknown): Record<string, unknown> {
  return isRecord(value) ? value : {};
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function readString(value: unknown): string | undefined {
  return typeof value === "string" && value.trim().length > 0 ? value : undefined;
}

function readNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function readBoolean(value: unknown): boolean | undefined {
  return typeof value === "boolean" ? value : undefined;
}

function stringifyError(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

function sameSet(left: Set<string>, right: Set<string>): boolean {
  if (left.size !== right.size) return false;
  for (const entry of left) {
    if (!right.has(entry)) return false;
  }
  return true;
}
