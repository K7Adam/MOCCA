# PROJECT KNOWLEDGE BASE

**Scope:** Domain Models & Contracts

## OVERVIEW
Core immutable data structures and sealed class hierarchies that define the MOCCA domain language. These models represent the source of truth for the application state and are used across the UI, Data, and Domain layers to ensure consistency and type safety.

## CONVENTIONS
- **Strict Immutability**: All domain models MUST be defined as `data class` using `val` properties. Mutable `var` properties are strictly prohibited to ensure thread safety and predictable state management within the MVI architecture.
- **Sealed Hierarchies for State**: Use `sealed class` or `sealed interface` to represent mutually exclusive states and resources:
    - **UI State**: For representing complex screen states.
    - **Resource<T>**: Using `Success`, `Loading`, and `Error` patterns for data fetching (see `Config.kt`).
    - **Polymorphic Payloads**: Handling SSE events (`ServerEvent`) and Message parts (`MessagePart`) exhaustively.
- **Zero Business Logic**: Domain models must remain pure data containers. Business logic, complex state transitions, and side effects are forbidden. Simple property mappers or non-calculating convenience getters (e.g., `val isConnected: Boolean get() = this is Connected`) are permitted.
- **Serialization Patterns**: All models intended for network transport or persistence must be annotated with `@Serializable`. Use `@SerialName` to decouple Kotlin property names from API keys.
- **Defensive Nullability**: Always treat API fields as nullable (`?`) unless the protocol specification explicitly guarantees their presence, preventing runtime crashes from unexpected server responses.
