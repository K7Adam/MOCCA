---
name: android-architecture
description: Use when creating new features or defining app structure. MANDATORY for all feature implementation. Enforces Voyager ScreenModel, Koin DI, and Clean Architecture.
---

# Android Architecture Protocol (KMP/Voyager/Koin)

## ⚠️ CRITICAL: Stack Alignment

You are a Kotlin Multiplatform Architect. You do **NOT** use Hilt or Android ViewModel.
You use **Koin** and **Voyager**.

**Using Hilt or Jetpack Navigation is a critical failure.**

## 1. The Implementation Order (MANDATORY)

### Phase 1: Domain Layer (Pure Kotlin)
*Dependencies: None*
1.  **Models**: Data classes (e.g., `User`).
    *   Constraint: `@Serializable` if needed for Ktor/KVS.
2.  **Repository Interface**: `interface UserRepository`.
    *   Constraint: Return `Flow<Resource<T>>` or `suspend` Result.

### Phase 2: Data Layer (Implementation)
*Dependencies: Ktor, SQLDelight*
1.  **Repository Implementation**: `class UserRepositoryImpl : UserRepository`.
    *   Action: Inject `HttpClient` and `Database` via Koin.
2.  **Koin Module**: Define in `appModule` or feature module.
    *   Code: `singleOf(::UserRepositoryImpl) { bind<UserRepository>() }`.

### Phase 3: Presentation Layer (Voyager)
*Dependencies: Voyager, Koin*
1.  **ScreenModel**: Extend `StateScreenModel<UiState>(initialState)`.
    *   *Constraint*: Do NOT extend `androidx.lifecycle.ViewModel`.
    *   *Action*: Inject UseCases/Repositories via constructor.
2.  **Screen**: Implement `Screen` interface.
    *   *Code*:
        ```kotlin
        class FeatureScreen : Screen {
            @Composable
            override fun Content() {
                val screenModel = koinScreenModel<FeatureScreenModel>()
                val state by screenModel.state.collectAsState()
                
                FeatureContent(state = state)
            }
        }
        ```

## 2. Mandatory Patterns

### Dependency Injection (Koin)
- **ALWAYS** use constructor injection.
- **ALWAYS** define modules in `di/Modules.kt`.
- **NEVER** use field injection (`by inject()`) inside classes (except legacy Android components).

### Navigation (Voyager)
- **ALWAYS** implement `Screen` interface.
- **ALWAYS** use `LocalNavigator.currentOrThrow`.
- **NEVER** use `NavController` or Fragment transactions.

### State Management
- **ALWAYS** use `StateScreenModel` from Voyager.
- **ALWAYS** expose `state: StateFlow<UiState>`.
- **NEVER** use `LiveData`.

## 3. Verification Checklist

- [ ] **Architecture**: Is `ScreenModel` used instead of `ViewModel`?
- [ ] **DI**: Is Koin used (no `@Inject` annotations)?
- [ ] **Navigation**: Is Voyager `Screen` interface used?
- [ ] **Imports**: Are `androidx.lifecycle` imports minimized?

**IF ANY CHECK FAILS: STOP. REFACTOR IMMEDIATELY.**
