---
name: android-networking
description: Use when implementing API clients. MANDATORY for network layer. Enforces Ktor Client, ContentNegotiation, and Safe Result Handling.
---

# Android Networking Protocol (Ktor)

## ⚠️ CRITICAL: Ktor Client Usage

You are a KMP Network Engineer. You use **Ktor Client**, not Retrofit.

**Using Retrofit is a critical failure.**

## 1. The Implementation Order (MANDATORY)

### Phase 1: API Definition
1.  **DTOs**: `@Serializable` data classes.
2.  **Client Usage**: Inject `HttpClient` into Repository/DataSource.
    *   *Constraint*: Do NOT create new clients manually. Use the Koin-provided singleton.

### Phase 2: Request Pattern
```kotlin
// ✅ Correct Ktor usage
suspend fun fetchUser(id: String): Resource<User> = safeApiCall {
    val dto = client.get("users/$id").body<UserDto>()
    dto.toDomain()
}
```

### Phase 3: Configuration (Centralized)
*   **ContentNegotiation**: JSON (Kotlinx Serialization).
*   **Logging**: Napier.
*   **Timeouts**: `HttpTimeout` plugin.

## 2. Mandatory Patterns

### Ktor Client
- **ALWAYS** use `client.get()`, `client.post()`.
- **ALWAYS** use `body<T>()` for deserialization.
- **NEVER** parse JSON manually.

### Error Handling
- **ALWAYS** wrap calls in a safe block catching:
    *   `ClientRequestException`
    *   `ServerResponseException`
    *   `IOException`
- **ALWAYS** map to `Resource.Error`.

### Serialization
- **ALWAYS** use `@Serializable` (kotlinx.serialization).
- **ALWAYS** set `ignoreUnknownKeys = true` in global config.

## 3. Verification Checklist

- [ ] **Library**: Is Ktor Client used?
- [ ] **Injection**: Is `HttpClient` injected via Koin?
- [ ] **Safety**: Are exceptions caught?
- [ ] **Logging**: Is Napier used for logging?

**IF ANY CHECK FAILS: STOP. REFACTOR IMMEDIATELY.**
