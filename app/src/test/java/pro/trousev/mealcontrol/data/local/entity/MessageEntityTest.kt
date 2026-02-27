package pro.trousev.mealcontrol.data.local.entity

import org.junit.Test
import org.junit.Assert.*

class MessageEntityTest {

    @Test
    fun messageEntity_constructor_createsCorrectObject() {
        val entity = MessageEntity(
            id = 1L,
            conversationId = 10L,
            content = "Hello",
            isFromUser = true,
            timestamp = 1000L
        )

        assertEquals(1L, entity.id)
        assertEquals(10L, entity.conversationId)
        assertEquals("Hello", entity.content)
        assertTrue(entity.isFromUser)
        assertEquals(1000L, entity.timestamp)
    }

    @Test
    fun messageEntity_defaultId_generatesZero() {
        val entity = MessageEntity(
            conversationId = 10L,
            content = "Hi",
            isFromUser = false,
            timestamp = 1000L
        )

        assertEquals(0L, entity.id)
    }

    @Test
    fun messageEntity_copy_modifiesCorrectly() {
        val original = MessageEntity(
            id = 1L,
            conversationId = 10L,
            content = "Original",
            isFromUser = true,
            timestamp = 1000L
        )

        val copied = original.copy(
            content = "Modified",
            isFromUser = false
        )

        assertEquals(1L, copied.id)
        assertEquals(10L, copied.conversationId)
        assertEquals("Modified", copied.content)
        assertFalse(copied.isFromUser)
        assertEquals(1000L, copied.timestamp)
    }

    @Test
    fun messageEntity_equality_worksCorrectly() {
        val entity1 = MessageEntity(
            id = 1L,
            conversationId = 10L,
            content = "Hello",
            isFromUser = true,
            timestamp = 1000L
        )

        val entity2 = MessageEntity(
            id = 1L,
            conversationId = 10L,
            content = "Hello",
            isFromUser = true,
            timestamp = 1000L
        )

        val entity3 = entity1.copy(isFromUser = false)

        assertEquals(entity1, entity2)
        assertNotEquals(entity1, entity3)
    }

    @Test
    fun messageEntity_botMessage_isValid() {
        val entity = MessageEntity(
            conversationId = 10L,
            content = "Bot response",
            isFromUser = false,
            timestamp = 2000L
        )

        assertFalse(entity.isFromUser)
    }

    @Test
    fun messageEntity_emptyContent_isValid() {
        val entity = MessageEntity(
            conversationId = 10L,
            content = "",
            isFromUser = true,
            timestamp = 1000L
        )

        assertEquals("", entity.content)
    }

    @Test
    fun messageEntity_longContent_isValid() {
        val longContent = "A".repeat(10000)
        val entity = MessageEntity(
            conversationId = 10L,
            content = longContent,
            isFromUser = true,
            timestamp = 1000L
        )

        assertEquals(10000, entity.content.length)
    }
}
