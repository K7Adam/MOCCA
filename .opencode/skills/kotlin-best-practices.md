---
description: (project - Skill) Best practices for Kotlin development in this project.
---

# Kotlin Best Practices

## 1. Null Safety
**Trust the Compiler.** Do not use unnecessary null-safety operators (`?`, `!!`, `?:`) on non-nullable types.

- **Bad**: `val x: String = "foo" ?: "bar"` (Redundant elvis)
- **Bad**: `val x: String = "foo"!!` (Unnecessary assertion)
- **Good**: `val x: String = "foo"`

When dealing with platform types or ambiguous nullability (e.g. from Java), check the type inference first. If the IDE/Compiler says it's non-nullable, treat it as such.

## 2. Resource Handling
- Use `use` for closable resources.
- Use `Resource<T>` sealed class for data loading states (Loading, Success, Error).

## 3. Coroutines
- **NEVER** block the main thread.
- Use `Dispatchers.IO` for DB/Network operations.
- Prefer `StateFlow` for UI state.
- Use `SupervisorJob` in Repositories to prevent crash propagation.

## 4. Architecture (MVI)
- **State**: Immutable `data class`.
- **Events**: Sealed interfaces/classes.
- **Side Effects**: `SharedFlow` or `Channel`.
- **Repositories**: Return `Flow<Resource<T>>` for offline-first data.

## 5. Logging
- Use `Napier` for logging.
- `Napier.i`, `Napier.d`, `Napier.e`.
- Do not use `android.util.Log` directly.
