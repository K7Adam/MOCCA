# SETTINGS SUBTREE KNOWLEDGE BASE

**Updated:** 2026-06-28
**Scope:** `com.mocca.app.ui.screens.settings`

## OVERVIEW
High-complexity settings subtree covering server profiles, provider auth, project path, server config display, user preferences, updates, and feature flags.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Main settings screen | `SettingsScreen.kt` | Assembles all sections in a long `LazyColumn` |
| Settings state/logic | `SettingsScreenModel.kt` | Server CRUD, provider auth, preferences, updates, project path |
| Shared settings widgets | `SettingsComponents.kt` | Server cards, edit dialog, row primitives |
| Feature flags screen | `FeatureFlagsScreen.kt` | Server-side config UI |
| Feature flags logic | `FeatureFlagsScreenModel.kt` | Loads/saves global app config |
| Section components | `sections/*.kt` | Dedicated sections for app config, appearance, updates, experimental, notifications, privacy, project, provider auth |

## STRUCTURE
```
settings/
├── SettingsScreen.kt
├── SettingsScreenModel.kt
├── SettingsComponents.kt
├── FeatureFlagsScreen.kt
├── FeatureFlagsScreenModel.kt
└── sections/
    ├── AppConfigSection.kt
    ├── AppearanceSection.kt
    ├── AppUpdatesSection.kt
    ├── ExperimentalSection.kt
    ├── NotificationsSection.kt
    ├── PrivacySecuritySection.kt
    ├── ProjectSection.kt
    └── ProviderAuthSection.kt
```

## CONVENTIONS
- Screen stays composition-only; section files hold UI chunks, `SettingsScreenModel` owns state mutations
- Provider auth is a mixed-mode flow: OAuth launch + manual key entry + auth removal
- Project path editing is part of settings, not a separate workspace screen concern
- Server-side config messaging matters: provider/model defaults come from OpenCode config, not local-only UI state
- Preferences must update both `SettingsRepository` and `PreferencesManager` to keep reactive UI in sync
- Dead controls have been pruned: compact mode, font scale, data saver, screen security, clear cache on exit, and "show thinking" toggles were removed because they did not change real behavior. Do not re-add them without wiring them to actual functionality
- Sections were consolidated to remove duplicates; every visible control must do something real

## ANTI-PATTERNS
- Do not move business logic into section composables
- Do not let local UI state become the only source of truth for persisted preferences
- Do not assume provider auth methods are already loaded; the subtree lazily requests them per provider expansion
- Do not remove the server-side config note unless that product flow changes everywhere else too

## TESTING NOTES
- Settings access is exercised from Maestro via `maestro-workspace/flows/navigation/navigate_to_settings.yaml`
- Feature flag and provider-auth changes should preserve dashboard-to-settings navigation and screenshot/catalog flows
