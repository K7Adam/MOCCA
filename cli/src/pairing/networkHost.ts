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

  const candidates: Array<{ address: string; score: number }> = [];
  for (const [interfaceName, entries] of Object.entries(interfaces)) {
    for (const entry of entries ?? []) {
      if (entry.family === "IPv4" && !entry.internal && entry.address.length > 0) {
        const score = scoreAdvertiseAddress(interfaceName, entry.address);
        if (score != null) candidates.push({ address: entry.address, score });
      }
    }
  }

  candidates.sort((left, right) => right.score - left.score);
  if (candidates[0] != null) return candidates[0].address;

  return "127.0.0.1";
}

function isWildcardHost(host: string): boolean {
  return host === "0.0.0.0" || host === "::" || host === "";
}

function scoreAdvertiseAddress(interfaceName: string, address: string): number | undefined {
  const octets = address.split(".").map((part) => Number(part));
  if (octets.length !== 4 || octets.some((part) => !Number.isInteger(part) || part < 0 || part > 255)) return undefined;
  const [a, b] = octets;
  if (a === 0 || a === 127 || a === 169 && b === 254 || a >= 224) return undefined;
  if (a === 100 && b >= 64 && b <= 127) return undefined;

  const normalizedName = interfaceName.toLowerCase();
  if (/(tailscale|wireguard|vpn|tunnel|tap|docker|veth|vethernet|wsl|hyper-v|vmware|virtualbox|bluetooth)/.test(normalizedName)) {
    return undefined;
  }

  let score = 0;
  if (a === 192 && b === 168) score = 300;
  else if (a === 10) score = 280;
  else if (a === 172 && b >= 16 && b <= 31) score = 260;
  else score = 100;

  if (/(wlan|wi-fi|wifi|wireless|802\.11)/.test(normalizedName)) score += 40;
  if (/(ethernet|gbe|lan)/.test(normalizedName)) score += 20;
  return score;
}
