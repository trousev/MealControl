package pro.trousev.mealcontrol.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.entity.UserSettingsEntity
import pro.trousev.mealcontrol.data.repository.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Gender {
    MALE, FEMALE
}

enum class ActivityLevel(val multiplier: Double, val description: String) {
    SEDENTARY(1.2, "I sit on sofa all day"),
    LIGHTLY_ACTIVE(1.375, "I go for walks sometimes"),
    MODERATELY_ACTIVE(1.55, "I exercise 3-4 times/week"),
    VERY_ACTIVE(1.725, "I go to gym 5-6 times/week"),
    EXTREMELY_ACTIVE(1.9, "I train hard every day / physical job")
}

enum class CalorieDistribution(val proteinPercent: Int, val fatPercent: Int, val carbPercent: Int) {
    HIGH_PROTEIN(40, 30, 30),
    BALANCED(30, 35, 35),
    LOW_FAT(30, 20, 50),
    CUSTOM(30, 30, 40)
}

data class CalorieCalculation(
    val bmr: Int,
    val tdee: Int,
    val dailyDeficit: Int,
    val dailyCalories: Int,
    val proteinGrams: Int,
    val fatGrams: Int,
    val carbGrams: Int
)

data class SettingsFormState(
    val weightKg: String = "",
    val heightCm: String = "",
    val age: String = "",
    val gender: Gender = Gender.MALE,
    val targetWeightChangeKg: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val calorieDistribution: CalorieDistribution = CalorieDistribution.HIGH_PROTEIN,
    val customProteinPercent: Int = 40,
    val customFatPercent: Int = 30,
    val customCarbPercent: Int = 30,
    val openAiApiKey: String = "",
    val isValid: Boolean = false,
    val calculation: CalorieCalculation? = null,
    val customDistributionError: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MealControlDatabase.getDatabase(application)
    private val repository = UserSettingsRepository(database.userSettingsDao())

    private val _formState = MutableStateFlow(SettingsFormState())
    val formState: StateFlow<SettingsFormState> = _formState.asStateFlow()

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = repository.getSettings()
            if (settings != null && settings.weightKg > 0) {
                val distribution = try {
                    CalorieDistribution.valueOf(settings.calorieDistribution)
                } catch (e: Exception) {
                    CalorieDistribution.HIGH_PROTEIN
                }
                val activityLevel = ActivityLevel.entries.getOrElse(settings.activityLevel - 1) {
                    ActivityLevel.SEDENTARY
                }
                val gender = try {
                    Gender.valueOf(settings.gender)
                } catch (e: Exception) {
                    Gender.MALE
                }

                _formState.value = SettingsFormState(
                    weightKg = settings.weightKg.toString(),
                    heightCm = settings.heightCm.toString(),
                    age = settings.age.toString(),
                    gender = gender,
                    targetWeightChangeKg = settings.targetWeightChangeKg.toString(),
                    activityLevel = activityLevel,
                    calorieDistribution = distribution,
                    customProteinPercent = settings.customProteinPercent,
                    customFatPercent = settings.customFatPercent,
                    openAiApiKey = settings.openAiApiKey,
                    isValid = true,
                    calculation = calculateCalories(
                        weightKg = settings.weightKg,
                        heightCm = settings.heightCm,
                        age = settings.age,
                        gender = gender,
                        targetWeightChangeKg = settings.targetWeightChangeKg,
                        activityLevel = activityLevel,
                        distribution = distribution,
                        customProtein = settings.customProteinPercent,
                        customFat = settings.customFatPercent,
                        customCarb = settings.customCarbPercent
                    )
                )
            }
            _settingsLoaded.value = true
        }
    }

    fun updateWeight(weight: String) {
        val filtered = weight.filter { it.isDigit() || it == '.' }
        _formState.value = _formState.value.copy(weightKg = filtered)
        recalculate()
    }

    fun updateHeight(height: String) {
        val filtered = height.filter { it.isDigit() || it == '.' }
        _formState.value = _formState.value.copy(heightCm = filtered)
        recalculate()
    }

    fun updateAge(age: String) {
        val filtered = age.filter { it.isDigit() }
        _formState.value = _formState.value.copy(age = filtered)
        recalculate()
    }

    fun updateGender(gender: Gender) {
        _formState.value = _formState.value.copy(gender = gender)
        recalculate()
    }

    fun updateTargetWeightChange(change: String) {
        val filtered = change.filter { it.isDigit() || it == '.' || it == '-' }
        _formState.value = _formState.value.copy(targetWeightChangeKg = filtered)
        
        val targetChange = filtered.toFloatOrNull() ?: 0f
        val currentDistribution = _formState.value.calorieDistribution
        val newDistribution = if (currentDistribution != CalorieDistribution.CUSTOM) {
            if (targetChange < 0) CalorieDistribution.HIGH_PROTEIN else CalorieDistribution.BALANCED
        } else {
            currentDistribution
        }
        
        _formState.value = _formState.value.copy(calorieDistribution = newDistribution)
        recalculate()
    }

    fun updateActivityLevel(level: ActivityLevel) {
        _formState.value = _formState.value.copy(activityLevel = level)
        recalculate()
    }

    fun updateCalorieDistribution(distribution: CalorieDistribution) {
        _formState.value = _formState.value.copy(calorieDistribution = distribution)
        recalculate()
    }

    fun updateCustomProtein(percent: Int) {
        _formState.value = _formState.value.copy(customProteinPercent = percent.coerceIn(0, 100))
        recalculate()
    }

    fun updateCustomFat(percent: Int) {
        _formState.value = _formState.value.copy(customFatPercent = percent.coerceIn(0, 100))
        recalculate()
    }

    fun updateCustomCarb(percent: Int) {
        _formState.value = _formState.value.copy(customCarbPercent = percent.coerceIn(0, 100))
        recalculate()
    }

    fun updateOpenAiApiKey(apiKey: String) {
        _formState.value = _formState.value.copy(openAiApiKey = apiKey)
        autoSaveSettings()
    }

    private fun recalculate() {
        val state = _formState.value
        val weight = state.weightKg.toFloatOrNull() ?: 0f
        val height = state.heightCm.toFloatOrNull() ?: 0f
        val age = state.age.toIntOrNull() ?: 0
        val targetChange = state.targetWeightChangeKg.toFloatOrNull() ?: 0f

        val customSum = state.customProteinPercent + state.customFatPercent + state.customCarbPercent
        val customError = if (state.calorieDistribution == CalorieDistribution.CUSTOM && customSum != 100) {
            "Must equal 100% (currently $customSum%)"
        } else null

        val isValid = weight in 30f..300f && height in 100f..250f && age in 10..120 && customError == null

        if (isValid) {
            val calculation = calculateCalories(
                weightKg = weight,
                heightCm = height,
                age = age,
                gender = state.gender,
                targetWeightChangeKg = targetChange,
                activityLevel = state.activityLevel,
                distribution = state.calorieDistribution,
                customProtein = state.customProteinPercent,
                customFat = state.customFatPercent,
                customCarb = state.customCarbPercent
            )
            _formState.value = state.copy(isValid = true, calculation = calculation, customDistributionError = null)
            autoSaveSettings()
        } else {
            _formState.value = state.copy(isValid = false, calculation = null, customDistributionError = customError)
        }
    }

    private fun autoSaveSettings() {
        viewModelScope.launch {
            val state = _formState.value
            val settings = UserSettingsEntity(
                id = 1,
                weightKg = state.weightKg.toFloatOrNull() ?: 0f,
                heightCm = state.heightCm.toFloatOrNull() ?: 0f,
                age = state.age.toIntOrNull() ?: 0,
                gender = state.gender.name,
                targetWeightChangeKg = state.targetWeightChangeKg.toFloatOrNull() ?: 0f,
                activityLevel = state.activityLevel.ordinal + 1,
                calorieDistribution = state.calorieDistribution.name,
                customProteinPercent = state.customProteinPercent,
                customFatPercent = state.customFatPercent,
                customCarbPercent = state.customCarbPercent,
                openAiApiKey = state.openAiApiKey
            )
            repository.saveSettings(settings)
        }
    }

    private fun calculateCalories(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: Gender,
        targetWeightChangeKg: Float,
        activityLevel: ActivityLevel,
        distribution: CalorieDistribution,
        customProtein: Int,
        customFat: Int,
        customCarb: Int
    ): CalorieCalculation {
        val bmr = if (gender == Gender.MALE) {
            10.0 * weightKg + 6.25 * heightCm - 5.0 * age + 5.0
        } else {
            10.0 * weightKg + 6.25 * heightCm - 5.0 * age - 161.0
        }

        val tdee = bmr * activityLevel.multiplier

        val dailyDeficit = if (targetWeightChangeKg != 0.0f) {
            targetWeightChangeKg * 7700.0 / 7.0
        } else {
            0.0
        }

        val minCalories = if (gender == Gender.MALE) {
            maxOf(1500.0, bmr * 0.75)
        } else {
            maxOf(1200.0, bmr * 0.70)
        }

        val dailyCalories = (tdee + dailyDeficit).coerceAtLeast(minCalories)

        val targetWeight = (weightKg + targetWeightChangeKg).coerceAtLeast(30.0f)

        val proteinGrams = when (distribution) {
            CalorieDistribution.HIGH_PROTEIN -> (targetWeight * 2.2f).toInt()
            CalorieDistribution.BALANCED -> (targetWeight * 1.8f).toInt()
            CalorieDistribution.LOW_FAT -> (targetWeight * 2.0f).toInt()
            CalorieDistribution.CUSTOM -> {
                val total = customProtein + customFat + customCarb
                if (total != 100) {
                    val factor = 100.0 / total
                    (dailyCalories * customProtein * factor / 100.0 / 4.0).toInt()
                } else {
                    (dailyCalories * customProtein / 100.0 / 4.0).toInt()
                }
            }
        }

        val proteinCalories = proteinGrams * 4.0
        val remainingCalories = dailyCalories - proteinCalories

        val (fatP, carbP) = if (distribution == CalorieDistribution.CUSTOM && customProtein + customFat + customCarb != 100) {
            val factor = 100.0 / (customFat + customCarb)
            Pair(customFat * factor, customCarb * factor)
        } else if (distribution == CalorieDistribution.CUSTOM) {
            Pair(customFat.toDouble(), customCarb.toDouble())
        } else {
            Pair(distribution.fatPercent.toDouble(), distribution.carbPercent.toDouble())
        }

        val fatGrams = (remainingCalories * fatP / (fatP + carbP) / 9.0).toInt()
        val carbGrams = (remainingCalories * carbP / (fatP + carbP) / 4.0).toInt()

        return CalorieCalculation(
            bmr = bmr.toInt(),
            tdee = tdee.toInt(),
            dailyDeficit = dailyDeficit.toInt(),
            dailyCalories = dailyCalories.toInt(),
            proteinGrams = proteinGrams,
            fatGrams = fatGrams,
            carbGrams = carbGrams
        )
    }
}
