package pro.trousev.mealcontrol.ui.meals

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
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
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealsScreen(
    mealViewModel: MealViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onAddMealClick: () -> Unit,
    onMealClick: (MealWithComponents) -> Unit,
    modifier: Modifier = Modifier
) {
    val mealsByDate by mealViewModel.mealsByDate.collectAsState()
    val availableDates by mealViewModel.availableDates.collectAsState()
    val currentPageIndex by mealViewModel.currentPageIndex.collectAsState()
    val settingsFormState by settingsViewModel.formState.collectAsState()

    val targetCalories = settingsFormState.calculation?.dailyCalories ?: 0
    val targetProtein = settingsFormState.calculation?.proteinGrams ?: 0
    val targetFat = settingsFormState.calculation?.fatGrams ?: 0
    val targetCarbs = settingsFormState.calculation?.carbGrams ?: 0

    val pagerState = rememberPagerState(
        initialPage = currentPageIndex,
        pageCount = { availableDates.size.coerceAtLeast(1) }
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        mealViewModel.setCurrentPageIndex(pagerState.currentPage)
    }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            val dateTimestamp = availableDates.getOrElse(page) { System.currentTimeMillis() }
            val isToday = mealViewModel.isToday(dateTimestamp)
            val dayMeals = mealsByDate[dateTimestamp] ?: emptyList()

            DayPage(
                dateTimestamp = dateTimestamp,
                isToday = isToday,
                meals = dayMeals,
                targetCalories = targetCalories,
                targetProtein = targetProtein,
                targetFat = targetFat,
                targetCarbs = targetCarbs,
                onAddMealClick = onAddMealClick,
                onMealClick = onMealClick,
                onGoToTodayClick = {
                    val todayIndex = availableDates.indexOfFirst { mealViewModel.isToday(it) }
                    if (todayIndex >= 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(todayIndex)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DayPage(
    dateTimestamp: Long,
    isToday: Boolean,
    meals: List<MealWithComponents>,
    targetCalories: Int,
    targetProtein: Int,
    targetFat: Int,
    targetCarbs: Int,
    onAddMealClick: () -> Unit,
    onMealClick: (MealWithComponents) -> Unit,
    onGoToTodayClick: () -> Unit
) {
    val consumedCalories = meals.flatMap { it.components }.sumOf { it.calories }
    val consumedProtein = meals.flatMap { it.components }.sumOf { it.proteinGrams }
    val consumedFat = meals.flatMap { it.components }.sumOf { it.fatGrams }
    val consumedCarbs = meals.flatMap { it.components }.sumOf { it.carbGrams }

    val remainingCalories = targetCalories - consumedCalories
    val remainingProtein = targetProtein - consumedProtein
    val remainingFat = targetFat - consumedFat
    val remainingCarbs = targetCarbs - consumedCarbs

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DaySummaryCard(
                    dateTimestamp = dateTimestamp,
                    isToday = isToday,
                    consumedCalories = consumedCalories,
                    targetCalories = targetCalories,
                    remainingCalories = remainingCalories,
                    consumedProtein = consumedProtein,
                    targetProtein = targetProtein,
                    remainingProtein = remainingProtein,
                    consumedFat = consumedFat,
                    targetFat = targetFat,
                    remainingFat = remainingFat,
                    consumedCarbs = consumedCarbs,
                    targetCarbs = targetCarbs,
                    remainingCarbs = remainingCarbs
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
                            text = if (isToday) "No meals yet. Tap + to add one!" else "No meals on this day.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(meals, key = { it.meal.id }) { mealWithComponents ->
                    MealCard(
                        mealWithComponents = mealWithComponents,
                        onClick = { onMealClick(mealWithComponents) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        if (isToday) {
            FloatingActionButton(
                onClick = onAddMealClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add meal"
                )
            }
        } else {
            IconButton(
                onClick = onGoToTodayClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Go to today",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun DaySummaryCard(
    dateTimestamp: Long,
    isToday: Boolean,
    consumedCalories: Int,
    targetCalories: Int,
    remainingCalories: Int,
    consumedProtein: Int,
    targetProtein: Int,
    remainingProtein: Int,
    consumedFat: Int,
    targetFat: Int,
    remainingFat: Int,
    consumedCarbs: Int,
    targetCarbs: Int,
    remainingCarbs: Int,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.US) }
    val title = if (isToday) "Today's Budget" else dateFormat.format(Date(dateTimestamp))
    val overColor = MaterialTheme.colorScheme.error

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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isToday) {
                Text(
                    text = if (remainingCalories >= 0) "$remainingCalories / $targetCalories kcal left" else "${-remainingCalories} / $targetCalories kcal over",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (remainingCalories < 0) overColor else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (remainingProtein >= 0) "$remainingProtein / ${targetProtein}g protein left" else "${-remainingProtein} / ${targetProtein}g protein over",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remainingProtein < 0) overColor else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (remainingFat >= 0) "$remainingFat / ${targetFat}g fat left" else "${-remainingFat} / ${targetFat}g fat over",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remainingFat < 0) overColor else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (remainingCarbs >= 0) "$remainingCarbs / ${targetCarbs}g carbs left" else "${-remainingCarbs} / ${targetCarbs}g carbs over",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remainingCarbs < 0) overColor else MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = "$consumedCalories / $targetCalories kcal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "P: ${consumedProtein}g / ${targetProtein}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "F: ${consumedFat}g / ${targetFat}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "C: ${consumedCarbs}g / ${targetCarbs}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MealCard(
    mealWithComponents: MealWithComponents,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val meal = mealWithComponents.meal
    val components = mealWithComponents.components
    val totalCalories = components.sumOf { it.calories }
    val totalProtein = components.sumOf { it.proteinGrams }
    val totalFat = components.sumOf { it.fatGrams }
    val totalCarbs = components.sumOf { it.carbGrams }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
