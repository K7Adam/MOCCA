import { randomBytes } from "node:crypto";
import { createOpencodeClient, createOpencodeServer, type OpencodeClient } from "@opencode-ai/sdk";

export type OpenCodeRuntimeStatus = "idle" | "starting" | "ready" | "failed" | "closed";

export type OpenCodeServerConnection = {
  baseUrl: string;
  host: string;
  port: number;
  username: string;
  password: string;
  useHttps: false;
};

export type OpenCodeRuntimeSnapshot = {
  status: OpenCodeRuntimeStatus;
  projectDir: string;
  startedAt?: string;
  lastError?: string;
  server?: OpenCodeServerConnection;
};

export type OpenCodeRuntimeEvent = {
  type: string;
  properties: Record<string, unknown>;
  raw?: unknown;
};

export type OpenCodeRuntimeManagerOptions = {
  projectDir: string;
  bindHost: string;
  advertiseHost: string;
  startupTimeoutMillis?: number;
};

type OpenCodeServer = Awaited<ReturnType<typeof createOpencodeServer>>;
export type OpenCodeRuntimeEventListener = (event: OpenCodeRuntimeEvent) => void;

const DEFAULT_STARTUP_TIMEOUT_MILLIS = 60_000;

export type OpenCodeRuntimeBridge = {
  getStatus(): OpenCodeRuntimeSnapshot;
  ensureServer(): Promise<OpenCodeServerConnection>;
  listSessions(): Promise<unknown>;
  createSession(payload: unknown): Promise<unknown>;
  getMessages(payload: unknown): Promise<unknown>;
  sendMessage(payload: unknown): Promise<{ ack: true }>;
  abortSession(payload: unknown): Promise<unknown>;
  listProviders(): Promise<unknown>;
  listProviderConfig(): Promise<unknown>;
  listAgents(): Promise<unknown>;
  listCommands(): Promise<unknown>;
  getMcpStatus(): Promise<unknown>;
  subscribe(listener: OpenCodeRuntimeEventListener): () => void;
  close(): Promise<void>;
};

export class OpenCodeRuntimeManager implements OpenCodeRuntimeBridge {
  private readonly options: OpenCodeRuntimeManagerOptions;
  private status: OpenCodeRuntimeStatus = "idle";
  private server: OpenCodeServer | null = null;
  private client: OpencodeClient | null = null;
  private connection: OpenCodeServerConnection | null = null;
  private startPromise: Promise<OpenCodeServerConnection> | null = null;
  private startedAt: string | undefined;
  private lastError: string | undefined;
  private eventLoopRunning = false;
  private closed = false;
  private readonly eventListeners = new Set<OpenCodeRuntimeEventListener>();

  constructor(options: OpenCodeRuntimeManagerOptions) {
    this.options = {
      ...options,
      startupTimeoutMillis: options.startupTimeoutMillis ?? DEFAULT_STARTUP_TIMEOUT_MILLIS,
    };
  }

  getStatus(): OpenCodeRuntimeSnapshot {
    return withoutUndefined({
      status: this.status,
      projectDir: this.options.projectDir,
      startedAt: this.startedAt,
      lastError: this.lastError,
      server: this.connection ?? undefined,
    });
  }

  async ensureServer(): Promise<OpenCodeServerConnection> {
    if (this.connection && this.client && this.server && this.status === "ready") {
      return this.connection;
    }
    if (this.startPromise) {
      return this.startPromise;
    }

    this.status = "starting";
    this.lastError = undefined;
    this.closed = false;
    this.startPromise = this.startServer();

    try {
      const connection = await this.startPromise;
      this.status = "ready";
      this.connection = connection;
      this.startedAt = new Date().toISOString();
      this.startEventLoop();
      return connection;
    } catch (error) {
      this.status = "failed";
      this.lastError = error instanceof Error ? error.message : String(error);
      throw error;
    } finally {
      this.startPromise = null;
    }
  }

  async listSessions(): Promise<unknown> {
    const client = await this.ensureClient();
    return requireData(await client.session.list({ query: { directory: this.options.projectDir } }), "session.list");
  }

  async createSession(payload: unknown): Promise<unknown> {
    const client = await this.ensureClient();
    const body = readOptionalRecord(payload);
    return requireData(
      await client.session.create({
        body: {
          title: readOptionalString(body.title),
          parentID: readOptionalString(body.parentID),
        },
        query: { directory: this.options.projectDir },
      }),
      "session.create",
    );
  }

  async getMessages(payload: unknown): Promise<unknown> {
    const client = await this.ensureClient();
    const body = readOptionalRecord(payload);
    const sessionId = readRequiredString(body.sessionId ?? body.sessionID ?? body.id, "sessionId");
    const limit = readOptionalNumber(body.limit);
    return requireData(
      await client.session.messages({
        path: { id: sessionId },
        query: withoutUndefined({ directory: this.options.projectDir, limit }),
      }),
      "session.messages",
    );
  }

  async sendMessage(payload: unknown): Promise<{ ack: true }> {
    const client = await this.ensureClient();
    const body = readOptionalRecord(payload);
    const sessionId = readRequiredString(body.sessionId ?? body.sessionID ?? body.id, "sessionId");
    const text = readOptionalString(body.text);
    const model = readModelSelector(body.model) ?? readModelSelector(body);
    const agent = readOptionalString(body.agent ?? body.agentId);
    const variant = readOptionalString(body.variant ?? body.variantId ?? body.variantID);
    const parts = readPromptParts(body.parts, text);
    if (parts.length === 0) {
      throw new Error("text or parts is required");
    }
    const response = await client.session.promptAsync({
      path: { id: sessionId },
      query: { directory: this.options.projectDir },
      body: withoutUndefined({
        model,
        agent,
        variant,
        parts,
      }) as never,
    });
    throwIfResponseError(response, "session.promptAsync");
    return { ack: true };
  }

  async abortSession(payload: unknown): Promise<unknown> {
    const client = await this.ensureClient();
    const body = readOptionalRecord(payload);
    const sessionId = readRequiredString(body.sessionId ?? body.sessionID ?? body.id, "sessionId");
    return requireData(
      await client.session.abort({
        path: { id: sessionId },
        query: { directory: this.options.projectDir },
      }),
      "session.abort",
    );
  }

  async listProviders(): Promise<unknown> {
    const client = await this.ensureClient();
    return requireData(
      await client.provider.list({ query: { directory: this.options.projectDir } }),
      "provider.list",
    );
  }

  async listProviderConfig(): Promise<unknown> {
    const client = await this.ensureClient();
    return requireData(
      await client.config.providers({ query: { directory: this.options.projectDir } }),
      "config.providers",
    );
  }

  async listAgents(): Promise<unknown> {
    const client = await this.ensureClient();
    return requireData(
      await client.app.agents({ query: { directory: this.options.projectDir } }),
      "app.agents",
    );
  }

  async listCommands(): Promise<unknown> {
    const client = await this.ensureClient();
    return requireData(
      await client.command.list({ query: { directory: this.options.projectDir } }),
      "command.list",
    );
  }

  async getMcpStatus(): Promise<unknown> {
    const client = await this.ensureClient();
    return requireData(
      await client.mcp.status({ query: { directory: this.options.projectDir } }),
      "mcp.status",
    );
  }

  subscribe(listener: OpenCodeRuntimeEventListener): () => void {
    this.eventListeners.add(listener);
    return () => {
      this.eventListeners.delete(listener);
    };
  }

  async close(): Promise<void> {
    this.closed = true;
    this.status = "closed";
    this.connection = null;
    this.client = null;
    this.startPromise = null;
    this.eventListeners.clear();
    this.server?.close();
    this.server = null;
  }

  private async ensureClient(): Promise<OpencodeClient> {
    await this.ensureServer();
    if (!this.client) {
      throw new Error("OpenCode runtime client is not available");
    }
    return this.client;
  }

  private async startServer(): Promise<OpenCodeServerConnection> {
    const username = "mocca";
    const password = randomBytes(32).toString("base64url");
    const authHeader = `Basic ${Buffer.from(`${username}:${password}`).toString("base64")}`;

    process.env.OPENCODE_SERVER_USERNAME = username;
    process.env.OPENCODE_SERVER_PASSWORD = password;

    const server = await createOpencodeServer({
      hostname: this.options.bindHost,
      port: 0,
      timeout: this.options.startupTimeoutMillis,
    });
    const localUrl = new URL(server.url);
    const port = Number(localUrl.port);
    if (!Number.isInteger(port) || port < 1 || port > 65535) {
      server.close();
      throw new Error(`OpenCode runtime returned an invalid port: ${server.url}`);
    }

    this.server = server;
    this.client = createOpencodeClient({
      baseUrl: server.url,
      headers: { Authorization: authHeader },
      directory: this.options.projectDir,
    });

    return {
      baseUrl: `http://${formatHostForUrl(this.options.advertiseHost)}:${port}`,
      host: this.options.advertiseHost,
      port,
      username,
      password,
      useHttps: false,
    };
  }

  private startEventLoop(): void {
    if (this.eventLoopRunning || !this.client) return;
    this.eventLoopRunning = true;
    void this.runEventLoop();
  }

  private async runEventLoop(): Promise<void> {
    let attempt = 0;
    while (!this.closed && this.client) {
      try {
        const events = await this.client.event.subscribe({ query: { directory: this.options.projectDir } });
        attempt = 0;
        for await (const raw of events.stream) {
          if (this.closed) return;
          this.emitRuntimeEvent(normalizeRuntimeEvent(raw));
        }
      } catch (error) {
        if (this.closed) return;
        attempt += 1;
        this.emitRuntimeEvent({
          type: "runtime.event.error",
          properties: {
            message: error instanceof Error ? error.message : String(error),
            attempt,
          },
        });
        await delay(Math.min(500 * 2 ** Math.min(attempt, 5), 15_000));
      }
    }
    this.eventLoopRunning = false;
  }

  private emitRuntimeEvent(event: OpenCodeRuntimeEvent): void {
    for (const listener of this.eventListeners) {
      listener(event);
    }
  }
}

function requireData<T>(response: { data?: T; error?: unknown }, label: string): T {
  if (response.data !== undefined) {
    return response.data;
  }
  const message = response.error === undefined ? `${label} returned no data` : stringifyError(response.error);
  throw new Error(message);
}

function throwIfResponseError(response: { error?: unknown }, label: string): void {
  if (response.error !== undefined) {
    throw new Error(`${label}: ${stringifyError(response.error)}`);
  }
}

function normalizeRuntimeEvent(raw: unknown): OpenCodeRuntimeEvent {
  const record = readOptionalRecord(raw);
  const payload = readOptionalRecord(record.payload);
  const source = typeof payload.type === "string" ? payload : record;
  const type = typeof source.type === "string" && source.type.length > 0 ? source.type : "runtime.event";
  const properties = readOptionalRecord(source.properties);
  return { type, properties, raw };
}

function readOptionalRecord(value: unknown): Record<string, unknown> {
  return value !== null && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
}

function readOptionalString(value: unknown): string | undefined {
  return typeof value === "string" && value.trim().length > 0 ? value : undefined;
}

function readRequiredString(value: unknown, field: string): string {
  const text = readOptionalString(value);
  if (!text) {
    throw new Error(`${field} is required`);
  }
  return text;
}

function readOptionalNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function readModelSelector(value: unknown): { providerID: string; modelID: string } | undefined {
  const model = readOptionalRecord(value);
  const providerID = readOptionalString(model.providerID ?? model.providerId);
  const modelID = readOptionalString(model.modelID ?? model.modelId);
  return providerID && modelID ? { providerID, modelID } : undefined;
}

export function readPromptParts(value: unknown, fallbackText: string | undefined): Array<Record<string, unknown>> {
  if (Array.isArray(value)) {
    const parts = value
      .map((entry) => readOptionalRecord(entry))
      .map((entry) => stripNullish(entry) as Record<string, unknown>)
      .filter((entry) => readOptionalString(entry.type) != null);
    if (parts.length > 0) return parts;
  }
  return fallbackText != null ? [{ type: "text", text: fallbackText }] : [];
}

function stripNullish(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map((entry) => stripNullish(entry));
  }
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>)
        .filter(([, entry]) => entry !== null && entry !== undefined)
        .map(([key, entry]) => [key, stripNullish(entry)]),
    );
  }
  return value;
}

function stringifyError(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === "string") {
    return error;
  }
  try {
    return JSON.stringify(error);
  } catch {
    return String(error);
  }
}

function withoutUndefined<T extends Record<string, unknown>>(value: T): T {
  return Object.fromEntries(Object.entries(value).filter(([, entry]) => entry !== undefined)) as T;
}

function formatHostForUrl(host: string): string {
  return host.includes(":") && !host.startsWith("[") ? `[${host}]` : host;
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
