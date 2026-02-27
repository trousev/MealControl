package pro.trousev.mealcontrol.data.local.entity

import org.junit.Test
import org.junit.Assert.*

class MealComponentEntityTest {

    @Test
    fun mealComponentEntity_constructor_createsCorrectObject() {
        val entity = MealComponentEntity(
            id = 1L,
            mealId = 10L,
            name = "Chicken",
            calories = 300
        )

        assertEquals(1L, entity.id)
        assertEquals(10L, entity.mealId)
        assertEquals("Chicken", entity.name)
        assertEquals(300, entity.calories)
    }

    @Test
    fun mealComponentEntity_defaultId_generatesZero() {
        val entity = MealComponentEntity(
            mealId = 10L,
            name = "Rice",
            calories = 200
        )

        assertEquals(0L, entity.id)
    }

    @Test
    fun mealComponentEntity_copy_modifiesCorrectly() {
        val original = MealComponentEntity(
            id = 1L,
            mealId = 10L,
            name = "Chicken",
            calories = 300
        )

        val copied = original.copy(calories = 350)

        assertEquals(1L, copied.id)
        assertEquals(10L, copied.mealId)
        assertEquals("Chicken", copied.name)
        assertEquals(350, copied.calories)
    }

    @Test
    fun mealComponentEntity_equality_worksCorrectly() {
        val entity1 = MealComponentEntity(
            id = 1L,
            mealId = 10L,
            name = "Chicken",
            calories = 300
        )

        val entity2 = MealComponentEntity(
            id = 1L,
            mealId = 10L,
            name = "Chicken",
            calories = 300
        )

        val entity3 = entity1.copy(name = "Beef")

        assertEquals(entity1, entity2)
        assertNotEquals(entity1, entity3)
    }

    @Test
    fun mealComponentEntity_zeroCalories_isValid() {
        val entity = MealComponentEntity(
            mealId = 10L,
            name = "Water",
            calories = 0
        )

        assertEquals(0, entity.calories)
    }

    @Test
    fun mealComponentEntity_negativeCalories_isValid() {
        val entity = MealComponentEntity(
            mealId = 10L,
            name = "Negative",
            calories = -100
        )

        assertEquals(-100, entity.calories)
    }
}
