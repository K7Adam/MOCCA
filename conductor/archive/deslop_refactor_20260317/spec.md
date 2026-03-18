# Track Specification: Deslop and Refactor MOCCA

## Overview
This track focuses on a comprehensive refactoring and "deslopifying" of the MOCCA codebase and application. The goal is to eliminate technical debt ("AI slop"), improve performance (specifically navigation fluidity), optimize battery usage, and strictly adhere to the latest 2026 Material 3 Expressive guidelines.

## Objectives
1.  **Deslopification:** Research and eliminate redundant, unoptimized, or inconsistent code patterns typical of LLM-generated code.
2.  **Performance Optimization:** Achieve consistent 60/120 FPS during navigation and UI transitions by optimizing the Material 3 motion system.
3.  **Resource Efficiency:** Improve the app's battery-usage through CPU and network optimization.
4.  **M3 Expressive Excellence:** Enhance the UI with adaptive motion, expressive polish, and dynamic components following 2026 best practices.

## Functional Requirements
- **Refactor Navigation:** Implement optimized navigation transitions that eliminate frame drops.
- **UI Component Cleanup:** Consolidate and refactor existing UI components for better maintainability and visual consistency.
- **Resource Profiling:** Implement instrumentation to measure and improve battery and CPU usage.

## Non-Functional Requirements
- **60/120 FPS:** All navigation transitions must run at a smooth frame rate.
- **Battery Impact:** Reduce active energy consumption of the app.
- **Code Maintainability:** Improve the codebase structure and readability.

## Research Items
- **"AI Slop" Analysis:** Identify and remove common LLM-generated code anti-patterns.
- **KMP/Compose Perf 2026:** Study latest 2026 best practices for Kotlin Multiplatform and Compose performance.
- **Latest M3 Guidelines:** Align the app's design with the latest Material 3 Expressive motion and style system.

## Acceptance Criteria
- [ ] No detectable frame drops during common navigation flows (Chat <-> Terminal <-> Git).
- [ ] Codebase is free of identified "slop" patterns.
- [ ] All core UI components strictly follow Material 3 Expressive 2026 guidelines.
- [ ] Measured improvement in battery usage compared to the baseline.

## Out of Scope
- Adding new functional features (e.g., new backend integrations) not related to refactoring or performance.
