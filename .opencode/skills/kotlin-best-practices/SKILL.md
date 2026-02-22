---
name: kotlin-best-practices
description: Best practices for Kotlin Multiplatform development with Compose Multiplatform
related_skills:
  - taste-skill-compose
  - taste-skill
  - android-mcp
---

# Kotlin Multiplatform Best Practices

## RELATED SKILLS

**This skill focuses on architecture and code patterns. For UI/UX design decisions or device testing, see:**

| Skill | Purpose | When to Use |
|-------|---------|-------------|
| **taste-skill-compose** | UI/UX design rules, animations, typography, color, layout | When building Compose UI components, screens, or implementing visual design |
| **taste-skill** | Original React/Next.js + Tailwind version | When building web interfaces or understanding design philosophy |
| **android-mcp** | Android device automation via ADB/MCP | When testing apps on emulators, automating UI interactions, capturing screenshots, or running ADB commands |

**UI/UX Concerns Delegated to taste-skill-compose:**
- Design variance levels (asymmetric vs symmetric layouts)
- Motion intensity levels (animation patterns)
- Visual density levels (spacing decisions)
- Typography hierarchy and font selection
- Color calibration and accent color usage
- Glassmorphic and liquid glass effects (via [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass))
- Staggered animations and spring physics
- Anti-patterns for generic "AI-looking" UI
- Interactive states (loading, empty, error)

## 1. KMP Architecture

### Source Set Structure
```
composeApp/
├── src/
│   ├── commonMain/kotlin/    # Shared code
│   ├── androidMain/kotlin/   # Android-specific
│   ├── desktopMain/kotlin/   # Desktop-specific (if needed)
│   └── commonTest/kotlin/    # Shared tests
```

### expect/actual Pattern
```kotlin
// commonMain
expect fun getPlatformName(): String
expect class PlatformContext

// androidMain
actual fun getPlatformName(): String = "Android"
actual class PlatformContext(val context: Context)
```

## 2. Null Safety

**Trust the Compiler.** Do not use unnecessary null-safety operators.

- **Bad**: `val x: String = "foo" ?: "bar"` (Redundant elvis)
- **Bad**: `val x: String = "foo"!!` (Unnecessary assertion)
- **Good**: `val x: String = "foo"`

For platform types from Java, check type inference first.

## 3. Coroutines & Concurrency

```kotlin
// Use appropriate dispatchers
withContext(Dispatchers.IO) { /* DB/Network */ }
withContext(Dispatchers.Default) { /* CPU-intensive */ }

// Flow for reactive data
fun observeData(): Flow<Resource<T>> = flow {
    emit(Resource.Loading)
    try {
        localDataSource.observe()
            .combine(remoteDataSource.fetch()) { local, remote ->
                mergeData(local, remote)
            }
            .collect { emit(Resource.Success(it)) }
    } catch (e: Exception) {
        emit(Resource.Error(e.message ?: "Unknown error"))
    }
}
```

## 4. Compose Multiplatform

### State Management
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    when (state) {
        is State.Loading -> LoadingIndicator()
        is State.Success -> Content(state.data)
        is State.Error -> ErrorMessage(state.message)
    }
}
```

### MVI Pattern
```kotlin
// State - Immutable data class
data class State(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Events - Sealed interface
sealed interface Event {
    data class LoadData(val force: Boolean = false) : Event
    data class SelectItem(val id: String) : Event
    data object Retry : Event
}

// Effects - Side effects
sealed interface Effect {
    data class Navigate(val route: String) : Effect
    data class ShowToast(val message: String) : Effect
}

// ViewModel
class MyViewModel : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    
    private val _effects = MutableSharedFlow<Effect>()
    val effects: SharedFlow<Effect> = _effects.asSharedFlow()
    
    fun onEvent(event: Event) {
        when (event) {
            is Event.LoadData -> loadData(event.force)
            is Event.SelectItem -> selectItem(event.id)
            Event.Retry -> retry()
        }
    }
}
```

## 5. Dependency Injection (Koin)

```kotlin
// Module definition
val appModule = module {
    // Singletons
    singleOf(::ApiClient)
    singleOf(::DataRepository)
    
    // ViewModels
    viewModelOf(::MyViewModel)
    
    // Platform-specific
    single<HttpClient> { createHttpClient(get()) }
}

// Platform-specific modules
// androidMain
val androidModule = module {
    single { AndroidContext(androidContext()) }
}

// Initialize
startKoin {
    androidContext(this@App)
    modules(appModule, androidModule)
}
```

## 6. SQLDelight

```kotlin
// .sq file
CREATE TABLE Item (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    createdAt INTEGER NOT NULL
);

selectAll:
SELECT * FROM Item ORDER BY createdAt DESC;

insert:
INSERT OR REPLACE INTO Item VALUES (?, ?, ?);

deleteById:
DELETE FROM Item WHERE id = ?;

// Repository
class ItemRepository(db: Database) {
    fun observeAll(): Flow<List<Item>> = 
        db.itemQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
    
    suspend fun insert(item: Item) = withContext(Dispatchers.IO) {
        db.itemQueries.insert(item.id, item.name, item.createdAt)
    }
}
```

## 7. Voyager Navigation

```kotlin
// Screen definition
class DetailScreen(val id: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<DetailViewModel>()
        
        // UI content
    }
}

// Navigation actions
navigator.push(DetailScreen(id))
navigator.pop()
navigator.popUntil { it == HomeScreen }
navigator.replace(NewScreen())

// Tab navigation
class MainTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = "Home",
            icon = painterResource(Res.drawable.home)
        )
    
    @Composable
    override fun Content() {
        // Tab content
    }
}
```

## 8. Resource Handling

```kotlin
// Compose Multiplatform Resources
// commonMain/resources/MR/values/strings.xml
// <resources>
//   <string name="app_name">MOCCA</string>
// </resources>

// Usage
Text(stringResource(MR.strings.app_name))
Image(painterResource(MR.drawable.logo), contentDescription = "Logo")
```

## 9. Testing

```kotlin
// Unit Test
class RepositoryTest {
    @Test
    fun `load data returns success`() = runTest {
        // Given
        val repo = DataRepository(fakeLocal, fakeRemote)
        
        // When
        val result = repo.loadData()
        
        // Then
        assertTrue(result.isSuccess)
    }
}

// Compose UI Test
@Test
fun myScreen_displaysLoading() {
    composeTestRule.setContent {
        MyScreen()
    }
    
    composeTestRule
        .onNodeWithText("Loading")
        .assertExists()
}
```

## 10. Anti-Patterns to Avoid

- **NEVER** use `RectangleShape` for interactive elements
- **NEVER** use relative paths - always use absolute paths
- **NEVER** block the main thread
- **NEVER** hold `HttpClient` references - use `ApiExecutor.execute {}`
- **DO NOT** add `iosMain` or `desktopMain` (Android-only for MOCCA)
- **DO NOT** use physical device for `android-mcp` tasks (emulator required)
- **DO NOT** ignore Detekt rules

## 11. Performance Tips

```kotlin
// Use remember for expensive calculations
val processedData = remember(input) { 
    expensiveOperation(input) 
}

// Use derivedStateOf for derived state
val filteredItems by remember { 
    derivedStateOf { items.filter { it.isActive } } 
}

// Lazy column with keys
LazyColumn {
    items(
        items = data,
        key = { it.id }
    ) { item ->
        ItemRow(item)
    }
}

// Stable annotations
@Stable
data class Item(
    val id: String,
    val name: String
)
```

## 12. Logging

Use `Napier` for logging (cross-platform):
```kotlin
Napier.i { "Info message" }
Napier.d { "Debug message" }
Napier.e { "Error message" }
```

Do NOT use `android.util.Log` directly.
