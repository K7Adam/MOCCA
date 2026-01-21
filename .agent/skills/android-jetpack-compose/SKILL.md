---
name: android-jetpack-compose
description: Use when building UI. MANDATORY for all UI work. Enforces Voyager Screens, State Hoisting, and Material 3.
---

# Jetpack Compose Protocol (Voyager)

## ⚠️ CRITICAL: Declarative UI & Voyager

You are a Compose UI Specialist. You build **stateless components** and use **Voyager** for navigation.

## 1. The Construction Order (MANDATORY)

### Phase 1: State Definition
1.  **UI State**: Immutable data class (e.g., `LoginUiState`).

### Phase 2: Content Composable (Stateless)
1.  **Function**: `fun LoginContent(state: LoginUiState, onAction: () -> Unit)`
    *   *Constraint*: PURE UI. No ViewModel references.

### Phase 3: Screen (Voyager)
1.  **Class**: `class LoginScreen : Screen`
2.  **Content Method**:
    ```kotlin
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<LoginScreenModel>()
        val state by screenModel.state.collectAsState()
        
        LoginContent(
            state = state,
            onLogin = { navigator.push(HomeScreen()) }
        )
    }
    ```

## 2. Mandatory Patterns

### Navigation (Voyager)
- **ALWAYS** use `LocalNavigator.currentOrThrow`.
- **ALWAYS** push new `Screen` instances.
- **NEVER** use Jetpack Navigation `NavController`.

### State
- **ALWAYS** collect state from `ScreenModel`.
- **NEVER** hold state in the Screen class itself (it's serialized).

## 3. Verification Checklist

- [ ] **Navigation**: Is Voyager used?
- [ ] **Screen**: Does it implement `Screen`?
- [ ] **DI**: Is `koinScreenModel` used?
- [ ] **Statelessness**: Is the UI logic isolated in a stateless function?

**IF ANY CHECK FAILS: STOP. REFACTOR IMMEDIATELY.**
