---
name: android-data-persistence
description: Use when implementing local database, file storage, or preferences. MANDATORY for all data layer work. Enforces Repository pattern, Room/SQLDelight usage, and Encryption.
---

# Android Data Persistence Protocol

## ⚠️ CRITICAL: Data Integrity & Security

You are a Data Engineer. You do not write "quick SQL queries". You build **robust, secure, and performant** data layers.

**Leaking sensitive data or blocking the Main Thread is a critical failure.**

## 1. The Implementation Order (MANDATORY)

You MUST implement data persistence in this exact order:

### Phase 1: Schema Definition
1.  **Entity/Table**: Define the data structure.
    *   Constraint: Annotated with `@Entity` (Room) or `.sq` file (SQLDelight).
    *   Constraint: Primary keys MUST be immutable.
    *   **Optimization**: DEFINE INDEXES for all query columns (`WHERE`, `JOIN`, `ORDER BY`).

### Phase 2: DAO/Queries
1.  **Interface**: Define data access methods.
    *   Constraint: ALL return types MUST be `suspend` or `Flow`.
    *   Constraint: NO blocking calls allowed.

### Phase 3: Repository (The Gatekeeper)
1.  **Implementation**: Map Database Entities to Domain Models.
    *   Action: Handle database exceptions.
    *   Action: Expose clean `Flow<Resource<T>>` to Domain.

### Phase 4: Migration Strategy
1.  **Version Bump**: Increment database version.
2.  **Migration Script**: Write the explicit SQL migration.
    *   Action: Verify data integrity after migration.

## 2. Mandatory Patterns

### Database (Room/SQLDelight)
- **ALWAYS** use `@Transaction` for multi-step operations.
- **NEVER** expose Entities/DTOs to the UI layer.
- **ALWAYS** index foreign keys and query columns.

### Secure Storage (EncryptedSharedPreferences/DataStore)
- **ALWAYS** use `EncryptedSharedPreferences` for tokens/secrets.
- **NEVER** store plain-text passwords or auth tokens in standard `SharedPreferences`.
- **ALWAYS** use `DataStore` (Proto/Preferences) for complex user settings.

### File Storage
- **ALWAYS** use `Context.filesDir` or `Context.cacheDir`.
- **NEVER** hardcode paths (`/sdcard`).
- **ALWAYS** scope file access to `Dispatchers.IO`.

## 3. Performance Optimization (Index Audit)

Before finalizing any schema change, perform this audit:

1.  **Query Analysis**: List all queries running against the table.
2.  **Index Check**:
    *   Is there an index on columns used in `WHERE`?
    *   Is there an index on foreign keys used in `JOIN`?
    *   Is there an index on `ORDER BY` columns?
3.  **N+1 Check**: Ensure Lists are fetched with `@Relation` or single query, not loops.

## 4. Verification Checklist

- [ ] **Thread Safety**: Are ALL DAO methods `suspend` or `Flow`?
- [ ] **Encryption**: Are auth tokens stored in EncryptedSharedPreferences?
- [ ] **Indexing**: Are all `WHERE/JOIN` columns indexed?
- [ ] **Mapping**: Is the Repository mapping Entities -> Domain Models?
- [ ] **Migration**: Is there a migration path for this schema change?

**IF ANY CHECK FAILS: STOP. REFACTOR IMMEDIATELY.**
