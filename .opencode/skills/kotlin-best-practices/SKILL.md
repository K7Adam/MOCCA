---
name: kotlin-best-practices
description: Implement Kotlin Multiplatform features in MOCCA using existing MVI, repository, and dependency-injection conventions.
license: MIT
compatibility: opencode
metadata:
  platform: kotlin-multiplatform
  architecture: mvi
---

## What this skill does

- Applies MOCCA architecture patterns (`ScreenModel -> StateFlow -> UI`, repository-first data access).
- Enforces existing network rule: consumers do not hold `HttpClient`; use `ApiExecutor.execute {}`.
- Keeps domain models immutable and repository flows offline-first.

## When to use

Use for Kotlin code changes in:

- `composeApp/src/commonMain/kotlin/com/mocca/app/data/**`
- `composeApp/src/commonMain/kotlin/com/mocca/app/domain/**`
- `composeApp/src/commonMain/kotlin/com/mocca/app/ui/**`

## Implementation rules

1. Match nearby naming, nullability, and coroutine patterns before adding new code.
2. Prefer small pure helper functions over deep nested branching.
3. Keep business logic in repositories/screen models, not composables.
4. Preserve thread-safety and avoid blocking calls on main dispatcher.
5. Validate with diagnostics and relevant Gradle tasks after edits.

## Done criteria

- Modified Kotlin files are diagnostics-clean.
- New behavior is covered by existing or added test paths where applicable.
- No violations of MOCCA anti-patterns in `AGENTS.md`.
