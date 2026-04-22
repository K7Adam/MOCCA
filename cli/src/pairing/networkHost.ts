import { networkInterfaces, type NetworkInterfaceInfo } from "node:os";

export type NetworkInterfaces = Record<string, NetworkInterfaceInfoLike[] | undefined>;

type NetworkInterfaceInfoLike = Pick<NetworkInterfaceInfo, "address" | "family" | "internal">;

export function selectAdvertiseHost(
  bindHost: string,
  interfaces: NetworkInterfaces = networkInterfaces(),
): string {
  const host = bindHost.trim();
  if (!isWildcardHost(host)) {
    return host;
  }

  for (const entries of Object.values(interfaces)) {
    for (const entry of entries ?? []) {
      if (entry.family === "IPv4" && !entry.internal && entry.address.length > 0) {
        return entry.address;
      }
    }
  }

  return "127.0.0.1";
}

function isWildcardHost(host: string): boolean {
  return host === "0.0.0.0" || host === "::" || host === "";
}
