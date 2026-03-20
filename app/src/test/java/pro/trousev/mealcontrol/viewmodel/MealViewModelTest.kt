package pro.trousev.mealcontrol.viewmodel

import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import pro.trousev.mealcontrol.ServiceLocator
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.dao.MealDao
import androidx.room.Room

@RunWith(RobolectricTestRunner::class)
class MealViewModelTest {

    private lateinit var database: MealControlDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication().applicationContext,
            MealControlDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        ServiceLocator.initialize(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        database.close()
        ServiceLocator.resetForTesting()
    }

    @Test
    fun mealViewModel_creation_doesNotCrash() {
        val viewModel = MealViewModel()
        assertNotNull(viewModel.meals)
    }

    @Test
    fun mealViewModel_mealsStateFlow_isInitialized() {
        val viewModel = MealViewModel()
        assertNotNull(viewModel.meals)
        assertNotNull(viewModel.meals.value)
    }
}