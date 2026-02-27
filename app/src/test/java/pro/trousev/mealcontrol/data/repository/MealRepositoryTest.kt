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
            Triple("Chicken", 150, listOf(300, 30, 10, 0)),
            Triple("Rice", 100, listOf(200, 4, 1, 45)),
            Triple("Salad", 50, listOf(20, 1, 0, 4))
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
        repository.saveMeal("photo1", "Meal 1", listOf(Triple("A", 100, listOf(100, 10, 5, 5))))
        Thread.sleep(10)
        repository.saveMeal("photo2", "Meal 2", listOf(Triple("B", 200, listOf(200, 20, 10, 10))))
        Thread.sleep(10)
        repository.saveMeal("photo3", "Meal 3", listOf(Triple("C", 300, listOf(300, 30, 15, 15))))

        val meals = repository.getAllMeals()

        assertEquals(3, meals.size)
        assertEquals("Meal 3", meals[0].meal.description)
        assertEquals("Meal 2", meals[1].meal.description)
        assertEquals("Meal 1", meals[2].meal.description)
    }

    @Test
    fun getMealById_returnsCorrectMeal() = runBlocking {
        val mealId = repository.saveMeal("photo", "Dinner", listOf(Triple("Beef", 250, listOf(500, 40, 30, 0))))

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
        val mealId = repository.saveMeal("photo", "Lunch", listOf(Triple("A", 100, listOf(100, 10, 5, 5))))

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
        val manyComponents = (1..100).map { Triple("Component $it", it * 10, listOf(it * 10, it, it, it)) }
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
            listOf(
                Triple("Apple", 100, listOf(100, 0, 0, 25)),
                Triple("Banana", 150, listOf(150, 1, 0, 38)),
                Triple("Orange", 80, listOf(80, 1, 0, 20))
            )
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
