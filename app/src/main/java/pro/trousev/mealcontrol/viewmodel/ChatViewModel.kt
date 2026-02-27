package pro.trousev.mealcontrol.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import pro.trousev.mealcontrol.data.local.MealControlDatabase
import pro.trousev.mealcontrol.data.local.entity.ConversationWithMessages
import pro.trousev.mealcontrol.data.repository.ChatRepository
import pro.trousev.mealcontrol.data.repository.ConversationWithLastMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MealControlDatabase.getDatabase(application)
    private val repository = ChatRepository(
        database.conversationDao(),
        database.messageDao()
    )

    private val _conversations = MutableStateFlow<List<ConversationWithLastMessage>>(emptyList())
    val conversations: StateFlow<List<ConversationWithLastMessage>> = _conversations.asStateFlow()

    private val _currentConversation = MutableStateFlow<ConversationWithMessages?>(null)
    val currentConversation: StateFlow<ConversationWithMessages?> = _currentConversation.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _conversations.value = repository.getAllConversations()
        }
    }

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            _currentConversation.value = repository.getConversation(conversationId)
        }
    }

    fun createConversation() {
        viewModelScope.launch {
            repository.createConversation()
            loadConversations()
        }
    }

    fun sendMessage(conversationId: Long, content: String) {
        viewModelScope.launch {
            repository.sendMessage(conversationId, content)
            loadConversation(conversationId)
            loadConversations()
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            loadConversations()
        }
    }
}
