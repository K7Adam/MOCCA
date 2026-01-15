# DATA REPOSITORY LAYER

## OVERVIEW
Core data access layer implementing offline-first repositories that mediate between remote API and local SQLDelight cache.

## STRUCTURE
```
repository/
├── *Repository.kt         # Feature-specific data operations (Session, File, Git, etc.)
└── AppConnectionManager.kt # Centralized network connection state logic
```

## WHERE TO LOOK
| Component | Purpose |
|-----------|---------|
| `SessionRepository` | Chat history, message persistence, and session management |
| `EventStreamRepository` | Single SSE connection handling and event routing |
| `ServerConfigRepository` | Active server URL management and switching logic |
| `FileRepository` | File system access, search, and symbol lookup |
| `GitRepository` | Version control operations and status tracking |

## CONVENTIONS
- **Offline-first**: All repositories must check `LocalCache` first, emit cached data, then fetch from network.
- **Resource Flow**: Public methods return `Flow<Resource<T>>` to handle Loading, Success (Cached/Network), and Error states.
- **LocalCache Usage**: Injected `LocalCache` is the single source of truth for persistent data.
- **EventStream Role**: `EventStreamRepository` maintains the SSE connection and broadcasts global events.

## ANTI-PATTERNS
- **Blocking Calls**: Never use `runBlocking` or blocking I/O; strictly use `suspend` functions and `Flows`.
- **In-Memory State**: Avoid storing business state in repository variables; persist to `LocalCache` immediately.
- **Direct API Usage**: UI screens must NEVER call `MoccaApiClient` directly; always go through a Repository.
- **Leaking Network types**: Map network DTOs to Domain models before emitting from the repository.
