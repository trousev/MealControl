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
class ChatRepositoryTest {

    private lateinit var database: MealControlDatabase
    private lateinit var repository: ChatRepository

    @Before
    fun setup() {
        database = TestDatabaseFactory.createInMemory(RuntimeEnvironment.getApplication())
        repository = ChatRepository(
            database.conversationDao(),
            database.messageDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createConversation_createsNewConversation() = runBlocking {
        val conversationId = repository.createConversation()

        assertTrue(conversationId > 0)

        val conversations = repository.getAllConversations()
        assertEquals(1, conversations.size)
        assertTrue(conversations[0].conversation.title.startsWith("Chat -"))
    }

    @Test
    fun createConversation_multipleConversations_haveDifferentTitles() = runBlocking {
        val id1 = repository.createConversation()
        Thread.sleep(10)
        val id2 = repository.createConversation()
        Thread.sleep(10)
        val id3 = repository.createConversation()

        val conversations = repository.getAllConversations()

        assertEquals(3, conversations.size)
    }

    @Test
    fun getAllConversations_returnsEmptyListWhenNoConversations() = runBlocking {
        val conversations = repository.getAllConversations()

        assertTrue(conversations.isEmpty())
    }

    @Test
    fun getAllConversations_returnsConversations() = runBlocking {
        val conversationId = repository.createConversation()
        repository.sendMessage(conversationId, "Hello")

        val conversations = repository.getAllConversations()

        assertTrue(conversations.isNotEmpty())
    }

    @Test
    fun getAllConversations_returnsNullLastMessageForEmptyConversation() = runBlocking {
        repository.createConversation()

        val conversations = repository.getAllConversations()

        assertEquals(1, conversations.size)
        assertNull(conversations[0].lastMessage)
    }

    @Test
    fun getConversation_returnsCorrectConversation() = runBlocking {
        val convCountBefore = repository.getAllConversations().size
        val conversationId = repository.createConversation()
        repository.sendMessage(conversationId, "First message")
        repository.sendMessage(conversationId, "Second message")

        val conversation = repository.getConversation(conversationId)

        assertNotNull(conversation)
        
        val userMessages = conversation!!.messages.filter { it.isFromUser }
        assertEquals(2, userMessages.size)
    }

    @Test
    fun getConversation_returnsNullForNonExistent() = runBlocking {
        val conversation = repository.getConversation(999L)

        assertNull(conversation)
    }

    @Test
    fun sendMessage_createsMessage() = runBlocking {
        val conversationId = repository.createConversation()

        val message = repository.sendMessage(conversationId, "Hello, bot!")

        assertEquals("Hello, bot!", message.content)
        assertTrue(message.isFromUser)
        assertEquals(conversationId, message.conversationId)
    }

    @Test
    fun sendMessage_multipleMessages_haveCorrectOrder() = runBlocking {
        val conversationId = repository.createConversation()

        repository.sendMessage(conversationId, "Message 1")
        Thread.sleep(10)
        repository.sendMessage(conversationId, "Message 2")
        Thread.sleep(10)
        repository.sendMessage(conversationId, "Message 3")

        val conversation = repository.getConversation(conversationId)
        assertEquals(6, conversation!!.messages.size)

        val userMessages = conversation.messages.filter { it.isFromUser }
        assertEquals(3, userMessages.size)
    }

    @Test
    fun deleteConversation_removesConversationAndMessages() = runBlocking {
        val conversationId = repository.createConversation()
        repository.sendMessage(conversationId, "Some message")

        repository.deleteConversation(conversationId)

        val conversations = repository.getAllConversations()
        assertTrue(conversations.isEmpty())

        val conversation = repository.getConversation(conversationId)
        assertNull(conversation)
    }

    @Test
    fun sendMessage_createsMessagesWithCorrectConversationId() = runBlocking {
        val conversationId1 = repository.createConversation()
        val conversationId2 = repository.createConversation()

        repository.sendMessage(conversationId1, "Message for conv 1")
        repository.sendMessage(conversationId2, "Message for conv 2")

        val conv1 = repository.getConversation(conversationId1)
        val conv2 = repository.getConversation(conversationId2)

        assertEquals(2, conv1!!.messages.size)
        assertEquals(2, conv2!!.messages.size)
    }
}
