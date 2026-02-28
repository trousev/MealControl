package pro.trousev.mealcontrol.data.repository

import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.trousev.mealcontrol.data.local.dao.ConversationDao
import pro.trousev.mealcontrol.data.local.dao.MessageDao
import pro.trousev.mealcontrol.data.local.dao.UserSettingsDao
import pro.trousev.mealcontrol.data.local.entity.ConversationEntity
import pro.trousev.mealcontrol.data.local.entity.ConversationWithMessages
import pro.trousev.mealcontrol.data.local.entity.MessageEntity
import pro.trousev.mealcontrol.data.remote.OpenAiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConversationWithLastMessage(
    val conversation: ConversationEntity,
    val lastMessage: MessageEntity?
)

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val userSettingsDao: UserSettingsDao
) {

    suspend fun getAllConversations(): List<ConversationWithLastMessage> {
        val conversations = conversationDao.getChatConversations()
        return conversations.map { conversation ->
            val lastMessage = messageDao.getLastMessage(conversation.id)
            ConversationWithLastMessage(conversation, lastMessage)
        }
    }

    suspend fun getConversation(conversationId: Long): ConversationWithMessages? {
        return conversationDao.getConversationWithMessages(conversationId)
    }

    suspend fun createConversation(): Long {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
        val title = "Chat - ${dateFormat.format(Date(timestamp))}"
        
        val conversation = ConversationEntity(
            title = title,
            createdAt = timestamp
        )
        return conversationDao.insertConversation(conversation)
    }

    suspend fun sendMessage(conversationId: Long, content: String): MessageEntity {
        val timestamp = System.currentTimeMillis()
        val message = MessageEntity(
            conversationId = conversationId,
            content = content,
            isFromUser = true,
            timestamp = timestamp
        )
        messageDao.insertMessage(message)

        val botReply = getBotReply(conversationId, content, timestamp)

        messageDao.insertMessage(botReply)

        return message
    }

    private suspend fun getBotReply(conversationId: Long, userMessage: String, timestamp: Long): MessageEntity {
        val settings = userSettingsDao.getSettings()
        val apiKey = settings?.openAiApiKey ?: ""

        if (apiKey.isBlank()) {
            return MessageEntity(
                conversationId = conversationId,
                content = "OpenAI API key not configured. Please set it in Settings to enable chat.",
                isFromUser = false,
                timestamp = timestamp + 1
            )
        }

        val conversation = conversationDao.getConversationWithMessages(conversationId)
        val history = conversation?.messages?.sortedBy { it.timestamp } ?: emptyList()

        val messages = buildChatMessages(history, userMessage)
        val truncatedMessages = OpenAiService.truncateToTokenLimit(messages)

        return withContext(Dispatchers.IO) {
            try {
                val openAiService = OpenAiService(apiKey)
                val result = openAiService.chat(truncatedMessages)

                when {
                    result.isSuccess -> {
                        MessageEntity(
                            conversationId = conversationId,
                            content = result.getOrNull() ?: "Unknown error occurred",
                            isFromUser = false,
                            timestamp = timestamp + 1
                        )
                    }
                    result.isFailure -> {
                        val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                        MessageEntity(
                            conversationId = conversationId,
                            content = "Sorry, something went wrong: $errorMessage. Please try again.",
                            isFromUser = false,
                            timestamp = timestamp + 1
                        )
                    }
                    else -> {
                        MessageEntity(
                            conversationId = conversationId,
                            content = "Sorry, something went wrong. Please try again.",
                            isFromUser = false,
                            timestamp = timestamp + 1
                        )
                    }
                }
            } catch (e: Exception) {
                MessageEntity(
                    conversationId = conversationId,
                    content = "Sorry, something went wrong: ${e.message}. Please try again.",
                    isFromUser = false,
                    timestamp = timestamp + 1
                )
            }
        }
    }

    private fun buildChatMessages(history: List<MessageEntity>, newUserMessage: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        messages.add(ChatMessage(role = ChatRole.System, content = OpenAiService.SYSTEM_PROMPT))

        for (msg in history) {
            val role = if (msg.isFromUser) ChatRole.User else ChatRole.Assistant
            messages.add(ChatMessage(role = role, content = msg.content))
        }

        messages.add(ChatMessage(role = ChatRole.User, content = newUserMessage))

        return messages
    }

    suspend fun deleteConversation(conversationId: Long) {
        conversationDao.deleteConversation(conversationId)
    }
}
