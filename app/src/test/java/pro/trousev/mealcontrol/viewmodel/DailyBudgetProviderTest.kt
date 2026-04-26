package pro.trousev.mealcontrol.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pro.trousev.mealcontrol.data.local.entity.UserSettingsEntity

class DailyBudgetProviderTest {
    @Test
    fun calculateBudget_normalMode_calculatesFromBodyMeasurements() {
        val settings =
            UserSettingsEntity(
                weightKg = 80f,
                heightCm = 180f,
                age = 30,
                gender = "MALE",
                targetWeightChangeKg = 0f,
                activityLevel = 2,
                calorieDistribution = "HIGH_PROTEIN",
                customModeEnabled = false,
            )

        val budget = DailyBudgetProvider.calculateBudget(settings)

        assertTrue(budget.calories > 0)
        assertTrue(budget.protein > 0)
        assertTrue(budget.fat > 0)
        assertTrue(budget.carbs > 0)
    }

    @Test
    fun calculateBudget_normalMode_femaleCalculatesCorrectly() {
        val settings =
            UserSettingsEntity(
                weightKg = 60f,
                heightCm = 165f,
                age = 25,
                gender = "FEMALE",
                targetWeightChangeKg = 0f,
                activityLevel = 2,
                calorieDistribution = "BALANCED",
                customModeEnabled = false,
            )

        val budget = DailyBudgetProvider.calculateBudget(settings)

        val expectedBmr = (10.0 * 60 + 6.25 * 165 - 5.0 * 25 - 161.0).toInt()
        val expectedTdee = (expectedBmr * 1.375).toInt()

        assertEquals(expectedTdee, budget.calories)
    }

    @Test
    fun calculateBudget_customMode_usesDirectGramTargets() {
        val settings =
            UserSettingsEntity(
                weightKg = 80f,
                heightCm = 180f,
                age = 30,
                gender = "MALE",
                targetWeightChangeKg = 0f,
                activityLevel = 2,
                calorieDistribution = "HIGH_PROTEIN",
                customModeEnabled = true,
                customProteinGrams = 150,
                customFatGrams = 80,
                customCarbGrams = 200,
            )

        val budget = DailyBudgetProvider.calculateBudget(settings)

        assertEquals(150, budget.protein)
        assertEquals(80, budget.fat)
        assertEquals(200, budget.carbs)
        assertEquals(150 * 4 + 80 * 9 + 200 * 4, budget.calories)
    }

    @Test
    fun calculateBudget_customMode_zeroMacros() {
        val settings =
            UserSettingsEntity(
                customModeEnabled = true,
                customProteinGrams = 0,
                customFatGrams = 0,
                customCarbGrams = 0,
            )

        val budget = DailyBudgetProvider.calculateBudget(settings)

        assertEquals(0, budget.calories)
        assertEquals(0, budget.protein)
        assertEquals(0, budget.fat)
        assertEquals(0, budget.carbs)
    }

    @Test
    fun calculateBudget_customMode_partialMacros() {
        val settings =
            UserSettingsEntity(
                customModeEnabled = true,
                customProteinGrams = 100,
                customFatGrams = 50,
                customCarbGrams = 0,
            )

        val budget = DailyBudgetProvider.calculateBudget(settings)

        assertEquals(100, budget.protein)
        assertEquals(50, budget.fat)
        assertEquals(0, budget.carbs)
        assertEquals(100 * 4 + 50 * 9, budget.calories)
    }

    @Test
    fun shouldHideCalories_returnsFalseWhenDisabled() {
        val settings = UserSettingsEntity(hideCaloriesEnabled = false)

        val result = DailyBudgetProvider.shouldHideCalories(settings)

        assertFalse(result)
    }

    @Test
    fun shouldHideCalories_returnsTrueWhenEnabled() {
        val settings = UserSettingsEntity(hideCaloriesEnabled = true)

        val result = DailyBudgetProvider.shouldHideCalories(settings)

        assertTrue(result)
    }

    @Test
    fun calculateBudget_customMode_doesNotUseBodyMeasurements() {
        val settings =
            UserSettingsEntity(
                weightKg = 0f,
                heightCm = 0f,
                age = 0,
                customModeEnabled = true,
                customProteinGrams = 200,
                customFatGrams = 100,
                customCarbGrams = 300,
            )

        val budget = DailyBudgetProvider.calculateBudget(settings)

        assertEquals(200, budget.protein)
        assertEquals(100, budget.fat)
        assertEquals(300, budget.carbs)
        assertEquals(200 * 4 + 100 * 9 + 300 * 4, budget.calories)
    }
}
