# AGENTS.md - MealControl Project Guide

## Project Overview
MealControl is an Android application built with Kotlin and Jetpack Compose. It manages meals and includes a chat feature.

## Build Commands

### Build
- `./script/build` - Build debug APK (recommended, handles Java/Android SDK setup)
- `./gradlew assembleDebug` - Build debug APK (requires manual JAVA_HOME/ANDROID_HOME setup)
- `./gradlew assembleRelease` - Build release APK

**IMPORTANT**: Always use `./script/build` for compilation. Do NOT attempt to install any packages, find custom Java installations, or set up the environment manually.

### Build Environment
All build operations require Java and Android SDK to be configured via environment variables. These MUST be set in a `.env` file in the project root:
- `JAVA_HOME` - Path to Java installation
- `ANDROID_ROOT` - Path to Android SDK

If the `.env` file is missing or does not contain valid paths, inform the user that building is not possible and ask them to set up the environment.

### Run Tests
- `./gradlew test` - Run unit tests
- `./gradlew testDebugUnitTest` - Run unit tests for debug variant
- `./gradlew testReleaseUnitTest` - Run unit tests for release variant
- `./gradlew connectedAndroidTest` - Run instrumented tests (requires emulator/device)
- `./gradlew testDebugUnitTest --tests "pro.trousev.mealcontrol.ExampleUnitTest"` - Run a single unit test class

### Lint
- `./gradlew lint` - Run lint analysis
- `./gradlew lintDebug` - Run lint on debug variant

### Other Commands
- `./gradlew clean` - Clean build artifacts
- `./gradlew build` - Full build with tests and lint

## Code Style Guidelines

### Language Version
- Kotlin 1.9.x with official code style
- Target Java 11 compatibility

### Package Structure
```
pro.trousev.mealcontrol/
  data/
    local/
      dao/         # Room DAOs
      entity/      # Room entities
    repository/    # Data repositories
  ui/
    theme/         # Compose theme (Color, Type, Theme)
    chat/          # Chat UI screens
    meals/         # Meals UI screens
    scanmeal/      # Camera/photo capture screens
  viewmodel/       # ViewModels
  util/            # Utility classes
```

### Naming Conventions
- **Files**: PascalCase (e.g., `MealViewModel.kt`, `ScanMealScreen.kt`)
- **Classes/Interfaces**: PascalCase (e.g., `MealEntity`, `MealRepository`)
- **Functions**: camelCase (e.g., `saveMeal`, `loadMeals`)
- **Variables/Properties**: camelCase (e.g., `mealViewModel`, `currentTab`)
- **Enums**: PascalCase with UPPER_SNAKE_CASE values (e.g., `AppTab.SCAN_MEAL`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `const val DEFAULT_TIMEOUT = 30`)

### Import Organization
Group imports in this order (no blank lines between groups):
1. Android framework imports (`android.*`)
2. Kotlin standard library (`kotlin.*`)
3. Third-party libraries (`androidx.*`, `com.*`)
4. Internal project imports (`pro.trousev.mealcontrol.*`)

Example:
```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.lifecycle.viewmodel.compose.viewModel
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.viewmodel.MealViewModel
```

### Compose Guidelines
- Use `@Composable` annotation for all composable functions
- Name composable functions with PascalCase (e.g., `MealControlApp`)
- Use `viewModel()` from `androidx.lifecycle.viewmodel.compose` for ViewModel injection
- Use `rememberSaveable` for UI state that survives configuration changes
- Use `MutableStateFlow` with `asStateFlow` for ViewModel state exposure
- Prefer `StateFlow` over `LiveData` in ViewModels

### Room Database
- Entities use `@Entity` annotation with explicit table names
- DAOs use `@Dao` annotation
- Use `data class` for entities with default values where appropriate
- Use `@PrimaryKey(autoGenerate = true)` for auto-incrementing IDs

### Database Migrations
When adding new entities or modifying existing schema:
1. **Always bump the version**: Increment `version` in `@Database` annotation
2. **ALWAYS create a proper migration** - Never use `fallbackToDestructiveMigration()` in production, as it will WIPE ALL USER DATA
3. **Test on device**: Schema changes require uninstall/reinstall or proper migration on device

**Migration Example**:
```kotlin
@Database(
    entities = [...],
    version = 3,  // Always increment when schema changes
    exportSchema = false
)
abstract class MealControlDatabase : RoomDatabase() {
    companion object {
        // Define migrations for each version increment
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns with default values for existing rows
                database.execSQL("ALTER TABLE meal_components ADD COLUMN proteinGrams INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE meal_components ADD COLUMN fatGrams INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE meal_components ADD COLUMN carbGrams INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): MealControlDatabase {
            return Room.databaseBuilder(...)
                .addMigrations(MIGRATION_2_3)  // Always add migrations
                .build()
        }
    }
}
```

**Important**: Using `fallbackToDestructiveMigration()` should ONLY be used during initial development when you don't care about user data. For any released version, you MUST create proper `Migration` classes to preserve user data.

### Error Handling
- Use `try-catch` blocks for operations that may throw
- Wrap database operations in coroutines with `viewModelScope.launch`
- Handle nullable types with `?` and Elvis operator `?:` appropriately

### Testing
- **Every feature must include tests** - Never add new functionality without writing corresponding tests
- Unit tests go in `app/src/test/java/`
- Instrumented tests go in `app/src/androidTest/java/`
- Use JUnit 4 with AndroidX Test
- Test classes should be named with `Test` suffix (e.g., `ExampleUnitTest`)
- Test ViewModels to verify state changes, calculations, and business logic
- Test repositories to verify data operations
- Run tests before committing: `./script/test`

### Gradle Configuration
- Uses Kotlin DSL (`*.gradle.kts` files)
- Version catalog in `gradle/libs.versions.toml` for dependency management
- KSP (Kotlin Symbol Processing) for Room annotation processing
- AGP (Android Gradle Plugin) 8.x compatibility

### Key Dependencies
- Jetpack Compose (BOM for version management)
- Room Database with KSP
- CameraX for photo capture
- Coil for image loading
- Material 3 for UI components

## Architecture Pattern
- **MVVM** (Model-View-ViewModel)
- **Repository Pattern** for data access
- **Clean separation**: UI layer → ViewModel → Repository → DAO → Entity
