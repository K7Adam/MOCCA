---
name: android-release-protocol
description: Use when preparing for release, configuring CI/CD, or setting up build variants. MANDATORY for production builds. Enforces R8/ProGuard, Signing, and Versioning.
---

# Android Release Protocol

## ⚠️ CRITICAL: Production Readiness

You are a Release Engineer. You do not ship debug builds. You do not leak secrets. You ensure **integrity and optimization**.

**Shipping a debug key or unoptimized code is a critical failure.**

## 1. The Configuration Protocol (MANDATORY)

You MUST configure builds in this order:

### Phase 1: Build Variants
1.  **Debug**: `debuggable true`, `minificationEnabled false`.
2.  **Release**: `debuggable false`, `minificationEnabled true` (R8).
    *   Constraint: Keep ProGuard rules minimal and documented.

### Phase 2: Signing Config
1.  **Keystore**: NEVER check `.jks` into git.
2.  **Properties**: Read `storePassword`/`keyPassword` from environment variables or `local.properties`.
    *   Constraint: CI pipeline must inject these securely.

### Phase 3: Versioning Strategy
1.  **Version Code**: Monotonic integer (Automate via CI timestamp or commit count).
2.  **Version Name**: Semantic Versioning (`major.minor.patch`).

## 2. Mandatory Optimization Patterns

### Code Shrinking (R8)
- **ALWAYS** enable `isMinifyEnabled = true` for release.
- **ALWAYS** enable `isShrinkResources = true` for release.
- **Action**: Verify `proguard-rules.pro` keeps reflection-based models (Serialization).

### Manifest Cleanup
- **ALWAYS** remove `android:debuggable` (handled by Gradle).
- **ALWAYS** verify permissions are minimal (Remove unused).

## 3. Pre-Flight Checklist (The Gatekeeper)

Before handing off an APK/AAB:

1.  **Lint Check**: Run `./gradlew lintRelease`. Must be 0 errors.
2.  **Test Suite**: Run `./gradlew testReleaseUnitTest`. Must pass.
3.  **Smoke Test**: Install Release build on Emulator.
    *   *Verify*: Does it crash on startup? (R8 issues).
    *   *Verify*: Do network calls work? (ProGuard serialization issues).

## 4. Verification

- [ ] **Debuggable**: Is it false?
- [ ] **Minified**: Is R8 enabled?
- [ ] **Secrets**: Are keys/passwords NOT in source code?
- [ ] **Logging**: Are `Log.d` calls stripped or disabled?

**IF ANY CHECK FAILS: STOP. FIX BUILD CONFIG.**
