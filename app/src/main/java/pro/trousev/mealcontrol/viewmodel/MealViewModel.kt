package pro.trousev.mealcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pro.trousev.mealcontrol.ServiceLocator
import pro.trousev.mealcontrol.data.local.entity.MealComponentEntity
import pro.trousev.mealcontrol.data.local.entity.MealWithComponents
import pro.trousev.mealcontrol.data.repository.MealRepository
import java.util.Calendar

data class DayNutrientTotals(
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,
)

class MealViewModel : ViewModel() {
    private val repository: MealRepository = ServiceLocator.provideMealRepository()

    private val _meals = MutableStateFlow<List<MealWithComponents>>(emptyList())
    val meals: StateFlow<List<MealWithComponents>> = _meals.asStateFlow()

    private val _todayMeals = MutableStateFlow<List<MealWithComponents>>(emptyList())
    val todayMeals: StateFlow<List<MealWithComponents>> = _todayMeals.asStateFlow()

    private val _pendingPhotoUri = MutableStateFlow<String?>(null)
    val pendingPhotoUri: StateFlow<String?> = _pendingPhotoUri.asStateFlow()

    private val _mealsByDate = MutableStateFlow<Map<Long, List<MealWithComponents>>>(emptyMap())
    val mealsByDate: StateFlow<Map<Long, List<MealWithComponents>>> = _mealsByDate.asStateFlow()

    private val _availableDates = MutableStateFlow<List<Long>>(emptyList())
    val availableDates: StateFlow<List<Long>> = _availableDates.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _dayTotals = MutableStateFlow<Map<Long, DayNutrientTotals>>(emptyMap())
    val dayTotals: StateFlow<Map<Long, DayNutrientTotals>> = _dayTotals.asStateFlow()

    init {
        viewModelScope.launch {
            loadMeals()
        }
    }

    fun loadMeals() {
        viewModelScope.launch {
            val allMeals = repository.getAllMeals()
            _meals.value = allMeals

            val today = Calendar.getInstance()
            val filteredMeals =
                allMeals.filter { meal ->
                    val mealDate =
                        Calendar.getInstance().apply {
                            timeInMillis = meal.meal.timestamp
                        }
                    mealDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        mealDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                }
            _todayMeals.value = filteredMeals

            val groupedMeals = groupMealsByDate(allMeals)
            _mealsByDate.value = groupedMeals

            val todayMidnight = normalizeToMidnight(System.currentTimeMillis())
            val dates = groupedMeals.keys.sortedDescending()
            val datesWithToday =
                if (dates.isEmpty() || dates.first() != todayMidnight) {
                    listOf(todayMidnight) + dates
                } else {
                    dates
                }
            _availableDates.value = datesWithToday

            val totals =
                groupedMeals.mapValues { (_, meals) ->
                    val calories = meals.flatMap { it.components }.sumOf { it.calories }
                    val protein = meals.flatMap { it.components }.sumOf { it.proteinGrams }
                    val fat = meals.flatMap { it.components }.sumOf { it.fatGrams }
                    val carbs = meals.flatMap { it.components }.sumOf { it.carbGrams }
                    DayNutrientTotals(calories, protein, fat, carbs)
                }
            _dayTotals.value = totals

            _currentPageIndex.value = 0
        }
    }

    private fun groupMealsByDate(meals: List<MealWithComponents>): Map<Long, List<MealWithComponents>> =
        meals.groupBy { meal ->
            normalizeToMidnight(meal.meal.timestamp)
        }

    private fun normalizeToMidnight(timestamp: Long): Long {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        return calendar.timeInMillis
    }

    fun isToday(dateTimestamp: Long): Boolean = normalizeToMidnight(dateTimestamp) == normalizeToMidnight(System.currentTimeMillis())

    fun navigateToToday() {
        val todayMidnight = normalizeToMidnight(System.currentTimeMillis())
        val index = _availableDates.value.indexOf(todayMidnight)
        if (index >= 0) {
            _currentPageIndex.value = index
        }
    }

    fun setCurrentPageIndex(index: Int) {
        if (index in _availableDates.value.indices) {
            _currentPageIndex.value = index
        }
    }

    fun saveMeal(
        photoUri: String,
        description: String,
        components: List<Triple<String, Double, List<Number>>>,
    ) {
        viewModelScope.launch {
            repository.saveMeal(photoUri, description, components)
            loadMeals()
        }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)
            loadMeals()
        }
    }

    fun updateMeal(
        mealId: Long,
        description: String,
        components: List<Triple<String, Double, List<Number>>>,
        timestamp: Long,
    ) {
        viewModelScope.launch {
            repository.updateMeal(mealId, description, components, timestamp)
            loadMeals()
        }
    }

    fun setPendingPhotoUri(uri: String) {
        _pendingPhotoUri.value = uri
    }

    fun clearPendingPhoto() {
        _pendingPhotoUri.value = null
    }

    suspend fun getMealById(mealId: Long): MealWithComponents? = repository.getMealById(mealId)

    fun computeMealTotals(components: List<MealComponentEntity>): DayNutrientTotals =
        DayNutrientTotals(
            calories = components.sumOf { it.calories },
            protein = components.sumOf { it.proteinGrams },
            fat = components.sumOf { it.fatGrams },
            carbs = components.sumOf { it.carbGrams },
        )
}
