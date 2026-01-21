---
name: android-data-persistence
description: Use when implementing local database. MANDATORY for data layer. Enforces SQLDelight, .sq files, and Koin injection.
---

# Android Data Persistence Protocol (SQLDelight)

## ⚠️ CRITICAL: SQLDelight Usage

You are a KMP Data Engineer. You use **SQLDelight**, not Room.

**Using Room annotations (@Entity, @Dao) is a critical failure.**

## 1. The Implementation Order (MANDATORY)

### Phase 1: Schema Definition (.sq)
1.  **Create File**: `src/commonMain/sqldelight/package/Database.sq`
2.  **Define Table**:
    ```sql
    CREATE TABLE user (
        id TEXT NOT NULL PRIMARY KEY,
        name TEXT NOT NULL
    );
    ```
3.  **Define Queries**:
    ```sql
    selectAll:
    SELECT * FROM user;
    
    insert:
    INSERT OR REPLACE INTO user(id, name) VALUES ?;
    ```

### Phase 2: Driver Configuration
*   **Android**: `AndroidSqliteDriver`.
*   **Koin**: Provide `SqlDriver` and the generated `Database` class.

### Phase 3: Repository Integration
*   **Inject**: `Database` class.
*   **Usage**: `database.userQueries.selectAll().executeAsList()`.
*   **Observing**: `asFlow().mapToList(Dispatchers.IO)`.

## 2. Mandatory Patterns

### SQLDelight
- **ALWAYS** write SQL in `.sq` files.
- **NEVER** write SQL in Kotlin strings.
- **ALWAYS** use `executeAsList()` or `executeAsOne()`.

### Threading
- **ALWAYS** use `Dispatchers.IO` for DB operations.
- **ALWAYS** consume Flows on IO dispatcher.

## 3. Verification Checklist

- [ ] **Library**: Is SQLDelight used?
- [ ] **Files**: Are tables defined in `.sq` files?
- [ ] **Injection**: Is the Database injected via Koin?
- [ ] **Flows**: Are Flow extensions used for reactive queries?

**IF ANY CHECK FAILS: STOP. REFACTOR IMMEDIATELY.**
