---
name: bug-diagnosis
description: Systematic bug diagnosis with diagnostic-first approach, hypothesis generation, and root cause documentation. Use when debugging complex issues, tracing errors, or implementing fixes methodically.
---

# Bug Diagnosis Skill

Intelligently diagnose and fix bugs using a systematic, diagnostic-first approach.

## When to Use

- Debugging complex issues
- Tracing errors through multiple layers
- Implementing fixes with proper testing
- Documenting root causes

## Core Methodology

1. **Add diagnostic instrumentation** to capture current behavior
2. **Deep architecture analysis** to understand context
3. **Hypothesis generation** (5-7 possible sources, distill to 1-2)
4. **User confirmation** before implementing fix
5. **Root cause documentation** for future reference

## Execution Flow

```
User Reports Bug
       ↓
Read Architecture Context
       ↓
Add Diagnostic Logs
       ↓
Attempt to Reproduce
       ↓
Analyze Log Output
       ↓
Generate 5-7 Hypotheses
       ↓
Distill to 1-2 Most Likely
       ↓
Present Diagnosis to User
       ↓
User Confirms? → No → Add More Diagnostics
       ↓ Yes
Implement Fix
       ↓
Test Thoroughly
       ↓
Remove Diagnostic Logs
       ↓
Document Root Cause
```

## Phase 1: Context Gathering

### Gather Bug Context
- Bug description and steps to reproduce
- Expected vs actual behavior
- Error messages/stack traces
- Environment (dev/staging/prod)

### Architecture Analysis
- **Database Layer**: ERD, constraints, query patterns
- **Service Layer**: DTOs, business logic, error handling
- **API Layer**: Endpoints, middleware, authentication
- **UI Layer**: Component flows, state management

## Phase 2: Diagnostic Instrumentation

**CRITICAL**: Add instrumentation FIRST, before attempting fixes.

### Database Layer
```typescript
console.log('[DEBUG] Query:', { query, params, timestamp });
console.log('[DEBUG] Result:', { rowCount, executionTime });
```

### Service Layer
```typescript
console.log('[DEBUG] Service:', { 
  service: 'UserService', 
  method: 'updateProfile',
  input: sanitizeForLog(input)
});
```

### API Layer
```typescript
console.log('[DEBUG] API:', { 
  method: req.method, 
  path: req.path, 
  body: sanitizeForLog(req.body)
});
```

### UI Layer
```typescript
console.log('[DEBUG] State:', { 
  component: 'UserProfile',
  prevState, nextState, trigger
});
```

## Phase 3: Hypothesis Generation

Generate 5-7 possible causes, then distill:

### Distillation Criteria
1. **Evidence Strength**: How much diagnostic data supports this?
2. **Architecture Alignment**: Does data flow confirm this?
3. **Data Flow Analysis**: Does timeline support this?
4. **Probability**: Based on error type and context

## Phase 4: User Confirmation (MANDATORY)

**STOP** - Do NOT implement fix until user confirms.

```markdown
## 🔍 Bug Diagnosis Report

### Bug Summary
**Issue**: [Description]
**Frequency**: [Reproducibility]
**Impact**: [Severity]

### 🎯 Root Cause Identified
**Primary Cause**: [Name] ([Confidence]% confidence)
**Explanation**: [Clear explanation]
**Evidence**: [List of evidence points]

### 🔧 Proposed Fix
**Location**: [File path]
**Change**: [Description]

### ❓ Confirmation Required
1. ✅ Yes, proceed with fix
2. 🔄 Need more data
3. ❌ Diagnosis seems wrong
4. 💬 Have additional context
```

## Phase 5: Implement & Test

1. Create fix implementation plan
2. Implement fix with tests
3. Remove diagnostic logs
4. Verify bug resolution
5. Document root cause

## Key Principles

1. **Diagnostic-First**: ALWAYS add instrumentation before fixing
2. **Hypothesis-Driven**: Generate multiple theories, validate with data
3. **User Confirmation**: NEVER fix without explicit approval
4. **Architecture-Aware**: Use architecture docs for deep context
5. **Evidence-Based**: All conclusions backed by diagnostic data
6. **Thorough Testing**: Comprehensive tests for fix and edge cases
7. **Clean Cleanup**: Remove all temporary diagnostic code
