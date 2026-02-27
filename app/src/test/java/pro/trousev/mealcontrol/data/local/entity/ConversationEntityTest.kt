package pro.trousev.mealcontrol.data.local.entity

import org.junit.Test
import org.junit.Assert.*

class ConversationEntityTest {

    @Test
    fun conversationEntity_constructor_createsCorrectObject() {
        val timestamp = System.currentTimeMillis()
        val entity = ConversationEntity(
            id = 1L,
            title = "Chat - Jan 01",
            createdAt = timestamp
        )

        assertEquals(1L, entity.id)
        assertEquals("Chat - Jan 01", entity.title)
        assertEquals(timestamp, entity.createdAt)
    }

    @Test
    fun conversationEntity_defaultId_generatesZero() {
        val entity = ConversationEntity(
            title = "New Chat",
            createdAt = 1000L
        )

        assertEquals(0L, entity.id)
    }

    @Test
    fun conversationEntity_copy_modifiesCorrectly() {
        val original = ConversationEntity(
            id = 1L,
            title = "Original Title",
            createdAt = 1000L
        )

        val copied = original.copy(title = "New Title")

        assertEquals(1L, copied.id)
        assertEquals("New Title", copied.title)
        assertEquals(1000L, copied.createdAt)
    }

    @Test
    fun conversationEntity_equality_worksCorrectly() {
        val entity1 = ConversationEntity(
            id = 1L,
            title = "Chat",
            createdAt = 1000L
        )

        val entity2 = ConversationEntity(
            id = 1L,
            title = "Chat",
            createdAt = 1000L
        )

        val entity3 = entity1.copy(id = 2L)

        assertEquals(entity1, entity2)
        assertNotEquals(entity1, entity3)
    }

    @Test
    fun conversationEntity_emptyTitle_isValid() {
        val entity = ConversationEntity(
            title = "",
            createdAt = 1000L
        )

        assertEquals("", entity.title)
    }
}
