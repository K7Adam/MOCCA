# Feature Parity — Issues & Gotchas

## [2026-02-27] Known Issues

### ApiExecutor pattern — CRITICAL
Never do:
```kotlin
val client = HttpClient { ... }
client.get(...)
```
Always do:
```kotlin
apiExecutor.execute { client ->
    client.get(...)
}
```

### SSE event format
Events arrive as: `data: {"payload":{"type":"event.name","properties":{...}}}`
Parse with: `Json.decodeFromString<SsePayload>(data).payload`

### Route naming: "terminal" vs "pty"
MOCCA uses "terminal" naming internally but server uses "/pty/" paths.
`MoccaApiClient.kt` already maps between these.

### Experimental worktree endpoint
Server path is `/experimental/worktree` (singular), not `/experimental/worktrees`.

### Global config vs instance config
- GET/PATCH /global/config → persists across all instances (AppConfig-level)
- GET/PATCH /config/ → per-instance config (bound to current server connection)
Both need to be supported — they are different!
