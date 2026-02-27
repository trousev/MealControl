package pro.trousev.mealcontrol.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.TestDatabaseFactory

@RunWith(RobolectricTestRunner::class)
class MealRepositoryTest {

    private lateinit var database: MealControlDatabase
    private lateinit var repository: MealRepository

    @Before
    fun setup() {
        database = TestDatabaseFactory.createInMemory(RuntimeEnvironment.getApplication())
        repository = MealRepository(database.mealDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveMeal_insertsMealAndComponents() = runBlocking {
        val photoUri = "content://photo/123"
        val description = "Lunch"
        val components = listOf(
            "Chicken" to 300,
            "Rice" to 200,
            "Salad" to 50
        )

        val mealId = repository.saveMeal(photoUri, description, components)

        assertTrue(mealId > 0)

        val meals = repository.getAllMeals()
        assertEquals(1, meals.size)
        assertEquals(photoUri, meals[0].meal.photoUri)
        assertEquals(description, meals[0].meal.description)
        assertEquals(3, meals[0].components.size)
    }

    @Test
    fun getAllMeals_returnsEmptyListWhenNoMeals() = runBlocking {
        val meals = repository.getAllMeals()

        assertTrue(meals.isEmpty())
    }

    @Test
    fun getAllMeals_returnsMealsSortedByTimestamp() = runBlocking {
        repository.saveMeal("photo1", "Meal 1", listOf("A" to 100))
        Thread.sleep(10)
        repository.saveMeal("photo2", "Meal 2", listOf("B" to 200))
        Thread.sleep(10)
        repository.saveMeal("photo3", "Meal 3", listOf("C" to 300))

        val meals = repository.getAllMeals()

        assertEquals(3, meals.size)
        assertEquals("Meal 3", meals[0].meal.description)
        assertEquals("Meal 2", meals[1].meal.description)
        assertEquals("Meal 1", meals[2].meal.description)
    }

    @Test
    fun getMealById_returnsCorrectMeal() = runBlocking {
        val mealId = repository.saveMeal("photo", "Dinner", listOf("Beef" to 500))

        val meal = repository.getMealById(mealId)

        assertNotNull(meal)
        assertEquals("Dinner", meal!!.meal.description)
    }

    @Test
    fun getMealById_returnsNullForNonExistent() = runBlocking {
        val meal = repository.getMealById(999L)

        assertNull(meal)
    }

    @Test
    fun deleteMeal_removesMealAndComponents() = runBlocking {
        val mealId = repository.saveMeal("photo", "Lunch", listOf("A" to 100))

        repository.deleteMeal(mealId)

        val meals = repository.getAllMeals()
        assertTrue(meals.isEmpty())
    }

    @Test
    fun saveMeal_withEmptyComponents_works() = runBlocking {
        val mealId = repository.saveMeal("photo", "No components", emptyList())

        val meal = repository.getMealById(mealId)

        assertNotNull(meal)
        assertTrue(meal!!.components.isEmpty())
    }

    @Test
    fun saveMeal_withManyComponents_works() = runBlocking {
        val manyComponents = (1..100).map { "Component $it" to it * 10 }
        val mealId = repository.saveMeal("photo", "Many components", manyComponents)

        val meal = repository.getMealById(mealId)

        assertNotNull(meal)
        assertEquals(100, meal!!.components.size)
    }

    @Test
    fun getMealById_returnsMealWithComponents() = runBlocking {
        val mealId = repository.saveMeal(
            "photo",
            "Test",
            listOf("Apple" to 100, "Banana" to 150, "Orange" to 80)
        )

        val meal = repository.getMealById(mealId)

        assertNotNull(meal)
        assertEquals(3, meal!!.components.size)
        val componentNames = meal.components.map { it.name }.toSet()
        assertTrue(componentNames.contains("Apple"))
        assertTrue(componentNames.contains("Banana"))
        assertTrue(componentNames.contains("Orange"))
    }
}
