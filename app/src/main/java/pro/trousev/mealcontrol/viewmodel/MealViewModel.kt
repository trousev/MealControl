package pro.trousev.mealcontrol.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.entity.MealWithComponents
import pro.trousev.mealcontrol.data.repository.MealRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class MealViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MealControlDatabase.getDatabase(application)
    private val repository = MealRepository(database.mealDao())

    private val _meals = MutableStateFlow<List<MealWithComponents>>(emptyList())
    val meals: StateFlow<List<MealWithComponents>> = _meals.asStateFlow()

    private val _todayMeals = MutableStateFlow<List<MealWithComponents>>(emptyList())
    val todayMeals: StateFlow<List<MealWithComponents>> = _todayMeals.asStateFlow()

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
            val filteredMeals = allMeals.filter { meal ->
                val mealDate = Calendar.getInstance().apply {
                    timeInMillis = meal.meal.timestamp
                }
                mealDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        mealDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            }
            _todayMeals.value = filteredMeals
        }
    }

    fun saveMeal(
        photoUri: String,
        description: String,
        components: List<Triple<String, Double, List<Number>>>
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
        timestamp: Long
    ) {
        viewModelScope.launch {
            repository.updateMeal(mealId, description, components, timestamp)
            loadMeals()
        }
    }
}
