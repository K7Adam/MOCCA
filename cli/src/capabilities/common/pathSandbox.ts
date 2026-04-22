import path from "node:path";
import { promises as fs } from "node:fs";
import { existsSync, realpathSync } from "node:fs";
import { CapabilityError } from "./errors.js";

export type SafePath = {
  relativePath: string;
  absolutePath: string;
};

export class PathSandbox {
  readonly root: string;

  constructor(projectDir: string) {
    this.root = realpathSync(projectDir);
  }

  resolveExisting(input = ""): SafePath {
    const relativePath = normalizeRelativePath(input);
    const absolutePath = path.resolve(this.root, relativePath);
    const real = realpathSync(absolutePath);
    this.assertInside(real);
    return {
      relativePath: this.toPublicPath(real),
      absolutePath: real,
    };
  }

  async resolveForWrite(input = ""): Promise<SafePath> {
    const relativePath = normalizeRelativePath(input);
    const absolutePath = path.resolve(this.root, relativePath);
    const parentReal = await this.realExistingAncestor(path.dirname(absolutePath));
    this.assertInside(parentReal);
    this.assertInside(absolutePath);
    return {
      relativePath,
      absolutePath,
    };
  }

  toPublicPath(absolutePath: string): string {
    this.assertInside(absolutePath);
    const relative = path.relative(this.root, absolutePath);
    return normalizeSlashes(relative === "" ? "" : relative);
  }

  private assertInside(absolutePath: string): void {
    const relative = path.relative(this.root, absolutePath);
    if (relative.startsWith("..") || path.isAbsolute(relative)) {
      throw new CapabilityError("path_outside_project", "Path escapes the MOCCA CLI project root", {
        root: this.root,
      });
    }
  }

  private async realExistingAncestor(inputPath: string): Promise<string> {
    let cursor = inputPath;
    while (!fileExists(cursor)) {
      const parent = path.dirname(cursor);
      if (parent === cursor) {
        throw new CapabilityError("path_not_found", "No existing parent path was found", { path: inputPath });
      }
      cursor = parent;
    }
    return fs.realpath(cursor);
  }
}

export function normalizeRelativePath(input = ""): string {
  const raw = input.replace(/\\/g, "/").trim();
  if (raw === "" || raw === ".") return "";
  if (raw.startsWith("/") || raw.startsWith("//") || /^[a-zA-Z]:/.test(raw)) {
    throw new CapabilityError("invalid_path", "Only project-relative paths are allowed", { path: input });
  }
  const normalized = path.posix.normalize(raw);
  if (normalized === "." || normalized === "") return "";
  if (normalized === ".." || normalized.startsWith("../")) {
    throw new CapabilityError("invalid_path", "Path traversal is not allowed", { path: input });
  }
  return normalized;
}

export function normalizeSlashes(value: string): string {
  return value.replace(/\\/g, "/");
}

export function fileExists(absolutePath: string): boolean {
  return existsSync(absolutePath);
}
