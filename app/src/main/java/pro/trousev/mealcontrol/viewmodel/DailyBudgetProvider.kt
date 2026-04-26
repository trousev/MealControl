package pro.trousev.mealcontrol.viewmodel

import pro.trousev.mealcontrol.data.local.entity.UserSettingsEntity

data class DailyBudget(
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,
)

object DailyBudgetProvider {
    fun calculateBudget(settings: UserSettingsEntity): DailyBudget =
        if (settings.customModeEnabled) {
            val calories =
                settings.customProteinGrams * 4 +
                    settings.customFatGrams * 9 +
                    settings.customCarbGrams * 4
            DailyBudget(
                calories = calories,
                protein = settings.customProteinGrams,
                fat = settings.customFatGrams,
                carbs = settings.customCarbGrams,
            )
        } else {
            calculateNormalBudget(settings)
        }

    fun shouldHideCalories(settings: UserSettingsEntity): Boolean = settings.hideCaloriesEnabled

    fun shouldHideBudgetExceeded(settings: UserSettingsEntity): Boolean = settings.hideBudgetExceededEnabled

    fun shouldShowProtein(
        customModeEnabled: Boolean,
        budget: DailyBudget,
    ): Boolean = !customModeEnabled || budget.protein > 0

    fun shouldShowFat(
        customModeEnabled: Boolean,
        budget: DailyBudget,
    ): Boolean = !customModeEnabled || budget.fat > 0

    fun shouldShowCarbs(
        customModeEnabled: Boolean,
        budget: DailyBudget,
    ): Boolean = !customModeEnabled || budget.carbs > 0

    private fun calculateNormalBudget(settings: UserSettingsEntity): DailyBudget {
        val gender =
            try {
                Gender.valueOf(settings.gender)
            } catch (e: Exception) {
                Gender.MALE
            }
        val activityLevel =
            ActivityLevel.entries.getOrElse(settings.activityLevel - 1) {
                ActivityLevel.SEDENTARY
            }
        val distribution =
            try {
                CalorieDistribution.valueOf(settings.calorieDistribution)
            } catch (e: Exception) {
                CalorieDistribution.HIGH_PROTEIN
            }

        val weightKg = settings.weightKg
        val heightCm = settings.heightCm
        val age = settings.age
        val targetWeightChangeKg = settings.targetWeightChangeKg

        val bmr =
            if (gender == Gender.MALE) {
                10.0 * weightKg + 6.25 * heightCm - 5.0 * age + 5.0
            } else {
                10.0 * weightKg + 6.25 * heightCm - 5.0 * age - 161.0
            }

        val tdee = bmr * activityLevel.multiplier

        val dailyDeficit =
            if (targetWeightChangeKg != 0.0f) {
                targetWeightChangeKg * 7700.0 / 7.0
            } else {
                0.0
            }

        val minCalories =
            if (gender == Gender.MALE) {
                maxOf(1500.0, bmr * 0.75)
            } else {
                maxOf(1200.0, bmr * 0.70)
            }

        val dailyCalories = (tdee + dailyDeficit).coerceAtLeast(minCalories).toInt()

        val targetWeight = (weightKg + targetWeightChangeKg).coerceAtLeast(30.0f)

        val proteinGrams =
            when (distribution) {
                CalorieDistribution.HIGH_PROTEIN -> (targetWeight * 2.2f).toInt()
                CalorieDistribution.BALANCED -> (targetWeight * 1.8f).toInt()
                CalorieDistribution.LOW_FAT -> (targetWeight * 2.0f).toInt()
                CalorieDistribution.CUSTOM -> {
                    val total = settings.customProteinPercent + settings.customFatPercent + settings.customCarbPercent
                    if (total != 100) {
                        val factor = 100.0 / total
                        (dailyCalories * settings.customProteinPercent * factor / 100.0 / 4.0).toInt()
                    } else {
                        (dailyCalories * settings.customProteinPercent / 100.0 / 4.0).toInt()
                    }
                }
            }

        val proteinCalories = proteinGrams * 4.0
        val remainingCalories = dailyCalories - proteinCalories

        val (fatP, carbP) =
            when {
                distribution == CalorieDistribution.CUSTOM &&
                    settings.customProteinPercent + settings.customFatPercent + settings.customCarbPercent != 100 -> {
                    val factor = 100.0 / (settings.customFatPercent + settings.customCarbPercent)
                    Pair(
                        settings.customFatPercent * factor,
                        settings.customCarbPercent * factor,
                    )
                }
                distribution == CalorieDistribution.CUSTOM -> {
                    Pair(
                        settings.customFatPercent.toDouble(),
                        settings.customCarbPercent.toDouble(),
                    )
                }
                else -> {
                    Pair(
                        distribution.fatPercent.toDouble(),
                        distribution.carbPercent.toDouble(),
                    )
                }
            }

        val fatGrams = (remainingCalories * fatP / (fatP + carbP) / 9.0).toInt()
        val carbGrams = (remainingCalories * carbP / (fatP + carbP) / 4.0).toInt()

        return DailyBudget(
            calories = dailyCalories,
            protein = proteinGrams,
            fat = fatGrams,
            carbs = carbGrams,
        )
    }
}
