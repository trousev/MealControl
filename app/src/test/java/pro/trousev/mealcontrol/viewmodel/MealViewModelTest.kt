package pro.trousev.mealcontrol.viewmodel

import androidx.room.Room
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import pro.trousev.mealcontrol.ServiceLocator
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.dao.MealDao
import pro.trousev.mealcontrol.util.SecureStorage

@RunWith(RobolectricTestRunner::class)
class MealViewModelTest {
    private lateinit var database: MealControlDatabase

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication().applicationContext,
                    MealControlDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        val mockSecureStorage =
            object : SecureStorage {
                override fun storeApiKey(apiKey: String) {}

                override fun retrieveApiKey(): String = ""
            }
        ServiceLocator.initialize(RuntimeEnvironment.getApplication(), mockSecureStorage)
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
