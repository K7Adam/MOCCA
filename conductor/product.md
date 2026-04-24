# Product Definition: MOCCA

## Vision

MOCCA is an Android companion for OpenCode that lets developers work with an AI coding agent from a mobile device without turning the phone into the source of truth for project state.

The preferred path is the MOCCA CLI bridge. The bridge owns local project access, OpenCode runtime details, and pairing; the Android app owns the mobile UI, state presentation, and interaction loops.

## Audience

- Mobile developers who need to monitor and steer agent sessions away from the desktop.
- Software engineers who want quick review, chat, git, terminal, and file actions from Android.
- Power users who rely on Tailscale/LAN workflows and need deterministic pairing.

## Core Goals

1. **Reliable AI Collaboration:** Real-time sessions, messages, permissions, questions, and runtime model selection.
2. **Project-Aware Tooling:** Files, git, terminal, MCP, commands, and agents exposed through typed repositories instead of ad hoc endpoint knowledge in UI code.
3. **Bridge-First Operation:** New pairing and runtime config should prefer the MOCCA CLI bridge; legacy direct OpenCode server mode remains an advanced fallback.
4. **Fast Mobile UI:** Server-first state, bounded caches, and Material 3 Expressive interactions that stay responsive during long-running agent work.
5. **Low-Maintenance Surface:** Remove dead compatibility paths and stale docs when architecture moves.

## Product Boundaries

- Android is the only supported target.
- The app must not require a physical device for local agent or Maestro verification.
- Model picker recents are project-scoped AI runtime data, not session history.
- Direct OpenCode HTTP/SSE dependencies should shrink as bridge parity improves.

## Success Signals

- A new user can pair through the CLI QR flow and start a chat without manual server setup.
- Sessions and messages remain usable with cache-first behavior during transient network failures.
- Runtime model, agent, mode, and variant selection reflect the active project.
- Cleanup work leaves tests, build verification, and current docs behind.
