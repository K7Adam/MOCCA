import { spawn } from "node:child_process";
import { CapabilityError } from "./errors.js";

export type CommandResult = {
  stdout: string;
  stderr: string;
  exitCode: number;
};

export function runCommand(
  command: string,
  args: readonly string[],
  options: { cwd: string; timeoutMillis?: number },
): Promise<CommandResult> {
  const timeoutMillis = options.timeoutMillis ?? 30_000;
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: options.cwd,
      shell: false,
      windowsHide: true,
      stdio: ["ignore", "pipe", "pipe"],
    });
    let stdout = "";
    let stderr = "";
    const timeout = setTimeout(() => {
      child.kill();
      reject(new CapabilityError("command_timeout", `${command} timed out`, { command, args, timeoutMillis }));
    }, timeoutMillis);

    child.stdout.setEncoding("utf8");
    child.stderr.setEncoding("utf8");
    child.stdout.on("data", (chunk: string) => {
      stdout += chunk;
    });
    child.stderr.on("data", (chunk: string) => {
      stderr += chunk;
    });
    child.on("error", (error) => {
      clearTimeout(timeout);
      reject(new CapabilityError("command_failed", error.message, { command, args }));
    });
    child.on("close", (exitCode) => {
      clearTimeout(timeout);
      const result = { stdout, stderr, exitCode: exitCode ?? -1 };
      if (result.exitCode !== 0) {
        reject(new CapabilityError("command_failed", stderr.trim() || stdout.trim() || `${command} failed`, {
          command,
          args,
          exitCode: result.exitCode,
          stdout,
          stderr,
        }));
        return;
      }
      resolve(result);
    });
  });
}
