export class CapabilityError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly details?: unknown,
  ) {
    super(message);
  }
}

export function toErrorCode(error: unknown): string {
  if (error instanceof CapabilityError) return error.code;
  if (typeof error === "object" && error != null && "code" in error && typeof error.code === "string") {
    return error.code;
  }
  return "bridge_request_failed";
}

export function toErrorDetails(error: unknown): unknown {
  if (error instanceof CapabilityError) return error.details;
  if (typeof error === "object" && error != null && "details" in error) {
    return error.details;
  }
  return undefined;
}
