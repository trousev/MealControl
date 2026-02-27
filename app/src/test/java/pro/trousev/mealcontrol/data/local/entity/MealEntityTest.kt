package pro.trousev.mealcontrol.data.local.entity

import org.junit.Test
import org.junit.Assert.*

class MealEntityTest {

    @Test
    fun mealEntity_constructor_createsCorrectObject() {
        val timestamp = System.currentTimeMillis()
        val entity = MealEntity(
            id = 1L,
            photoUri = "content://photo/123",
            description = "Lunch",
            timestamp = timestamp
        )

        assertEquals(1L, entity.id)
        assertEquals("content://photo/123", entity.photoUri)
        assertEquals("Lunch", entity.description)
        assertEquals(timestamp, entity.timestamp)
    }

    @Test
    fun mealEntity_defaultId_generatesZero() {
        val entity = MealEntity(
            photoUri = "content://photo/123",
            description = "Dinner",
            timestamp = 1000L
        )

        assertEquals(0L, entity.id)
    }

    @Test
    fun mealEntity_copy_modifiesCorrectly() {
        val original = MealEntity(
            id = 1L,
            photoUri = "content://photo/123",
            description = "Lunch",
            timestamp = 1000L
        )

        val copied = original.copy(description = "Breakfast")

        assertEquals(1L, copied.id)
        assertEquals("content://photo/123", copied.photoUri)
        assertEquals("Breakfast", copied.description)
        assertEquals(1000L, copied.timestamp)
    }

    @Test
    fun mealEntity_equality_worksCorrectly() {
        val entity1 = MealEntity(
            id = 1L,
            photoUri = "content://photo/123",
            description = "Lunch",
            timestamp = 1000L
        )

        val entity2 = MealEntity(
            id = 1L,
            photoUri = "content://photo/123",
            description = "Lunch",
            timestamp = 1000L
        )

        val entity3 = entity1.copy(id = 2L)

        assertEquals(entity1, entity2)
        assertNotEquals(entity1, entity3)
    }
}
