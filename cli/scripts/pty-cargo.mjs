import { spawnSync } from "node:child_process";
import { existsSync, readdirSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const task = process.argv[2] === "test" ? "test" : "build";
const manifestPath = path.join("pty", "Cargo.toml");
const env = { ...process.env };
const cargoBin = path.join(os.homedir(), ".cargo", "bin");
if (existsSync(path.join(cargoBin, process.platform === "win32" ? "cargo.exe" : "cargo"))) {
  env.PATH = `${cargoBin}${path.delimiter}${env.PATH ?? ""}`;
}
const args = [];

if (process.platform === "win32" && hasRustupToolchain("stable-x86_64-pc-windows-gnullvm")) {
  args.push("+stable-x86_64-pc-windows-gnullvm");
  const llvmMingwBin = findLlvmMingwBin();
  if (llvmMingwBin != null) {
    env.PATH = `${llvmMingwBin}${path.delimiter}${env.PATH ?? ""}`;
  }
  env.RUSTFLAGS = `${env.RUSTFLAGS ?? ""} -C target-feature=+crt-static`.trim();
}

args.push(task, "--manifest-path", manifestPath);

const result = spawnSync("cargo", args, {
  cwd: path.resolve(path.dirname(fileURLToPath(import.meta.url)), ".."),
  env,
  stdio: "inherit",
  shell: process.platform === "win32",
});

if (result.error != null) {
  console.error(result.error.message);
  process.exit(1);
}
process.exit(result.status ?? 1);

function hasRustupToolchain(name) {
  const result = spawnSync("rustup", ["toolchain", "list"], {
    env,
    encoding: "utf8",
    shell: process.platform === "win32",
  });
  return result.status === 0 && result.stdout.includes(name);
}

function findLlvmMingwBin() {
  const pathEntries = (env.PATH ?? "").split(path.delimiter);
  const fromPath = pathEntries.find((entry) => existsSync(path.join(entry, "x86_64-w64-mingw32-clang.exe")));
  if (fromPath != null) return fromPath;

  const wingetPackages = path.join(os.homedir(), "AppData", "Local", "Microsoft", "WinGet", "Packages");
  if (!existsSync(wingetPackages)) return undefined;

  for (const packageDir of readdirSync(wingetPackages)) {
    if (!packageDir.startsWith("MartinStorsjo.LLVM-MinGW")) continue;
    const fullPackageDir = path.join(wingetPackages, packageDir);
    for (const childDir of readdirSync(fullPackageDir)) {
      if (!childDir.startsWith("llvm-mingw")) continue;
      const bin = path.join(fullPackageDir, childDir, "bin");
      if (existsSync(path.join(bin, "x86_64-w64-mingw32-clang.exe"))) return bin;
    }
  }
  return undefined;
}
