package pro.trousev.mealcontrol.viewmodel

import android.app.Application
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.TestDatabaseFactory

@RunWith(RobolectricTestRunner::class)
class MealViewModelTest {

    private lateinit var application: Application
    private lateinit var database: MealControlDatabase

    @Before
    fun setup() {
        application = RuntimeEnvironment.getApplication()
        database = TestDatabaseFactory.createInMemory(application)
    }

    @Test
    fun mealViewModel_creation_doesNotCrash() {
        val viewModel = MealViewModel(application)
        assertNotNull(viewModel.meals)
    }

    @Test
    fun mealViewModel_mealsStateFlow_isInitialized() {
        val viewModel = MealViewModel(application)
        assertNotNull(viewModel.meals)
        assertNotNull(viewModel.meals.value)
    }
}
