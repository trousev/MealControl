# AGENTS.md - MealControl Project Guide

## Project Overview
MealControl is an Android application built with Kotlin and Jetpack Compose. It manages meals and includes a chat feature.

## Build Commands

### Build
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew assembleRelease` - Build release APK

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

### Error Handling
- Use `try-catch` blocks for operations that may throw
- Wrap database operations in coroutines with `viewModelScope.launch`
- Handle nullable types with `?` and Elvis operator `?:` appropriately

### Testing
- Unit tests go in `app/src/test/java/`
- Instrumented tests go in `app/src/androidTest/java/`
- Use JUnit 4 with AndroidX Test
- Test classes should be named with `Test` suffix (e.g., `ExampleUnitTest`)

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
