package pro.trousev.mealcontrol.ui.meals

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import pro.trousev.mealcontrol.data.local.entity.MealWithComponents
import pro.trousev.mealcontrol.viewmodel.MealViewModel
import pro.trousev.mealcontrol.viewmodel.SettingsViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MealsScreen(
    mealViewModel: MealViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onAddMealClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meals by mealViewModel.meals.collectAsState()
    val settingsFormState by settingsViewModel.formState.collectAsState()

    val todayMeals = meals.filter { meal ->
        val mealDate = Calendar.getInstance().apply {
            timeInMillis = meal.meal.timestamp
        }
        val today = Calendar.getInstance()
        mealDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                mealDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    val consumedCalories = todayMeals.flatMap { it.components }.sumOf { it.calories }
    val consumedProtein = todayMeals.flatMap { it.components }.sumOf { it.proteinGrams }
    val consumedFat = todayMeals.flatMap { it.components }.sumOf { it.fatGrams }
    val consumedCarbs = todayMeals.flatMap { it.components }.sumOf { it.carbGrams }

    val targetCalories = settingsFormState.calculation?.dailyCalories ?: 0
    val targetProtein = settingsFormState.calculation?.proteinGrams ?: 0
    val targetFat = settingsFormState.calculation?.fatGrams ?: 0
    val targetCarbs = settingsFormState.calculation?.carbGrams ?: 0

    val remainingCalories = (targetCalories - consumedCalories).coerceAtLeast(0)
    val remainingProtein = (targetProtein - consumedProtein).coerceAtLeast(0)
    val remainingFat = (targetFat - consumedFat).coerceAtLeast(0)
    val remainingCarbs = (targetCarbs - consumedCarbs).coerceAtLeast(0)

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMealClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add meal"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DailyBudgetCard(
                    remainingCalories = remainingCalories,
                    targetCalories = targetCalories,
                    remainingProtein = remainingProtein,
                    targetProtein = targetProtein,
                    remainingFat = remainingFat,
                    targetFat = targetFat,
                    remainingCarbs = remainingCarbs,
                    targetCarbs = targetCarbs
                )
            }

            if (meals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No meals yet. Tap + to add one!",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(meals) { mealWithComponents ->
                    MealCard(mealWithComponents = mealWithComponents)
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun DailyBudgetCard(
    remainingCalories: Int,
    targetCalories: Int,
    remainingProtein: Int,
    targetProtein: Int,
    remainingFat: Int,
    targetFat: Int,
    remainingCarbs: Int,
    targetCarbs: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Today's Budget",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$remainingCalories / $targetCalories kcal left",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$remainingProtein / ${targetProtein}g protein left",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$remainingFat / ${targetFat}g fat left",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$remainingCarbs / ${targetCarbs}g carbs left",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MealCard(
    mealWithComponents: MealWithComponents,
    modifier: Modifier = Modifier
) {
    val meal = mealWithComponents.meal
    val components = mealWithComponents.components
    val totalCalories = components.sumOf { it.calories }
    val totalProtein = components.sumOf { it.proteinGrams }
    val totalFat = components.sumOf { it.fatGrams }
    val totalCarbs = components.sumOf { it.carbGrams }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(File(meal.photoUri)),
                contentDescription = "Meal photo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = meal.description,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateFormat.format(Date(meal.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$totalCalories kcal (P:${totalProtein}g F:${totalFat}g C:${totalCarbs}g)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
