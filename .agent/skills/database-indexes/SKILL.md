---
name: database-indexes
description: Analyze SQL queries, recommend missing indexes, identify N+1 patterns for performance optimization. Use when investigating slow queries or auditing database performance.
---

# Database Index Analysis Skill

Scan codebase to identify missing database indexes that could improve query performance.

## When to Use

- Investigating slow database queries
- Optimizing application performance
- Auditing index coverage
- Identifying N+1 query patterns
- Preparing for scale

## Analysis Process

### 1. Query Discovery
- Scan all source files for database queries
- Identify ORM query patterns (Prisma, TypeORM, Sequelize, Room, etc.)
- Find raw SQL queries in the codebase
- Detect N+1 query patterns

### 2. Query Pattern Analysis

For each discovered query:
- Identify `WHERE` clause columns
- Find `JOIN` conditions
- Detect `ORDER BY` columns
- Analyze `GROUP BY` clauses
- Check for full table scans

### 3. Current Index Audit
- Parse database schema files
- List existing indexes
- Identify redundant indexes
- Check for unused indexes

### 4. Index Recommendations

For each recommendation, provide:

| Field | Description |
|-------|-------------|
| **Table** | Affected table name |
| **Columns** | Columns to index |
| **Type** | B-tree, Hash, GIN, GiST, etc. |
| **Rationale** | Why this index helps |
| **Query Impact** | Which queries benefit |
| **Estimated Improvement** | Performance gain estimate |

### 5. Priority Classification

- 🔴 **Critical**: Queries causing timeouts or severe slowdowns
- 🟡 **High**: Frequently executed queries without indexes
- 🟢 **Medium**: Occasional queries that could benefit
- ⚪ **Low**: Nice-to-have optimizations

## Output Format

```markdown
# Database Index Analysis Report

## Executive Summary
- Total queries analyzed: X
- Missing indexes identified: Y
- Critical issues: Z

## Critical Indexes (Implement Immediately)

### 1. [Table Name] - [Column(s)]

**Query Pattern:**
```sql
SELECT * FROM users WHERE email = $1
```

**Current Behavior:** Full table scan

**Recommendation:**
```sql
CREATE INDEX idx_users_email ON users(email);
```

**Expected Impact:** 100x improvement for user lookup queries

---

## Implementation Plan
1. [First index to create]
2. [Second index to create]

## Monitoring Recommendations
- Queries to monitor after index creation
- Performance metrics to track
```

## Common Index Patterns

### Single Column Index
```sql
CREATE INDEX idx_users_email ON users(email);
```

### Composite Index (Multi-Column)
```sql
CREATE INDEX idx_orders_user_date ON orders(user_id, created_at);
```

### Partial Index
```sql
CREATE INDEX idx_active_users ON users(email) WHERE is_active = true;
```

### Covering Index
```sql
CREATE INDEX idx_users_lookup ON users(email) INCLUDE (name, created_at);
```

## Best Practices

✅ Index columns used in WHERE clauses
✅ Index columns used in JOIN conditions
✅ Consider composite indexes for multi-column queries
✅ Order composite index columns by selectivity
✅ Don't over-index (impacts write performance)
✅ Monitor index usage and remove unused indexes
