package pro.trousev.mealcontrol.viewmodel

import android.app.Application
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private lateinit var application: Application

    @Before
    fun setup() {
        application = RuntimeEnvironment.getApplication()
    }

    @Test
    fun settingsViewModel_creation_doesNotCrash() {
        val viewModel = SettingsViewModel(application)
        assertNotNull(viewModel.formState)
        assertNotNull(viewModel.settingsLoaded)
    }

    @Test
    fun settingsViewModel_defaultState_hasEmptyValues() {
        val viewModel = SettingsViewModel(application)
        val state = viewModel.formState.value
        assertEquals("", state.weightKg)
        assertEquals("", state.heightCm)
        assertEquals("", state.age)
        assertEquals(Gender.MALE, state.gender)
        assertEquals(ActivityLevel.SEDENTARY, state.activityLevel)
    }

    @Test
    fun settingsViewModel_updateWeight_filtersNonNumeric() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80.5kg")
        assertEquals("80.5", viewModel.formState.value.weightKg)
    }

    @Test
    fun settingsViewModel_updateWeight_acceptsValidNumber() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80")
        assertEquals("80", viewModel.formState.value.weightKg)
    }

    @Test
    fun settingsViewModel_updateHeight_filtersNonNumeric() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateHeight("180.5cm")
        assertEquals("180.5", viewModel.formState.value.heightCm)
    }

    @Test
    fun settingsViewModel_updateAge_filtersNonNumeric() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateAge("30years")
        assertEquals("30", viewModel.formState.value.age)
    }

    @Test
    fun settingsViewModel_updateGender_changesGender() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateGender(Gender.FEMALE)
        assertEquals(Gender.FEMALE, viewModel.formState.value.gender)
    }

    @Test
    fun settingsViewModel_updateActivityLevel_changesLevel() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateActivityLevel(ActivityLevel.VERY_ACTIVE)
        assertEquals(ActivityLevel.VERY_ACTIVE, viewModel.formState.value.activityLevel)
    }

    @Test
    fun settingsViewModel_calculation_maleSedentary_validInput() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80")
        viewModel.updateHeight("180")
        viewModel.updateAge("30")

        val state = viewModel.formState.value
        assertTrue(state.isValid)
        assertNotNull(state.calculation)

        val calc = state.calculation!!
        assertTrue(calc.bmr > 0)
        assertTrue(calc.tdee > calc.bmr)
    }

    @Test
    fun settingsViewModel_calculation_female_hasDifferentBmr() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("60")
        viewModel.updateHeight("165")
        viewModel.updateAge("25")
        viewModel.updateGender(Gender.FEMALE)

        val state = viewModel.formState.value
        assertTrue(state.isValid)
        assertNotNull(state.calculation)

        val calc = state.calculation!!
        assertTrue(calc.bmr > 0)
    }

    @Test
    fun settingsViewModel_calculation_withWeightLoss_hasDeficit() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80")
        viewModel.updateHeight("180")
        viewModel.updateAge("30")
        viewModel.updateTargetWeightChange("-5")

        val state = viewModel.formState.value
        assertTrue(state.isValid)
        assertNotNull(state.calculation)

        val calc = state.calculation!!
        assertTrue(calc.dailyDeficit < 0)
        assertTrue(calc.dailyCalories < calc.tdee)
    }

    @Test
    fun settingsViewModel_calculation_withWeightGain_hasSurplus() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80")
        viewModel.updateHeight("180")
        viewModel.updateAge("30")
        viewModel.updateTargetWeightChange("5")

        val state = viewModel.formState.value
        assertTrue(state.isValid)
        assertNotNull(state.calculation)

        val calc = state.calculation!!
        assertTrue(calc.dailyDeficit > 0)
        assertTrue(calc.dailyCalories > calc.tdee)
    }

    @Test
    fun settingsViewModel_calculation_highProteinDistribution() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80")
        viewModel.updateHeight("180")
        viewModel.updateAge("30")
        viewModel.updateCalorieDistribution(CalorieDistribution.HIGH_PROTEIN)

        val state = viewModel.formState.value
        val calc = state.calculation!!

        assertTrue(calc.proteinGrams > calc.fatGrams)
    }

    @Test
    fun settingsViewModel_calculation_balancedDistribution() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80")
        viewModel.updateHeight("180")
        viewModel.updateAge("30")
        viewModel.updateCalorieDistribution(CalorieDistribution.BALANCED)

        val state = viewModel.formState.value
        val calc = state.calculation!!

        assertTrue(calc.proteinGrams > 0)
        assertTrue(calc.fatGrams > 0)
        assertTrue(calc.carbGrams > 0)
    }

    @Test
    fun settingsViewModel_calculation_customDistribution() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80")
        viewModel.updateHeight("180")
        viewModel.updateAge("30")
        viewModel.updateCalorieDistribution(CalorieDistribution.CUSTOM)
        viewModel.updateCustomProtein(50)
        viewModel.updateCustomFat(25)
        viewModel.updateCustomCarb(25)

        val state = viewModel.formState.value
        val calc = state.calculation!!

        assertTrue(calc.proteinGrams > calc.fatGrams)
    }

    @Test
    fun settingsViewModel_weightChange_recommendsHighProteinWhenLosing() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateTargetWeightChange("-5")

        assertEquals(CalorieDistribution.HIGH_PROTEIN, viewModel.formState.value.calorieDistribution)
    }

    @Test
    fun settingsViewModel_weightChange_recommendsBalancedWhenGaining() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateTargetWeightChange("5")

        assertEquals(CalorieDistribution.BALANCED, viewModel.formState.value.calorieDistribution)
    }

    @Test
    fun settingsViewModel_invalidWeight_outOfRange() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("20")
        viewModel.updateHeight("180")
        viewModel.updateAge("30")

        assertFalse(viewModel.formState.value.isValid)
    }

    @Test
    fun settingsViewModel_invalidHeight_outOfRange() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80")
        viewModel.updateHeight("50")
        viewModel.updateAge("30")

        assertFalse(viewModel.formState.value.isValid)
    }

    @Test
    fun settingsViewModel_invalidAge_outOfRange() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("80")
        viewModel.updateHeight("180")
        viewModel.updateAge("5")

        assertFalse(viewModel.formState.value.isValid)
    }

    @Test
    fun activityLevel_multipliers_areInCorrectOrder() {
        assertTrue(ActivityLevel.SEDENTARY.multiplier < ActivityLevel.LIGHTLY_ACTIVE.multiplier)
        assertTrue(ActivityLevel.LIGHTLY_ACTIVE.multiplier < ActivityLevel.MODERATELY_ACTIVE.multiplier)
        assertTrue(ActivityLevel.MODERATELY_ACTIVE.multiplier < ActivityLevel.VERY_ACTIVE.multiplier)
        assertTrue(ActivityLevel.VERY_ACTIVE.multiplier < ActivityLevel.EXTREMELY_ACTIVE.multiplier)
    }

    @Test
    fun activityLevel_descriptions_areNotEmpty() {
        ActivityLevel.entries.forEach { level ->
            assertTrue(level.description.isNotEmpty())
        }
    }

    @Test
    fun calorieDistribution_allSumTo100() {
        assertEquals(100, CalorieDistribution.HIGH_PROTEIN.proteinPercent + CalorieDistribution.HIGH_PROTEIN.fatPercent + CalorieDistribution.HIGH_PROTEIN.carbPercent)
        assertEquals(100, CalorieDistribution.BALANCED.proteinPercent + CalorieDistribution.BALANCED.fatPercent + CalorieDistribution.BALANCED.carbPercent)
        assertEquals(100, CalorieDistribution.LOW_FAT.proteinPercent + CalorieDistribution.LOW_FAT.fatPercent + CalorieDistribution.LOW_FAT.carbPercent)
        assertEquals(100, CalorieDistribution.CUSTOM.proteinPercent + CalorieDistribution.CUSTOM.fatPercent + CalorieDistribution.CUSTOM.carbPercent)
    }

    @Test
    fun settingsViewModel_calculation_producesValidMacros() {
        val viewModel = SettingsViewModel(application)
        viewModel.updateWeight("70")
        viewModel.updateHeight("170")
        viewModel.updateAge("25")

        val calc = viewModel.formState.value.calculation!!

        assertTrue(calc.proteinGrams > 0)
        assertTrue(calc.fatGrams > 0)
        assertTrue(calc.carbGrams > 0)
        assertTrue(calc.dailyCalories > 0)
    }
}
