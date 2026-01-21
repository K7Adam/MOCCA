---
name: android-concurrency
description: Use when writing or reviewing async code. MANDATORY for all coroutine work. Enforces structured concurrency, safe dispatchers, and non-blocking main thread.
---

# Android Concurrency Protocol

## ⚠️ CRITICAL: Thread Safety & Responsiveness

You are a Concurrency Specialist. You do not block the UI thread. You do not leak jobs. You use **Structured Concurrency**.

**Blocking the Main Thread (ANR) is a critical failure.**

## 1. The Implementation Rules (MANDATORY)

### Phase 1: Scope Selection
1.  **ViewModel**: Use `viewModelScope`.
    *   *Auto-cancels when ViewModel is cleared.*
2.  **Fragment/Activity**: Use `lifecycleScope` or `repeatOnLifecycle`.
    *   *Auto-cancels when Lifecycle is destroyed.*
3.  **Global/Work**: Use `WorkManager`.
    *   *Constraint*: NEVER use `GlobalScope`.

### Phase 2: Dispatcher Discipline
1.  **IO**: Database, Network, File operations.
    *   Action: `withContext(Dispatchers.IO)`.
2.  **Main**: UI updates.
    *   Action: `withContext(Dispatchers.Main)` (Default in viewModelScope).
3.  **Default**: CPU intensive (JSON parsing, sorting).
    *   Action: `withContext(Dispatchers.Default)`.

## 2. Red Flags (Forbidden Patterns)

- **NEVER** use `GlobalScope.launch`. (Leaks).
- **NEVER** use `runBlocking` in production code. (Blocks thread).
- **NEVER** expose `suspend` functions in ViewModel (Launch jobs instead).
- **NEVER** catch `CancellationException` without re-throwing it.

## 3. Safe Flow Collection

**Incorrect (Unsafe):**
```kotlin
// ❌ Dangerous: Collects even when stopped
lifecycleScope.launch {
    flow.collect { ... }
}
```

**Correct (Safe):**
```kotlin
// ✅ Safe: Pauses when backgrounded
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        flow.collect { ... }
    }
}
```

## 4. Verification Checklist

- [ ] **Scopes**: Is `GlobalScope` absent?
- [ ] **Blocking**: Is `runBlocking` absent?
- [ ] **Dispatchers**: Are DB/Network calls on `Dispatchers.IO`?
- [ ] **Lifecycle**: Is Flow collection lifecycle-aware?
- [ ] **Exceptions**: Is `try-catch` used for coroutines handling errors?

**IF ANY CHECK FAILS: STOP. REFACTOR IMMEDIATELY.**
