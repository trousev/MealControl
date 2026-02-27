package pro.trousev.mealcontrol.data.repository

import pro.trousev.mealcontrol.data.local.dao.ConversationDao
import pro.trousev.mealcontrol.data.local.dao.MessageDao
import pro.trousev.mealcontrol.data.local.entity.ConversationEntity
import pro.trousev.mealcontrol.data.local.entity.ConversationWithMessages
import pro.trousev.mealcontrol.data.local.entity.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ConversationWithLastMessage(
    val conversation: ConversationEntity,
    val lastMessage: MessageEntity?
)

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    suspend fun getAllConversations(): List<ConversationWithLastMessage> {
        val conversations = conversationDao.getAllConversations()
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

        val botReply = MessageEntity(
            conversationId = conversationId,
            content = "Sorry, not implemented yet",
            isFromUser = false,
            timestamp = timestamp + 1
        )
        messageDao.insertMessage(botReply)

        return message
    }

    suspend fun deleteConversation(conversationId: Long) {
        conversationDao.deleteConversation(conversationId)
    }
}
