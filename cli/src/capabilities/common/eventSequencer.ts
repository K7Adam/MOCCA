import { createEvent, type BridgeNamespace, type MoccaEvent } from "../../protocol/message.js";

export class EventSequencer {
  private readonly nextByNamespace = new Map<string, number>();

  create(input: { ns: BridgeNamespace; event: string; payload?: unknown }): MoccaEvent {
    const next = this.next(input.ns);
    return createEvent({
      ns: input.ns,
      event: input.event,
      seq: next,
      payload: input.payload,
    });
  }

  next(namespace: BridgeNamespace): number {
    const value = this.nextByNamespace.get(namespace) ?? 0;
    this.nextByNamespace.set(namespace, value + 1);
    return value;
  }
}
