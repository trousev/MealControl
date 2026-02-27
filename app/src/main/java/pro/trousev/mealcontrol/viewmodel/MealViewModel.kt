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

class MealViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MealControlDatabase.getDatabase(application)
    private val repository = MealRepository(database.mealDao())

    private val _meals = MutableStateFlow<List<MealWithComponents>>(emptyList())
    val meals: StateFlow<List<MealWithComponents>> = _meals.asStateFlow()

    init {
        loadMeals()
    }

    fun loadMeals() {
        _meals.value = repository.getAllMeals()
    }

    fun saveMeal(
        photoUri: String,
        description: String,
        components: List<Pair<String, Int>>
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
}
