# PROJECT KNOWLEDGE BASE

**Generated:** 2026-01-14
**Scope:** Domain Models & Contracts

## OVERVIEW
Core immutable data structures and contracts defining the application's domain logic, API responses, and event systems.

## STRUCTURE
```
domain/model/
├── Models.kt             # Core entities (Session, Message, FileInfo) and UI mappers
├── ServerEvents.kt       # Sealed class hierarchy for SSE events matching OpenCode SDK
├── GitTypes.kt           # Git operations: status, log, diff, remote, checkout
├── AgentTypes.kt         # Agent configuration and capability definitions
├── CommandTypes.kt       # Slash command definitions and parameters
├── ProviderTypes.kt      # LLM provider and model definitions
├── LspTypes.kt           # Language Server Protocol types (diagnostics, symbols)
└── DiffTypes.kt          # Unified diff parsing and representation structures
```

## WHERE TO LOOK
| Type | File | Notes |
|------|------|-------|
| Session/Chat | `Models.kt` | `Session`, `Message`, `MessagePart` (Text, Tool, File) |
| SSE Events | `ServerEvents.kt` | `ServerEvent` sealed class + `Properties` data classes |
| Tool UI | `Models.kt` | `RichToolState` sealed hierarchy (Pending, Running, Completed, Error) |
| Git | `GitTypes.kt` | `GitLogEntry`, `GitBranch`, `GitRemote`, `GitCommitRequest` |
| Permissions | `ServerEvents.kt` | `PermissionRequest` (mapped from `permission.asked`), `QuestionRequest` |
| File System | `Models.kt` | `FileInfo`, `FileContent`, `FileStatus`, `Diagnostic` |

## CONVENTIONS
- **Immutability**: All models are `data class` with `val` properties for thread safety.
- **Serialization**: All network types annotated with `@Serializable` for Ktor/Kotlinx.
- **Sealed Hierarchies**: Used for exhaustive state handling (`ServerEvent`, `MessagePart`, `RichToolState`).
- **Mapping**: Companion object functions (e.g., `fromEvent`, `fromResponse`) convert DTOs to UI-ready models.
- **Nullable Fields**: extensive use of nullable types (`?`) to handle optional API fields safely.

## ANTI-PATTERNS
- **NEVER** put complex business logic in data classes; keep them as pure data containers or simple mappers.
- **NEVER** use mutable `var` properties in domain models; state changes should produce new instances.
- **AVOID** using `LegacyPermissionProperties` directly; prefer the normalized `PermissionRequest`.
- **DO NOT** assume non-nullability for API fields unless guaranteed by the OpenCode protocol.
